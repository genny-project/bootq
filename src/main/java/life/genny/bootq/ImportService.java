package life.genny.bootq;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImportService {
    private final Log log = LogFactory.getLog(ImportService.class);
    private Map<String, XlsxImportOnline> state;

    public XlsxImportOnline createXlsImport(String key) {
        if (SheetState.getUpdateState().contains(key)) {
            XlsxImportOnline xlsxImportOnline = new XlsxImportOnline(GoogleImportService.getInstance().getService());
            state.put(key, xlsxImportOnline);
            SheetState.removeUpdateState(key);
            log.info("The state it is being updated... " + key);
            return xlsxImportOnline;
        } else if (state.containsKey(key)) {
            return state.get(key);
        } else {
            log.info("Creating a new Import service for " + key);
            XlsxImportOnline xlsxImportOnline = new XlsxImportOnline(GoogleImportService.getInstance().getService());
            state.put(key, xlsxImportOnline);
            return xlsxImportOnline;
        }
    }

    public ImportService(Map<String, XlsxImportOnline> state) {
        this.state = state;
    }

    public List<RealmUnit> fetchRealmUnit(String sheetURI) {
        String projects = "Projects";
        String key = sheetURI + projects;
        XlsxImportOnline createXlsImport = createXlsImport(key);
        List<RealmUnit> list = new ArrayList<>();
        for (Map<String, String> rawData : createXlsImport
                .mappingRawToHeaderAndValuesFmt(sheetURI, projects)) {
            if (!rawData.isEmpty()) {
                RealmUnit name = new RealmUnit(rawData);
                list.add(name);
            }
        }
        return list;
    }

    public List<ModuleUnit> fetchModuleUnit(String sheetURI) {
        String modules = "Modules";
        String key = sheetURI + modules;
        XlsxImportOnline createXlsImport = createXlsImport(key);
        return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, modules)
                .stream()
                .filter(rawData -> !rawData.isEmpty())
                .map(d1 -> {
                    ModuleUnit moduleUnit = new ModuleUnit(d1.get("sheetID".toLowerCase()));
                    moduleUnit.setName(d1.get("name"));
                    return moduleUnit;
                })
                .collect(Collectors.toList());
    }

}
