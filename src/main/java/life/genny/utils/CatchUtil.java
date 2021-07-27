package life.genny.utils;

import life.genny.bootxport.bootx.RealmUnit;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwandautils.JsonUtils;
import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

public class CatchUtil {
/*
    private void pushBEsToCache(CriteriaBuilder builder, final String realm) {
        CriteriaQuery<BaseEntity> query = builder.createQuery(BaseEntity.class);
        Root<BaseEntity> be = query.from(BaseEntity.class);
        Join<BaseEntity, EntityAttribute> ea = (Join) be.fetch("baseEntityAttributes");
        query.select(be);
        query.distinct(true);
        query.where(builder.equal(ea.get("realm"), realm));

        List<BaseEntity> results = em.createQuery(query).getResultList();

        log.info("Pushing " + realm + " : " + results.size() + " Basentitys to Cache");
        service.writeToDDT(results);
        log.info("Pushed " + realm + " : " + results.size() + " Basentitys to Cache");

    }

    private void pushQuestionsToCache(CriteriaBuilder builder, final String realm) {
//		CriteriaQuery<Question> query = builder.createQuery(Question.class);
//		Root<Question> be = query.from(Question.class);
//		Join<Question, QuestionQuestion> ea = (Join) be.fetch("childQuestions");
//		query.select(be);
//		query.distinct(true);
//		query.where(builder.equal(ea.get("realm"), realm));
//
//		List<Question> results = em.createQuery(query).getResultList();

        final List<Question> results = em
                .createQuery("SELECT a FROM Question a where a.realm=:realmStr").setParameter("realmStr", realm)
                .getResultList();


        log.info("Pushing " + realm + " : " + results.size() + " Questions to Cache");
        service.writeQuestionsToDDT(results);
        log.info("Pushed " + realm + " : " + results.size() + " Questions to Cache");

    }


    public void pushToDTT(RealmUnit realmUnit) {
        // Attributes
        log.info("Pushing Attributes to Cache");
        final List<Attribute> entitys = service.findAttributes();
        Attribute[] atArr = new Attribute[entitys.size()];
        atArr = entitys.toArray(atArr);
        QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
        String json = JsonUtils.toJson(msg);
        service.writeToDDT("attributes", json);
        log.info("Pushed " + entitys.size() + " attributes to cache");

        String realmCode = realmUnit.getCode();
        // BaseEntitys
//		List<BaseEntity> results = em
//				.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ").getResultList();
        Session session = em.unwrap(org.hibernate.Session.class);

//		for (String realmCode : projects.keySet()) {
//			Map<String, Object> project = projects.get(realmCode);
        if ("FALSE".equals((String) realmUnit.getDisable().toString().toUpperCase())) {

            service.setCurrentRealm(realmCode);
            log.info("Project: " + realmCode + " push to DDT");

            String realm = realmCode;

            CriteriaBuilder builder = em.getCriteriaBuilder();
            pushBEsToCache(builder, realm);
            pushQuestionsToCache(builder, realm);
        }

//		}

        // Collect all the baseentitys

    }

 */
}
