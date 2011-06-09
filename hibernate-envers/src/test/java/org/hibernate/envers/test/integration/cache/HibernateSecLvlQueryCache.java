package org.hibernate.envers.test.integration.cache;

import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.criteria.RevisionTypeAuditExpression;
import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;

import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class HibernateSecLvlQueryCache extends AbstractSessionTest {
    private static final String QUERY_CACHE_REGION = "queryCacheRegion";

    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        config.addAnnotatedClass(StrTestEntity.class);
        config.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
        config.setProperty(Environment.USE_QUERY_CACHE, "true");
		//i don't see there is any specific reason that we need ehcache, so change it to the testing cache
        config.setProperty(Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName());
//        config.setProperty(Environment.CACHE_PROVIDER_CONFIG, "ehcache-test.xml");
    }

    @Test
    @Priority(10)
    public void initData() {
        // Revision 1
        getSession().getTransaction().begin();
        StrTestEntity ste = new StrTestEntity("data");
        getSession().persist(ste);
        getSession().getTransaction().commit();

        // Evicting old query cache.
        getSession().getSessionFactory().getCache().evictQueryRegion(QUERY_CACHE_REGION);
    }

    @Test
    @TestForIssue(jiraKey="HHH-5025")
    public void testSecLvlCacheWithRevisionTypeDiskPersistent() throws InterruptedException {
        // Cached query that requires serializing RevisionType variable when persisting to disk.
        getAuditReader().createQuery().forEntitiesAtRevision(StrTestEntity.class, 1)
                                      .add(new RevisionTypeAuditExpression(RevisionType.ADD, "="))
                                      .setCacheable(true).setCacheRegion(QUERY_CACHE_REGION).getResultList();

        // Waiting max 3 seconds for cached data to persist to disk.
        for (int i=0; i<30; i++) {
            if (getQueryCacheSize() > 0) {
                break;
            }

            Thread.sleep(100);
        }

        Assert.assertTrue(getQueryCacheSize() > 0);
    }

    private int getQueryCacheSize() {
        // Statistics are not notified about persisting cached data failure. However, cache entry gets evicted.
        // See DiskWriteTask.call() method (net.sf.ehcache.store.compound.factories.DiskStorageFactory).
        SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) getSession().getSessionFactory();
        return sessionFactoryImplementor.getQueryCache(QUERY_CACHE_REGION).getRegion().toMap().size();
    }
}
