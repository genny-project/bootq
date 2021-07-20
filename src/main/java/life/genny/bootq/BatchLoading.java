package life.genny.bootq;

import life.genny.QwandaRepositoryService;
import life.genny.entity.DataType;
import life.genny.entity.Validation;
import life.genny.entity.ValidationList;
import life.genny.utils.KeycloakUtils;
import life.genny.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import javax.persistence.NoResultException;
import java.lang.invoke.MethodHandles;
import java.util.*;

class Options {
    public String optionCode = null;
    public String optionLabel = null;
}


public class BatchLoading {
    private QwandaRepositoryService service;

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    public static final Boolean CleanupTaskAndBeAttrForm = System.getenv("CLEANUP_TASK_AND_BEATTRFORM") != null ? "TRUE".equalsIgnoreCase(System.getenv("CLEANUP_TASK_AND_BEATTRFORM")) : true;


    public BatchLoading(QwandaRepositoryService repo) {
        this.service = repo;
    }

    private String decodePassword(String realm, String securityKey, String servicePass) {
        String initVector = "PRJ_" + realm.toUpperCase();
        initVector = StringUtils.rightPad(initVector, 16, '*');
        String decrypt = SecurityUtils.decrypt(securityKey, initVector, servicePass);
        return decrypt;
    }

    public Map<String, DataType> dataType(Map<String, Map<String, String>> project) {
        final Map<String, DataType> dataTypeMap = new HashMap<>();
        project.entrySet().stream().filter(d -> !d.getKey().matches("\\s*")).forEach(data -> {
            Map<String, String> dataType = data.getValue();
            String validations = dataType.get("validations");
            String code = (dataType.get("code")).trim().replaceAll("^\"|\"$", "");
            String className = (dataType.get("classname")).replaceAll("^\"|\"$", "");
            String name = (dataType.get("name")).replaceAll("^\"|\"$", "");
            String inputmask = dataType.get("inputmask");
            String component = dataType.get("component");
            final ValidationList validationList = new ValidationList();
            validationList.setValidationList(new ArrayList<Validation>());
            if (validations != null) {
                final String[] validationListStr = validations.split(",");
                for (final String validationCode : validationListStr) {
                    try {
                        Validation validation = service.findValidationByCode(validationCode);
                        validationList.getValidationList().add(validation);
                    } catch (NoResultException e) {
                        log.error("Could not load Validation " + validationCode);
                    }
                }
            }
            if (!dataTypeMap.containsKey(code)) {
                DataType dataTypeRecord;
                if (component == null) {
                    dataTypeRecord = new DataType(className, validationList, name, inputmask);
                } else {
                    dataTypeRecord = new DataType(className, validationList, name, inputmask, component);
                }
                dataTypeRecord.setDttCode(code);
                dataTypeMap.put(code, dataTypeRecord);
            }
        });
        return dataTypeMap;
    }


    public void persistProjectOptimization(RealmUnit rx) {
        boolean isSynchronise = false;
        service.setRealm(rx.getCode());

        String decrypt = decodePassword(rx.getCode(), rx.getSecurityKey(), rx.getServicePassword());
        HashMap<String, String> userCodeUUIDMapping = KeycloakUtils.getUsersByRealm(rx.getKeycloakUrl(), rx.getCode(), decrypt);
        Optimization optimization = new Optimization(service);

        // clean up       
        if (CleanupTaskAndBeAttrForm) {
            System.out.println("Clean Task and BeAttrForm");
            service.cleanAsk(rx.getCode());
            service.cleanFrameFromBaseentityAttribute(rx.getCode());
        }

        optimization.validationsOptimization(rx.getValidations(), rx.getCode());

        Map<String, DataType> dataTypes = dataType(rx.getDataTypes());
        optimization.attributesOptimization(rx.getAttributes(), dataTypes, rx.getCode());

        optimization.def_baseEntitysOptimization(rx.getDef_baseEntitys(), rx.getCode(), userCodeUUIDMapping);
        optimization.def_baseEntityAttributesOptimization(rx.getDef_entityAttributes(), rx.getCode(), userCodeUUIDMapping, dataTypes);

        optimization.baseEntitysOptimization(rx.getBaseEntitys(), rx.getCode(), userCodeUUIDMapping);

        optimization.attributeLinksOptimization(rx.getAttributeLinks(), dataTypes, rx.getCode());

        optimization.baseEntityAttributesOptimization(rx.getEntityAttributes(), rx.getCode(), userCodeUUIDMapping);

        optimization.entityEntitysOptimization(rx.getEntityEntitys(), rx.getCode(), isSynchronise, userCodeUUIDMapping);

        optimization.questionsOptimization(rx.getQuestions(), rx.getCode(), isSynchronise);

        optimization.questionQuestionsOptimization(rx.getQuestionQuestions(), rx.getCode());
    }

}
