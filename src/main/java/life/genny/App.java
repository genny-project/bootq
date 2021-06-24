package life.genny;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import life.genny.bootxport.bootx.*;
import life.genny.bootxport.utils.HibernateUtil;
import life.genny.bootxport.xlsimport.BatchLoading;

import org.jboss.logging.Logger;


import org.hibernate.Session;
import org.hibernate.SessionFactory;

@Path("/")
public class App {
	
	private static final Logger log = Logger.getLogger(App.class);

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return "running version:9.0.0";
    }

    @GET
    @Path("/loadsheets")
    @Produces(MediaType.TEXT_PLAIN)
    public String loadSheets() {
        String sheetId = System.getenv("GOOGLE_SHEETS_ID");
        String msg = "";
        if (sheetId == null) {
            msg = "Can't find env GOOGLE_SHEETS_ID!!!";
            log.error(msg);
            return msg;
        }

        Realm realm = new Realm(BatchLoadMode.ONLINE, sheetId);
        List<Tuple2<RealmUnit, BatchLoading>> collect = realm.getDataUnits().stream().map(d -> {
                    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
                    Session openSession = sessionFactory.openSession();
                    EntityManager createEntityManager =
                            openSession.getEntityManagerFactory().createEntityManager();
                    QwandaRepository repo = new QwandaRepositoryImpl(createEntityManager);
                    BatchLoading bl = new BatchLoading(repo);
                    return Tuple.of(d, bl);
                }
        ).collect(Collectors.toList());

        collect.parallelStream().forEach(d ->
                {
                    if (!d._1.getDisable() && !d._1.getSkipGoogleDoc())
                        d._2.persistProject(d._1);
                    else {
                        System.out.println("Realm:" + d._1.getName()
                                + ", disabled:" + d._1.getDisable()
                                + ", skipGoogleDoc:" + d._1.getSkipGoogleDoc());
                    }
                }
        );
        return "Finished batch loading";
    }
}
