/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
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
	public void loadEntity(String entityName);
	public void fetchEntity(String entityName);
	public void updateEntity(String entityName);
	public void insertEntity(String entityName);
	public void deleteEntity(String entityName);
	public void loadCollection(String role);
	public void fetchCollection(String role);
	public void updateCollection(String role);
	public void recreateCollection(String role);
	public void removeCollection(String role);
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