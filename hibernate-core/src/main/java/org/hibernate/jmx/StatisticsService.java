package org.hibernate.jmx;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Reference;

import org.jboss.logging.Logger;

import org.hibernate.SessionFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.NaturalIdCacheStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.internal.ConcurrentStatisticsImpl;

/**
 * JMX service for Hibernate statistics<br>
 * <br>
 * Register this MBean in your JMX server for a specific session factory
 * <pre>
 * //build the ObjectName you want
 * Hashtable tb = new Hashtable();
 * tb.put("type", "statistics");
 * tb.put("sessionFactory", "myFinancialApp");
 * ObjectName on = new ObjectName("hibernate", tb);
 * StatisticsService stats = new StatisticsService();
 * stats.setSessionFactory(sessionFactory);
 * server.registerMBean(stats, on);
 * </pre>
 * And call the MBean the way you want<br>
 * <br>
 * Register this MBean in your JMX server with no specific session factory
 * <pre>
 * //build the ObjectName you want
 * Hashtable tb = new Hashtable();
 * tb.put("type", "statistics");
 * tb.put("sessionFactory", "myFinancialApp");
 * ObjectName on = new ObjectName("hibernate", tb);
 * StatisticsService stats = new StatisticsService();
 * server.registerMBean(stats, on);
 * </pre>
 * And call the MBean by providing the <code>SessionFactoryJNDIName</code> first.
 * Then the session factory will be retrieved from JNDI and the statistics
 * loaded.
 *
 * @author Emmanuel Bernard
 * @deprecated See <a href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-6190">HHH-6190</a> for details
 */
@Deprecated
public class StatisticsService implements StatisticsServiceMBean {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, StatisticsService.class.getName() );
	//TODO: We probably should have a StatisticsNotPublishedException, to make it clean

	SessionFactory sf;
	String sfJNDIName;
	Statistics stats = new ConcurrentStatisticsImpl();

	/**
	 * @see StatisticsServiceMBean#setSessionFactoryJNDIName(java.lang.String)
	 */
	public void setSessionFactoryJNDIName(String sfJNDIName) {
		this.sfJNDIName = sfJNDIName;
		try {
			final SessionFactory sessionFactory;
			final Object jndiValue = new InitialContext().lookup( sfJNDIName );
			if ( jndiValue instanceof Reference ) {
				final String uuid = (String) ( (Reference) jndiValue ).get( 0 ).getContent();
				sessionFactory = SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid );
			}
			else {
				sessionFactory = (SessionFactory) jndiValue;
			}
			setSessionFactory( sessionFactory );
		}
		catch (NameNotFoundException e) {
			LOG.noSessionFactoryWithJndiName( sfJNDIName, e );
			setSessionFactory(null);
		}
		catch (NamingException e) {
			LOG.unableToAccessSessionFactory( sfJNDIName, e );
			setSessionFactory(null);
		}
		catch (ClassCastException e) {
			LOG.jndiNameDoesNotHandleSessionFactoryReference( sfJNDIName, e );
			setSessionFactory(null);
		}
	}

	/**
	 * Useful to init this MBean wo a JNDI session factory name
	 *
	 * @param sf session factory to register
	 */
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
		if (sf == null) {
			stats = new ConcurrentStatisticsImpl();
		}
		else {
			stats = sf.getStatistics();
		}

	}
	/**
	 * @see StatisticsServiceMBean#clear()
	 */
	public void clear() {
		stats.clear();
	}
	/**
	 * @see StatisticsServiceMBean#getEntityStatistics(java.lang.String)
	 */
	public EntityStatistics getEntityStatistics(String entityName) {
		return stats.getEntityStatistics(entityName);
	}
	/**
	 * @see StatisticsServiceMBean#getCollectionStatistics(java.lang.String)
	 */
	public CollectionStatistics getCollectionStatistics(String role) {
		return stats.getCollectionStatistics(role);
	}
	/**
	 * @see StatisticsServiceMBean#getSecondLevelCacheStatistics(java.lang.String)
	 */
	public SecondLevelCacheStatistics getSecondLevelCacheStatistics(String regionName) {
		return stats.getSecondLevelCacheStatistics(regionName);
	}
	/**
	 * @see StatisticsServiceMBean#getQueryStatistics(java.lang.String)
	 */
	public QueryStatistics getQueryStatistics(String hql) {
		return stats.getQueryStatistics(hql);
	}
	/**
	 * @see StatisticsServiceMBean#getEntityDeleteCount()
	 */
	public long getEntityDeleteCount() {
		return stats.getEntityDeleteCount();
	}
	/**
	 * @see StatisticsServiceMBean#getEntityInsertCount()
	 */
	public long getEntityInsertCount() {
		return stats.getEntityInsertCount();
	}
	/**
	 * @see StatisticsServiceMBean#getEntityLoadCount()
	 */
	public long getEntityLoadCount() {
		return stats.getEntityLoadCount();
	}
	/**
	 * @see StatisticsServiceMBean#getEntityFetchCount()
	 */
	public long getEntityFetchCount() {
		return stats.getEntityFetchCount();
	}
	/**
	 * @see StatisticsServiceMBean#getEntityUpdateCount()
	 */
	public long getEntityUpdateCount() {
		return stats.getEntityUpdateCount();
	}
	/**
	 * @see StatisticsServiceMBean#getQueryExecutionCount()
	 */
	public long getQueryExecutionCount() {
		return stats.getQueryExecutionCount();
	}
	public long getQueryCacheHitCount() {
		return stats.getQueryCacheHitCount();
	}
	public long getQueryExecutionMaxTime() {
		return stats.getQueryExecutionMaxTime();
	}
	public long getQueryCacheMissCount() {
		return stats.getQueryCacheMissCount();
	}
	public long getQueryCachePutCount() {
		return stats.getQueryCachePutCount();
	}

	public long getUpdateTimestampsCacheHitCount() {
		return stats.getUpdateTimestampsCacheHitCount();
	}

	public long getUpdateTimestampsCacheMissCount() {
		return stats.getUpdateTimestampsCacheMissCount();
	}

	public long getUpdateTimestampsCachePutCount() {
		return stats.getUpdateTimestampsCachePutCount();
	}

	/**
	 * @see StatisticsServiceMBean#getFlushCount()
	 */
	public long getFlushCount() {
		return stats.getFlushCount();
	}
	/**
	 * @see StatisticsServiceMBean#getConnectCount()
	 */
	public long getConnectCount() {
		return stats.getConnectCount();
	}
	/**
	 * @see StatisticsServiceMBean#getSecondLevelCacheHitCount()
	 */
	public long getSecondLevelCacheHitCount() {
		return stats.getSecondLevelCacheHitCount();
	}
	/**
	 * @see StatisticsServiceMBean#getSecondLevelCacheMissCount()
	 */
	public long getSecondLevelCacheMissCount() {
		return stats.getSecondLevelCacheMissCount();
	}
	/**
	 * @see StatisticsServiceMBean#getSecondLevelCachePutCount()
	 */
	public long getSecondLevelCachePutCount() {
		return stats.getSecondLevelCachePutCount();
	}
		
	public NaturalIdCacheStatistics getNaturalIdCacheStatistics(String regionName) {
		return stats.getNaturalIdCacheStatistics( regionName );
	}

	public long getNaturalIdCacheHitCount() {
		return stats.getNaturalIdCacheHitCount();
	}

	public long getNaturalIdCacheMissCount() {
		return stats.getNaturalIdCacheMissCount();
	}

	public long getNaturalIdCachePutCount() {
		return stats.getNaturalIdCachePutCount();
	}
	
	@Override
	public long getNaturalIdQueryExecutionCount() {
		return stats.getNaturalIdQueryExecutionCount();
	}

	@Override
	public long getNaturalIdQueryExecutionMaxTime() {
		return stats.getNaturalIdQueryExecutionMaxTime();
	}

	@Override
	public String getNaturalIdQueryExecutionMaxTimeRegion() {
		return stats.getNaturalIdQueryExecutionMaxTimeRegion();
	}

	/**
	 * @see StatisticsServiceMBean#getSessionCloseCount()
	 */
	public long getSessionCloseCount() {
		return stats.getSessionCloseCount();
	}
	/**
	 * @see StatisticsServiceMBean#getSessionOpenCount()
	 */
	public long getSessionOpenCount() {
		return stats.getSessionOpenCount();
	}
	/**
	 * @see StatisticsServiceMBean#getCollectionLoadCount()
	 */
	public long getCollectionLoadCount() {
		return stats.getCollectionLoadCount();
	}
	/**
	 * @see StatisticsServiceMBean#getCollectionFetchCount()
	 */
	public long getCollectionFetchCount() {
		return stats.getCollectionFetchCount();
	}
	/**
	 * @see StatisticsServiceMBean#getCollectionUpdateCount()
	 */
	public long getCollectionUpdateCount() {
		return stats.getCollectionUpdateCount();
	}
	/**
	 * @see StatisticsServiceMBean#getCollectionRemoveCount()
	 */
	public long getCollectionRemoveCount() {
		return stats.getCollectionRemoveCount();
	}
	/**
	 * @see StatisticsServiceMBean#getCollectionRecreateCount()
	 */
	public long getCollectionRecreateCount() {
		return stats.getCollectionRecreateCount();
	}
	/**
	 * @see StatisticsServiceMBean#getStartTime()
	 */
	public long getStartTime() {
		return stats.getStartTime();
	}

	/**
	 * @see StatisticsServiceMBean#isStatisticsEnabled()
	 */
	public boolean isStatisticsEnabled() {
		return stats.isStatisticsEnabled();
	}

	/**
	 * @see StatisticsServiceMBean#setStatisticsEnabled(boolean)
	 */
	public void setStatisticsEnabled(boolean enable) {
		stats.setStatisticsEnabled(enable);
	}

	public void logSummary() {
		stats.logSummary();
	}

	public String[] getCollectionRoleNames() {
		return stats.getCollectionRoleNames();
	}

	public String[] getEntityNames() {
		return stats.getEntityNames();
	}

	public String[] getQueries() {
		return stats.getQueries();
	}

	public String[] getSecondLevelCacheRegionNames() {
		return stats.getSecondLevelCacheRegionNames();
	}

	public long getSuccessfulTransactionCount() {
		return stats.getSuccessfulTransactionCount();
	}
	public long getTransactionCount() {
		return stats.getTransactionCount();
	}

	public long getCloseStatementCount() {
		return stats.getCloseStatementCount();
	}
	public long getPrepareStatementCount() {
		return stats.getPrepareStatementCount();
	}

	public long getOptimisticFailureCount() {
		return stats.getOptimisticFailureCount();
	}

	public String getQueryExecutionMaxTimeQueryString() {
		return stats.getQueryExecutionMaxTimeQueryString();
	}
}
