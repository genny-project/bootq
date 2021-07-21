package life.genny;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import life.genny.bootxport.bootx.*;
import life.genny.bootxport.xlsimport.BatchLoading;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.LinkedBlockingQueue;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import life.genny.bootxport.utils.HibernateUtil;

public class LoadSheetThread extends Thread {
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    LinkedBlockingQueue<String> queue;


    public LoadSheetThread(LinkedBlockingQueue<String> queue) {
        this.queue = queue;
    }

    private void doSheetsLoading(String sheetId) {
        Realm realm = new Realm(BatchLoadMode.ONLINE, sheetId);
        realm.getDataUnits().forEach(d -> {
            if (!d.getSkipGoogleDoc() && !d.getSkipGoogleDoc()) {
                SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
                Session openSession = sessionFactory.openSession();
                EntityManager createEntityManager = openSession.getEntityManagerFactory().createEntityManager();
                QwandaRepository repo = new QwandaRepositoryImpl(createEntityManager);
                BatchLoading bl = new BatchLoading(repo);
                bl.persistProjectOptimization(d);
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
