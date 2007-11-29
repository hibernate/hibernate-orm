//$Id$
package org.hibernate.stat;

/**
 * Statistics SPI for the Hibernate core
 * 
 * @author Emmanuel Bernard
 */
public interface StatisticsImplementor {
	public void openSession();
	public void closeSession();
	public void flush();
	public void connect();
	public void loadEntity(String entityName, long time);
	public void fetchEntity(String entityName, long time);
	public void updateEntity(String entityName, long time);
	public void insertEntity(String entityName, long time);
	public void deleteEntity(String entityName, long time);
	public void loadCollection(String role, long time);
	public void fetchCollection(String role, long time);
	public void updateCollection(String role, long time);
	public void recreateCollection(String role, long time);
	public void removeCollection(String role, long time);
	public void secondLevelCachePut(String regionName);
	public void secondLevelCacheHit(String regionName);
	public void secondLevelCacheMiss(String regionName);
	public void queryExecuted(String hql, int rows, long time);
	public void queryCacheHit(String hql, String regionName);
	public void queryCacheMiss(String hql, String regionName);
	public void queryCachePut(String hql, String regionName);
	public void endTransaction(boolean success);
	public void closeStatement();
	public void prepareStatement();
	public void optimisticFailure(String entityName);
}