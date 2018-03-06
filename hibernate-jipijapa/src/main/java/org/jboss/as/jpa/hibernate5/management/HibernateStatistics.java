/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5.management;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Cache;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;
import org.jipijapa.management.spi.Statistics;

/**
 * HibernateStatistics
 *
 * @author Scott Marlow
 */
public class HibernateStatistics extends HibernateAbstractStatistics {

    public static final String PROVIDER_LABEL = "hibernate-persistence-unit";
    public static final String OPERATION_CLEAR = "clear";
    public static final String OPERATION_EVICTALL = "evict-all";
    public static final String OPERATION_SUMMARY = "summary";
    public static final String OPERATION_STATISTICS_ENABLED_DEPRECATED = "enabled";    // deprecated by JIPI-28
    public static final String OPERATION_STATISTICS_ENABLED = "statistics-enabled";
    public static final String OPERATION_ENTITY_DELETE_COUNT = "entity-delete-count";
    public static final String OPERATION_ENTITY_INSERT_COUNT = "entity-insert-count";
    public static final String OPERATION_ENTITY_LOAD_COUNT = "entity-load-count";
    public static final String OPERATION_ENTITY_FETCH_COUNT = "entity-fetch-count";
    public static final String OPERATION_ENTITY_UPDATE_COUNT = "entity-update-count";
    public static final String OPERATION_COLLECTION_FETCH_COUNT = "collection-fetch-count";
    public static final String OPERATION_COLLECTION_LOAD_COUNT = "collection-load-count";
    public static final String OPERATION_COLLECTION_RECREATED_COUNT = "collection-recreated-count";
    public static final String OPERATION_COLLECTION_REMOVE_COUNT = "collection-remove-count";
    public static final String OPERATION_COLLECTION_UPDATE_COUNT = "collection-update-count";
    public static final String OPERATION_QUERYCACHE_HIT_COUNT = "query-cache-hit-count";
    public static final String OPERATION_QUERYCACHE_MISS_COUNT ="query-cache-miss-count";
    public static final String OPERATION_QUERYQUERYCACHE_PUT_COUNT ="query-cache-put-count";
    public static final String OPERATION_QUERYEXECUTION_COUNT ="query-execution-count";
    public static final String OPERATION_QUERYEXECUTION_MAX_TIME ="query-execution-max-time";
    public static final String OPERATION_QUERYEXECUTION_MAX_TIME_STRING ="query-execution-max-time-query-string";
    public static final String OPERATION_SECONDLEVELCACHE_HIT_COUNT= "second-level-cache-hit-count";
    public static final String OPERATION_SECONDLEVELCACHE_MISS_COUNT="second-level-cache-miss-count";
    public static final String OPERATION_SECONDLEVELCACHE_PUT_COUNT="second-level-cache-put-count";

    public static final String OPERATION_FLUSH_COUNT = "flush-count";
    public static final String OPERATION_CONNECT_COUNT = "connect-count";
    public static final String OPERATION_SESSION_CLOSE_COUNT = "session-close-count";
    public static final String OPERATION_SESSION_OPEN_COUNT = "session-open-count";
    public static final String OPERATION_SUCCESSFUL_TRANSACTION_COUNT = "successful-transaction-count";
    public static final String OPERATION_COMPLETED_TRANSACTION_COUNT = "completed-transaction-count";
    public static final String OPERATION_PREPARED_STATEMENT_COUNT = "prepared-statement-count";
    public static final String OPERATION_CLOSE_STATEMENT_COUNT = "close-statement-count";
    public static final String OPERATION_OPTIMISTIC_FAILURE_COUNT = "optimistic-failure-count";
    public static final String ENTITYCACHE = "entity-cache";
    public static final String COLLECTION = "collection";
    public static final String ENTITY = "entity";
    public static final String QUERYCACHE = "query-cache";

    private final Map<String, Statistics> childrenStatistics = new HashMap<String,Statistics>();
    public HibernateStatistics() {

        /**
         * specify the different operations
         */
        operations.put(PROVIDER_LABEL, label);
        types.put(PROVIDER_LABEL,String.class);

        operations.put(OPERATION_CLEAR, clear);
        types.put(OPERATION_CLEAR, Operation.class);

        operations.put(OPERATION_EVICTALL, evictAll);
        types.put(OPERATION_EVICTALL, Operation.class);

        operations.put(OPERATION_SUMMARY, summary);
        types.put(OPERATION_SUMMARY, Operation.class);

        operations.put(OPERATION_STATISTICS_ENABLED, statisticsEnabled);
        types.put(OPERATION_STATISTICS_ENABLED, Boolean.class);
        writeableNames.add(OPERATION_STATISTICS_ENABLED);   // make 'enabled' writeable

        operations.put(OPERATION_STATISTICS_ENABLED_DEPRECATED, statisticsEnabled);
        types.put(OPERATION_STATISTICS_ENABLED_DEPRECATED, Boolean.class);
        writeableNames.add(OPERATION_STATISTICS_ENABLED_DEPRECATED);   // make 'enabled' writeable

        operations.put(OPERATION_ENTITY_DELETE_COUNT, entityDeleteCount);
        types.put(OPERATION_ENTITY_DELETE_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_FETCH_COUNT, collectionFetchCount);
        types.put(OPERATION_COLLECTION_FETCH_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_LOAD_COUNT, collectionLoadCount);
        types.put(OPERATION_COLLECTION_LOAD_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_RECREATED_COUNT, collectionRecreatedCount);
        types.put(OPERATION_COLLECTION_RECREATED_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_REMOVE_COUNT, collectionRemoveCount);
        types.put(OPERATION_COLLECTION_REMOVE_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_UPDATE_COUNT, collectionUpdateCount);
        types.put(OPERATION_COLLECTION_UPDATE_COUNT, Long.class);

        operations.put(OPERATION_QUERYCACHE_HIT_COUNT, queryCacheHitCount);
        types.put(OPERATION_QUERYCACHE_HIT_COUNT, Long.class);

        operations.put(OPERATION_QUERYCACHE_MISS_COUNT, queryCacheMissCount);
        types.put(OPERATION_QUERYCACHE_MISS_COUNT, Long.class);

        operations.put(OPERATION_QUERYQUERYCACHE_PUT_COUNT, queryCachePutCount);
        types.put(OPERATION_QUERYQUERYCACHE_PUT_COUNT, Long.class);

        operations.put(OPERATION_QUERYEXECUTION_COUNT, queryExecutionCount);
        types.put(OPERATION_QUERYEXECUTION_COUNT, Long.class);

        operations.put(OPERATION_QUERYEXECUTION_MAX_TIME, queryExecutionMaxTime);
        types.put(OPERATION_QUERYEXECUTION_MAX_TIME, Long.class);

        operations.put(OPERATION_QUERYEXECUTION_MAX_TIME_STRING, queryExecutionMaxTimeString);
        types.put(OPERATION_QUERYEXECUTION_MAX_TIME_STRING, String.class);

        operations.put(OPERATION_ENTITY_INSERT_COUNT, entityInsertCount);
        types.put(OPERATION_ENTITY_INSERT_COUNT, Long.class);

        operations.put(OPERATION_ENTITY_LOAD_COUNT, entityLoadCount);
        types.put(OPERATION_ENTITY_LOAD_COUNT, Long.class);

        operations.put(OPERATION_ENTITY_FETCH_COUNT, entityFetchCount);
        types.put(OPERATION_ENTITY_FETCH_COUNT, Long.class);

        operations.put(OPERATION_ENTITY_UPDATE_COUNT, entityUpdateCount);
        types.put(OPERATION_ENTITY_UPDATE_COUNT, Long.class);

        operations.put(OPERATION_FLUSH_COUNT, flushCount);
        types.put(OPERATION_FLUSH_COUNT, Long.class);

        operations.put(OPERATION_CONNECT_COUNT, connectCount);
        types.put(OPERATION_CONNECT_COUNT, Long.class);

        operations.put(OPERATION_SESSION_CLOSE_COUNT, sessionCloseCount);
        types.put(OPERATION_SESSION_CLOSE_COUNT, Long.class);

        operations.put(OPERATION_SESSION_OPEN_COUNT, sessionOpenCount);
        types.put(OPERATION_SESSION_OPEN_COUNT, Long.class);

        operations.put(OPERATION_SUCCESSFUL_TRANSACTION_COUNT, transactionCount);
        types.put(OPERATION_SUCCESSFUL_TRANSACTION_COUNT, Long.class);

        operations.put(OPERATION_COMPLETED_TRANSACTION_COUNT, transactionCompletedCount);
        types.put(OPERATION_COMPLETED_TRANSACTION_COUNT, Long.class);

        operations.put(OPERATION_PREPARED_STATEMENT_COUNT, preparedStatementCount);
        types.put(OPERATION_PREPARED_STATEMENT_COUNT, Long.class);

        operations.put(OPERATION_CLOSE_STATEMENT_COUNT, closedStatementCount);
        types.put(OPERATION_CLOSE_STATEMENT_COUNT, Long.class);

        operations.put(OPERATION_OPTIMISTIC_FAILURE_COUNT, optimisticFailureCount);
        types.put(OPERATION_OPTIMISTIC_FAILURE_COUNT, Long.class);

        operations.put(OPERATION_SECONDLEVELCACHE_HIT_COUNT, secondLevelCacheHitCount);
        types.put(OPERATION_SECONDLEVELCACHE_HIT_COUNT, Long.class);

        operations.put(OPERATION_SECONDLEVELCACHE_MISS_COUNT, secondLevelCacheMissCount);
        types.put(OPERATION_SECONDLEVELCACHE_MISS_COUNT, Long.class);

        operations.put(OPERATION_SECONDLEVELCACHE_PUT_COUNT, secondLevelCachePutCount);
        types.put(OPERATION_SECONDLEVELCACHE_PUT_COUNT, Long.class);

        /**
         * Specify the children statistics
         */
        childrenNames.add(ENTITY);
        childrenStatistics.put(ENTITY, new HibernateEntityStatistics());

        childrenNames.add(ENTITYCACHE);
        childrenStatistics.put(ENTITYCACHE, new HibernateEntityCacheStatistics());

        childrenNames.add(COLLECTION);
        childrenStatistics.put(COLLECTION, new HibernateCollectionStatistics());

        childrenNames.add(QUERYCACHE);
        childrenStatistics.put(QUERYCACHE , new HibernateQueryCacheStatistics());

    }

    @Override
    public Statistics getChild(String childName) {
        return childrenStatistics.get(childName);
    }

    static final org.hibernate.stat.Statistics getStatistics(final EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory == null) {
            return null;
        }
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        if (sessionFactory != null) {
            return sessionFactory.getStatistics();
        }
        return null;
    }

    @Override
    public Collection<String> getDynamicChildrenNames(EntityManagerFactoryAccess entityManagerFactoryLookup, PathAddress pathAddress) {

        return Collections.EMPTY_LIST;
    }

    private Operation clear = new Operation() {
        @Override
        public Object invoke(Object... args) {
            getStatistics(getEntityManagerFactory(args)).clear();
            return null;
        }
    };

    private Operation label = new Operation() {
        @Override
        public Object invoke(Object... args) {
            PathAddress pathAddress = getPathAddress(args);
            if (pathAddress != null) {
                return pathAddress.getValue(PROVIDER_LABEL);
            }
            return "";
        }
    };

    private Operation evictAll = new Operation() {
        @Override
        public Object invoke(Object... args) {
            Cache secondLevelCache = getEntityManagerFactory(args).getCache();
            if (secondLevelCache != null) {
                secondLevelCache.evictAll();
            }
            return null;
        }
    };

    private Operation summary = new Operation() {
        @Override
        public Object invoke(Object... args) {
            getStatistics(getEntityManagerFactory(args)).logSummary();
            return null;
        }
    };

    private Operation statisticsEnabled = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            if (statistics != null) {
                if (args.length > 0 && args[0] instanceof Boolean) {
                    Boolean newValue = (Boolean) args[0];
                    statistics.setStatisticsEnabled(newValue.booleanValue());
                }
            return Boolean.valueOf(statistics.isStatisticsEnabled());
            }
         return null;
        }
    };

    private Operation entityDeleteCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getEntityDeleteCount() : 0);
        }
    };

    private Operation collectionLoadCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getCollectionLoadCount() : 0);
        }
    };

    private Operation collectionFetchCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getCollectionFetchCount() : 0);
        }
    };

    private Operation collectionRecreatedCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getCollectionRecreateCount() : 0);
        }
    };

    private Operation collectionRemoveCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getCollectionRemoveCount() : 0);
        }
    };

    private Operation collectionUpdateCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getCollectionUpdateCount() : 0);
        }
    };

    private Operation queryCacheHitCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getQueryCacheHitCount() : 0);
        }
    };
    private Operation queryCacheMissCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getQueryCacheMissCount() : 0);
        }
    };
    private Operation queryCachePutCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getQueryCachePutCount() : 0);
        }
    };
    private Operation queryExecutionCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getQueryExecutionCount() : 0);
        }
    };
    private Operation queryExecutionMaxTime = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getQueryExecutionMaxTime() : 0);
        }
    };
    private Operation queryExecutionMaxTimeString = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return String.valueOf(statistics != null ? statistics.getQueryExecutionMaxTimeQueryString() : 0);
        }
    };

    private Operation entityFetchCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getEntityFetchCount() : 0);
        }
    };

    private Operation entityInsertCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getEntityInsertCount() : 0);
        }
    };

    private Operation entityLoadCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getEntityLoadCount() : 0);
        }
    };

    private Operation entityUpdateCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getEntityUpdateCount() : 0);
        }
    };

    private Operation flushCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getFlushCount() : 0);
        }
    };

    private Operation connectCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getConnectCount() : 0);
        }
    };

    private Operation sessionCloseCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getSessionCloseCount() : 0);
        }
    };

    private Operation sessionOpenCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getSessionOpenCount() : 0);
        }
    };

    private Operation transactionCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getTransactionCount() : 0);
        }
    };

    private Operation transactionCompletedCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getSuccessfulTransactionCount() : 0);
        }
    };

    private Operation preparedStatementCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getPrepareStatementCount() : 0);
        }
    };

    private Operation closedStatementCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getCloseStatementCount() : 0);
        }
    };

    private Operation optimisticFailureCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getOptimisticFailureCount() : 0);
        }
    };

    private Operation secondLevelCacheHitCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getSecondLevelCacheHitCount() : 0);
        }
    };

    private Operation secondLevelCacheMissCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getSecondLevelCacheMissCount() : 0);
        }
    };

    private Operation secondLevelCachePutCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.Statistics statistics = getStatistics(getEntityManagerFactory(args));
            return Long.valueOf(statistics != null ? statistics.getSecondLevelCachePutCount() : 0);
        }
    };

}
