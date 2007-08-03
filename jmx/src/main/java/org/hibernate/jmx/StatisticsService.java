//$Id: StatisticsService.java 8262 2005-09-30 07:48:53Z oneovthafew $
package org.hibernate.jmx;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.impl.SessionFactoryObjectFactory;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.StatisticsImpl;

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
 */
public class StatisticsService implements StatisticsServiceMBean {
	
	//TODO: We probably should have a StatisticsNotPublishedException, to make it clean
	
	SessionFactory sf;
	String sfJNDIName;
	Logger log = LoggerFactory.getLogger(StatisticsService.class);
	Statistics stats = new StatisticsImpl();

	/**
	 * @see StatisticsServiceMBean#setSessionFactoryJNDIName(java.lang.String)
	 */
	public void setSessionFactoryJNDIName(String sfJNDIName) {
		this.sfJNDIName = sfJNDIName;
		try {
			Object obj = new InitialContext().lookup(sfJNDIName);
			if (obj instanceof Reference) {
				Reference ref = (Reference) obj;
				setSessionFactory( (SessionFactory) SessionFactoryObjectFactory.getInstance( (String) ref.get(0).getContent() ) );
			}
			else {
				setSessionFactory( (SessionFactory) obj );
			} 
		} 
		catch (NameNotFoundException e) {
			log.error("No session factory with JNDI name " + sfJNDIName, e);
			setSessionFactory(null);
		} 
		catch (NamingException e) {
			log.error("Error while accessing session factory with JNDI name " + sfJNDIName, e);
			setSessionFactory(null);
		} 
		catch (ClassCastException e) {
			log.error("JNDI name " + sfJNDIName + " does not handle a session factory reference", e);
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
			stats = new StatisticsImpl();
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
