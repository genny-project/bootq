package life.genny;

import ch.qos.logback.core.status.Status;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import life.genny.bootxport.bootx.*;
import life.genny.bootxport.xlsimport.BatchLoading;
import life.genny.models.GennyToken;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwandautils.GennySettings;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@Path("/bootq/")
public class App {

    private static final Logger log = Logger.getLogger(App.class);

    private boolean isBatchLoadingRunning = false;

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authUrl;

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response version() {
        log.info("Application endpoint hit");
        return Response.status(200).entity("Application version:" + version).build();
    }

    @Inject
    EntityManager em;

    @Inject
    JsonWebToken accessToken;

    public boolean getIsTaskRunning() {
        return isBatchLoadingRunning;
    }

    public void setIsTaskRunning(boolean isTaskRunning) {
        this.isBatchLoadingRunning = isTaskRunning;
    }

    @GET
    @Path("/loadsheets")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsUsingDefaultSheetId() {
        String defaultSheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
        if (defaultSheetId != null) {
            return Response.ok().entity(loadSheetsById(defaultSheetId)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Can't find default google sheetId, please set environment variable GOOGLE_HOSTING_SHEET_ID, or call /loadsheets/{sheetid}")
                    .build();
        }
    }

    @GET
    @Path("/loadsheets/{sheetid}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsById(@PathParam("sheetid") final String sheetId) {
        log.info("Loading in sheet " + sheetId);
        String msg = "";
        String authToken = accessToken.getRawToken();

        if (getIsTaskRunning()) {
            log.error("Batch loading task is running, please try later or force restart pod");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Batch loading task is running, please try later or force restart pod")
                    .build();
        }

        if (sheetId == null) {
            msg = "Can't find env GOOGLE_SHEETS_ID!!!";
            log.error(msg);
            return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
        }

        setIsTaskRunning(true);

        Realm realm = new Realm(BatchLoadMode.ONLINE, sheetId);
        List<RealmUnit> realmUnits = realm.getDataUnits();
        try {
            for (RealmUnit realmUnit : realmUnits) {
                log.info("Importing from sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());

                if (!realmUnit.getDisable() && !realmUnit.getSkipGoogleDoc()) {
                    log.info("Starting batch loading for sheet:" + realmUnit.getUri()
                            + ", realm:" + realmUnit.getName());
                    QwandaRepository repo = new QwandaRepositoryService(em);
                    BatchLoading bl = new BatchLoading(repo);
                    bl.persistProjectOptimization(realmUnit);
                    log.info("Finished batch loading for sheet:" + realmUnit.getUri()
                            + ", realm:" + realmUnit.getName() + ", now syncing be, attr and questions");

                    // SyncEntityThread syncEntityThread = new SyncEntityThread(authToken,
                    // realmUnit.getName());
                    // syncEntityThread.start();
                } else {
                    log.info("SKIPPING sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());
                }
                msg = "Finished batch loading for all realms in google sheets";
            }
        } catch (Exception ex) {
            msg = "Exception:" + ex.getMessage() + " occurred when batch loading";
			log.info(msg);
			ex.printStackTrace();
        } finally {
            setIsTaskRunning(false);
        }
        return Response.ok().entity(msg).build();

        /*
         * List<Tuple2<RealmUnit, BatchLoading>> collect =
         * realm.getDataUnits().stream().map(d -> {
         * // SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
         * // Session openSession = sessionFactory.openSession();
         * // EntityManager createEntityManager =
         * openSession.getEntityManagerFactory().createEntityManager();
         * QwandaRepository repo = new QwandaRepositoryService(em);
         * BatchLoading bl = new BatchLoading(repo);
         * return Tuple.of(d, bl);
         * }).collect(Collectors.toList());
         * 
         * collect.parallelStream().forEach(d -> {
         * if (!d._1.getDisable() && !d._1.getSkipGoogleDoc()) {
         * d._2.persistProject(d._1);
         * System.out.println("Finish batch loading, sheetID:" + d._1.getUri());
         * } else {
         * System.out.println("Realm:" + d._1.getName() + ", disabled:" +
         * d._1.getDisable() + ", skipGoogleDoc:"
         * + d._1.getSkipGoogleDoc());
         * }
         * });
         */
    }

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

        if ((serviceToken == null) || ("DUMMY".equalsIgnoreCase(serviceToken.getToken()))) {
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

    /*
     * @GET
     * 
     * @Path("/sync/{baseEntityCode}")
     * public Response syncBaseEntityByCode(@PathParam("baseEntityCode") final
     * String baseEntityCode) throws IOException {
     * String authToken = accessToken.getRawToken();
     * GennyToken userToken = new GennyToken(authToken);
     * // Get value in db from qwanda endpoint
     * String getUrl = GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/" +
     * baseEntityCode;
     * String body = QwandaUtils.apiGet(getUrl, userToken);
     * 
     * // sync to cache
     * String postUrl = GennySettings.qwandaServiceUrl + "/service/cache/write/" +
     * baseEntityCode;
     * String result = QwandaUtils.apiPostEntity2(postUrl, body, userToken, null);
     * return Response.ok().build();
     * }
     */

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Bootq Endpoint starting with auth Server " + authUrl);

    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Bootq Endpoint Shutting down");
    }
}
