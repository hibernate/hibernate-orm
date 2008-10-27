package org.jboss.envers.test.performance;

import org.jboss.envers.test.entities.StrTestEntity;
import org.jboss.envers.test.entities.UnversionedStrTestEntity;
import org.jboss.envers.tools.Pair;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.io.IOException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InsertsPerformance extends AbstractPerformanceTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(UnversionedStrTestEntity.class);
    }

    private final static int NUMBER_INSERTS = 1000;
    
    private void insertUnversioned() {
        EntityManager entityManager = getEntityManager();
        for (int i=0; i<NUMBER_INSERTS; i++) {
            entityManager.getTransaction().begin();
            entityManager.persist(new UnversionedStrTestEntity("x" + i));
            entityManager.getTransaction().commit();
        }
    }

    private void insertVersioned() {
        EntityManager entityManager = getEntityManager();
        for (int i=0; i<NUMBER_INSERTS; i++) {
            entityManager.getTransaction().begin();
            entityManager.persist(new StrTestEntity("x" + i));
            entityManager.getTransaction().commit();
        }
    }

    protected Pair<Long, Long> doTest() {
        long unversioned = measureTime(new Runnable() { public void run() { insertUnversioned(); } });
        long versioned = measureTime(new Runnable() { public void run() { insertVersioned(); } });

        return Pair.make(unversioned, versioned);
    }

    protected String getName() {
        return "INSERTS";
    }

    public static void main(String[] args) throws IOException {
        InsertsPerformance insertsPerformance = new InsertsPerformance();
        insertsPerformance.init();
        insertsPerformance.run(10);
    }
}
