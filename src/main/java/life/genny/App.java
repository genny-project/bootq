package life.genny;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.invoke.MethodHandles;
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

     private Realm rx;

    @GET
    @Path("/loadsheets")
    @Produces(MediaType.TEXT_PLAIN)
    public void hello() {
        System.out.println("hello");
        Realm realm = new Realm(BatchLoadMode.ONLINE, "17CbqWLICh882xKVTU5J5mqqvGVl2F0Z7mdTgiAHAXx8");

        List<Tuple2<RealmUnit, BatchLoading>> collect = realm.getDataUnits().stream().map(d -> {
                    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
                    Session openSession = sessionFactory.openSession();
                    EntityManager createEntityManager =
                            openSession.getEntityManagerFactory().createEntityManager();
                    QwandaRepository repo =
                            new QwandaRepositoryImpl(createEntityManager);
                    BatchLoading bl = new BatchLoading(repo);
                    return Tuple.of(d, bl);
                }
        ).collect(Collectors.toList());
//
//        collect.parallelStream().forEach(d ->
//                {
//                    if (!d._1.getDisable() && !d._1.getSkipGoogleDoc())
//                        d._2.persistProject(d._1);
//                    else {
//                        System.out.println("Realm:" + d._1.getName()
//                                + ", disabled:" + d._1.getDisable()
//                                + ", skipGoogleDoc:" + d._1.getSkipGoogleDoc());
//                    }
//                }
//        );
    }
}