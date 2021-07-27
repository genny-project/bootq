package life.genny;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.json.JsonObject;
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
import org.jboss.logging.Logger;

import ch.qos.logback.core.status.Status;

@Path("/bootq/")
public class App {

    private static final Logger log = Logger.getLogger(App.class);

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response version() {
        return Response.status(200).entity("Application version:" + version).build();
    }

    @Inject
    EntityManager em;


    /*
    // Test HibernateUtil
        @GET
        @Path("/test")
        public void test() {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            Session openSession = sessionFactory.openSession();
            EntityManager createEntityManager = openSession.getEntityManagerFactory().createEntityManager();
            QwandaRepository repo = new QwandaRepositoryImpl(createEntityManager);
            BatchLoading bl = new BatchLoading(repo);
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

        Realm realm = new Realm(BatchLoadMode.ONLINE, sheetId);
        List<RealmUnit> realmUnits = realm.getDataUnits();
        for (RealmUnit realmUnit : realmUnits) {
            if (!realmUnit.getDisable() && !realmUnit.getSkipGoogleDoc()) {
                QwandaRepository repo = new QwandaRepositoryService(em);
                BatchLoading bl = new BatchLoading(repo);
                bl.persistProjectOptimization(realmUnit);
                log.info("Finished batch loading for sheet" + realmUnit.getUri());
            }
        }

        /*
        List<Tuple2<RealmUnit, BatchLoading>> collect = realm.getDataUnits().stream().map(d -> {
//            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
//            Session openSession = sessionFactory.openSession();
//            EntityManager createEntityManager = openSession.getEntityManagerFactory().createEntityManager();
            QwandaRepository repo = new QwandaRepositoryService(em);
            BatchLoading bl = new BatchLoading(repo);
            return Tuple.of(d, bl);
        }).collect(Collectors.toList());

        collect.parallelStream().forEach(d -> {
            if (!d._1.getDisable() && !d._1.getSkipGoogleDoc()) {
                d._2.persistProject(d._1);
                System.out.println("Finish batch loading, sheetID:" + d._1.getUri());
            } else {
                System.out.println("Realm:" + d._1.getName() + ", disabled:" + d._1.getDisable() + ", skipGoogleDoc:"
                        + d._1.getSkipGoogleDoc());
            }
        });
         */
        return "Finished batch loading";
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

        RulesUtils.defs.put(realm, new ConcurrentHashMap<String, BaseEntity>());

        for (BaseEntity item : items) {
            item.setFastAttributes(true); // make fast
            RulesUtils.defs.get(realm).put(item.getCode(), item);
            log.info("Saving (" + realm + ") DEF " + item.getCode());
        }
        log.info("Saved " + items.size() + " yummy DEFs!");
        return Response.ok().build();
    }
}
