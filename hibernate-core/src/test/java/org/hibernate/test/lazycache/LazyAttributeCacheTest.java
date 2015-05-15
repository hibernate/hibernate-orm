package org.hibernate.test.lazycache;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HHH-9563")
public class LazyAttributeCacheTest extends BaseCoreFunctionalTestCase {

    public static final String HIBERNATE = "Hibernate";
    private final LazyAttributeEntity persistedEntity = new LazyAttributeEntity();

    @Override
    protected void configure(Configuration cfg) {
        super.configure(cfg);
        cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { LazyAttributeEntity.class };
    }

    @Test
    public void testLazyAttributesWhenCached() throws Exception {
        persistEntity();
        putEntityIntoL2Cache();
        loadEntityFromL2Cache();
    }

    private void persistEntity() {
        Session s = openSession();
        s.beginTransaction();
        persistedEntity.setDate(new Date());
        persistedEntity.setString(HIBERNATE);
        persistedEntity.setIntVal(Integer.MAX_VALUE);
        persistedEntity.setLongVal(Long.MAX_VALUE);
        s.persist(persistedEntity);
        s.getTransaction().commit();
        s.close();
    }

    private void putEntityIntoL2Cache() {
        Session s = openSession();
        //clear cache to get rid of entry created by persist
        s.getSessionFactory().getCache().evictAllRegions();
        s.beginTransaction();
        LazyAttributeEntity proxy = s.load(LazyAttributeEntity.class, persistedEntity.getId());
        assertEntityHasExpectedFieldsValues(proxy);
        s.getTransaction().commit();
        s.close();
    }

    private void loadEntityFromL2Cache() {
        Session s = openSession();
        s.beginTransaction();
        LazyAttributeEntity proxy = s.load(LazyAttributeEntity.class, persistedEntity.getId());
        assertEntityHasExpectedFieldsValues(proxy);
        s.getTransaction().commit();
        s.close();
    }

    private void assertEntityHasExpectedFieldsValues(LazyAttributeEntity entity) {
        assertNotNull(entity.getDate());
        assertNotNull(entity.getString());
        assertEquals(Integer.MAX_VALUE, entity.getIntVal());
        assertEquals(Long.MAX_VALUE, entity.getLongVal());
    }
}
