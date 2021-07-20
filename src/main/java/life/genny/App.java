package life.genny;

import life.genny.bootq.BatchLoading;
import life.genny.bootq.Realm;
import life.genny.bootq.RealmUnit;
import org.apache.maven.shared.utils.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@ApplicationScoped
@Path("/bootq/")
public class App {
    private static final Logger log = Logger.getLogger(App.class);
    @Inject
    @PersistenceContext
    EntityManager em;

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response version() {
        return Response.status(200).entity("Application version:" + version).build();
    }

/*
    @GET
    @Path("/loadsheets")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public String loadSheets() {
        String sheetId = System.getenv("GOOGLE_SHEETS_ID");
        if (StringUtils.isBlank(sheetId)) {
            sheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
        }
        loadSheetsById(sheetId);
        return "Finished batch loading";
    }
 */

    @GET
    @Path("/loadsheets/{sheetid}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String loadSheetsById(@PathParam("sheetid") final String sheetId) {
        String msg = "";

        if (sheetId == null) {
            msg = "Can't find env GOOGLE_SHEETS_ID!!!";
            log.error(msg);
            return msg;
        }
        QwandaRepositoryService repo = new QwandaRepositoryService(em);
        Realm realm = new Realm(sheetId);
        List<RealmUnit> units = realm.getDataUnits();
        for (RealmUnit rm : units) {
            if (!(rm.getDisable()) && !(rm.getSkipGoogleDoc())) {
                BatchLoading bl = new BatchLoading(repo);
                bl.persistProjectOptimization(rm);
            }
        }
        return "Added batch loading request to queue.";
    }

/*
    @GET
    @Path("/loaddefs/{realm}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response loadDefs(@PathParam("realm") final String realm) {

        log.info("Loading in DEFS for realm " + realm);

        SearchEntity searchBE = new SearchEntity("SBE_DEF", "DEF test")
                .addSort("PRI_NAME", "Created", SearchEntity.Sort.ASC)
                .addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "DEF_%")

                .addColumn("PRI_NAME", "Name");

        searchBE.setRealm(realm);
        searchBE.setPageStart(0);
        searchBE.setPageSize(10000);

        JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
        String sToken = tokenObj.getString("value");
        GennyToken serviceToken = new GennyToken("PER_SERVICE", sToken);

        if ("DUMMY".equalsIgnoreCase(serviceToken.getToken())) {
            log.error("NO SERVICE TOKEN FOR " + realm + " IN CACHE");
            return Response.status(Status.ERROR).entity("NO SERVICE TOKEN FOR " + realm + " IN CACHE").build();
        }
        BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken, serviceToken);

        List<BaseEntity> items = beUtils.getBaseEntitys(searchBE);
        log.info("Loaded " + items.size() + " DEF baseentitys");

        RulesUtils.defs.put(realm, new ConcurrentHashMap<>());

        for (BaseEntity item : items) {
            item.setFastAttributes(true); // make fast
            RulesUtils.defs.get(realm).put(item.getCode(), item);
            log.info("Saving (" + realm + ") DEF " + item.getCode());
        }
        log.info("Saved " + items.size() + " yummy DEFs!");
        return Response.ok().build();
    }

 */
}
