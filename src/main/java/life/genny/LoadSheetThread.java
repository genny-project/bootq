package life.genny;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import life.genny.bootxport.bootx.*;
import life.genny.bootxport.xlsimport.BatchLoading;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@ApplicationScoped
public class LoadSheetThread extends Thread {
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    LinkedBlockingQueue<String> queue;

    @Inject
    EntityManager em;

    public LoadSheetThread(LinkedBlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Transactional
    private void doSheetsLoading(String sheetId) {
        Realm realm = new Realm(BatchLoadMode.ONLINE, sheetId);
        List<Tuple2<RealmUnit, BatchLoading>> collect = realm.getDataUnits().stream().map(d -> {
//            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
//            Session openSession = sessionFactory.openSession();
//            EntityManager createEntityManager = openSession.getEntityManagerFactory().createEntityManager();
            QwandaRepository repo = new QwandaRepositoryImpl(em);
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
    }

    @Override
    public void run() {
        while (true) {
            try {
                String sheetId = queue.take();
                log.info("Start loading sheets, sheetId:" + sheetId);
                doSheetsLoading(sheetId);
                log.info("Finish loading sheets, sheetId:" + sheetId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
