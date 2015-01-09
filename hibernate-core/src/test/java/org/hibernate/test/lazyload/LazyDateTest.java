package org.hibernate.test.lazyload;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HHH-9563")
public class LazyDateTest extends BaseCoreFunctionalTestCase {

    private final LazyDateEntity persistedEntity = new LazyDateEntity();

    @Override
    protected void configure(Configuration cfg) {
        super.configure(cfg);
        cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { LazyDateEntity.class };
    }

    @Test
    public void testLazyDate() throws Exception {
        persistEntity();
        putEntityIntoL2Cache();
        loadEntityFromL2Cache();
    }

    private void persistEntity() {
        Session s = openSession();
        s.beginTransaction();
        persistedEntity.setDate(new Date());
        s.persist(persistedEntity);
        s.getTransaction().commit();
        s.close();
    }

    private void putEntityIntoL2Cache() {
        Session s = openSession();
        //clear cache to get rid of entry created by persist
        s.getSessionFactory().getCache().evictAllRegions();
        s.beginTransaction();
        LazyDateEntity proxy = (LazyDateEntity) s.load(LazyDateEntity.class, persistedEntity.getId());
        assertNotNull("getDate() returned null (but shouldn't have)", proxy.getDate());
        s.getTransaction().commit();
        s.close();
    }

    private void loadEntityFromL2Cache() {
        Session s = openSession();
        s.beginTransaction();
        LazyDateEntity proxy = (LazyDateEntity) s.load(LazyDateEntity.class, persistedEntity.getId());
        assertNotNull("getDate() returned null (but shouldn't have)", proxy.getDate());
        s.getTransaction().commit();
        s.close();
    }
}
