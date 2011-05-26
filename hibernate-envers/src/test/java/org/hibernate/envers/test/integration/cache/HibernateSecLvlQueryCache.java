package org.hibernate.envers.test.integration.cache;

import org.hibernate.MappingException;
import org.hibernate.cache.internal.EhCacheProvider;
import org.hibernate.cfg.Environment;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.criteria.RevisionTypeAuditExpression;
import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.stat.Statistics;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class HibernateSecLvlQueryCache extends AbstractSessionTest {
    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        config.addAnnotatedClass(StrTestEntity.class);
        config.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
        config.setProperty(Environment.USE_QUERY_CACHE, "true");
        config.setProperty(Environment.CACHE_PROVIDER, EhCacheProvider.class.getName());
        URL cacheURL = Thread.currentThread().getContextClassLoader().getResource("ehCache.xml");
		config.setProperty(Environment.CACHE_PROVIDER_CONFIG, new File(cacheURL.toURI()).getAbsolutePath());
        config.setProperty(Environment.GENERATE_STATISTICS, "true");
    }

    @Test
    @Priority(10)
    public void initData() {
        // Revision 1
        getSession().getTransaction().begin();
        StrTestEntity ste = new StrTestEntity("data");
        getSession().persist(ste);
        getSession().getTransaction().commit();
    }

    @Test
    public void testSecLvlCacheWithRevisionTypeDiskPersistent() {
        // Invoking the same query twice for caching purpose.
        invokeSampleCachingRevTypeQuery();
        invokeSampleCachingRevTypeQuery();
        
        assert getQueryCacheStatistics() > 0;
    }

    private void invokeSampleCachingRevTypeQuery() {
        // Cached query that requires serializing RevisionType variable when persisting to disk.
        getAuditReader().createQuery().forEntitiesAtRevision(StrTestEntity.class, 1)
                                      .add(new RevisionTypeAuditExpression(RevisionType.ADD, "="))
                                      .setCacheable(true).getResultList();
    }

    private double getQueryCacheStatistics() {
        Statistics stats = getSession().getSessionFactory().getStatistics();

        double queryCacheHitCount  = stats.getQueryCacheHitCount();
        double queryCacheMissCount = stats.getQueryCacheMissCount();
        double queryCacheHitRatio = queryCacheHitCount / (queryCacheHitCount + queryCacheMissCount);

        return queryCacheHitRatio;
    }
}
