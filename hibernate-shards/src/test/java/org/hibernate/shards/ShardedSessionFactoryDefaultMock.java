/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.cache.Cache;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.UpdateTimestampsCache;
import org.hibernate.cfg.Settings;
import org.hibernate.classic.Session;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.query.QueryPlanCache;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;
import org.hibernate.shards.session.ShardedSession;
import org.hibernate.shards.session.ShardedSessionFactory;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.StatisticsImplementor;
import org.hibernate.type.Type;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardedSessionFactoryDefaultMock implements ShardedSessionFactoryImplementor {

  public Map<SessionFactoryImplementor, Set<ShardId>> getSessionFactoryShardIdMap() {
    throw new UnsupportedOperationException();
  }

  public Session openSession(Connection connection) {
    throw new UnsupportedOperationException();
  }

  public ShardedSession openSession(Interceptor interceptor)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Session openSession(Connection connection, Interceptor interceptor) {
    throw new UnsupportedOperationException();
  }

  public ShardedSession openSession() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Session getCurrentSession() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ClassMetadata getClassMetadata(Class persistentClass)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ClassMetadata getClassMetadata(String entityName)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public CollectionMetadata getCollectionMetadata(String roleName)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Map getAllClassMetadata() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Map getAllCollectionMetadata() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Statistics getStatistics() {
    throw new UnsupportedOperationException();
  }

  public void close() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean isClosed() {
    throw new UnsupportedOperationException();
  }

  public void evict(Class persistentClass) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evict(Class persistentClass, Serializable id)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictEntity(String entityName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictEntity(String entityName, Serializable id)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictCollection(String roleName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictCollection(String roleName, Serializable id)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictQueries() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictQueries(String cacheRegion) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public StatelessSession openStatelessSession() {
    throw new UnsupportedOperationException();
  }

  public StatelessSession openStatelessSession(Connection connection) {
    throw new UnsupportedOperationException();
  }

  public Set getDefinedFilterNames() {
    throw new UnsupportedOperationException();
  }

  public FilterDefinition getFilterDefinition(String filterName)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Reference getReference() throws NamingException {
    throw new UnsupportedOperationException();
  }

  public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
    throw new UnsupportedOperationException();
  }

  public SessionImplementor openControlSession() {
    throw new UnsupportedOperationException();
  }

  public boolean containsFactory(SessionFactoryImplementor factory) {
    throw new UnsupportedOperationException();
  }

  public List<SessionFactory> getSessionFactories() {
    throw new UnsupportedOperationException();
  }

  public EntityPersister getEntityPersister(String entityName)
      throws MappingException {
    throw new UnsupportedOperationException();
  }

  public CollectionPersister getCollectionPersister(String role)
      throws MappingException {
    throw new UnsupportedOperationException();
  }

  public Dialect getDialect() {
    throw new UnsupportedOperationException();
  }

  public Interceptor getInterceptor() {
    throw new UnsupportedOperationException();
  }

  public QueryPlanCache getQueryPlanCache() {
    throw new UnsupportedOperationException();
  }

  public Type[] getReturnTypes(String queryString) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public String[] getReturnAliases(String queryString)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ConnectionProvider getConnectionProvider() {
    throw new UnsupportedOperationException();
  }

  public String[] getImplementors(String className) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public String getImportedClassName(String name) {
    throw new UnsupportedOperationException();
  }

  public TransactionManager getTransactionManager() {
    throw new UnsupportedOperationException();
  }

  public QueryCache getQueryCache() {
    throw new UnsupportedOperationException();
  }

  public QueryCache getQueryCache(String regionName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public UpdateTimestampsCache getUpdateTimestampsCache() {
    throw new UnsupportedOperationException();
  }

  public StatisticsImplementor getStatisticsImplementor() {
    throw new UnsupportedOperationException();
  }

  public NamedQueryDefinition getNamedQuery(String queryName) {
    throw new UnsupportedOperationException();
  }

  public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
    throw new UnsupportedOperationException();
  }

  public ResultSetMappingDefinition getResultSetMapping(String name) {
    throw new UnsupportedOperationException();
  }

  public Cache getSecondLevelCacheRegion(String regionName) {
    throw new UnsupportedOperationException();
  }

  public Map getAllSecondLevelCacheRegions() {
    throw new UnsupportedOperationException();
  }

  public SQLExceptionConverter getSQLExceptionConverter() {
    throw new UnsupportedOperationException();
  }

  public Settings getSettings() {
    throw new UnsupportedOperationException();
  }

  public Session openTemporarySession() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Session openSession(final Connection connection,
      final boolean flushBeforeCompletionEnabled,
      final boolean autoCloseSessionEnabled,
      final ConnectionReleaseMode connectionReleaseMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Set getCollectionRolesByEntityParticipant(String entityName) {
    throw new UnsupportedOperationException();
  }

  public Type getIdentifierType(String className) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public String getIdentifierPropertyName(String className)
      throws MappingException {
    throw new UnsupportedOperationException();
  }

  public Type getReferencedPropertyType(String className, String propertyName)
      throws MappingException {
    throw new UnsupportedOperationException();
  }

  public EntityNotFoundDelegate getEntityNotFoundDelegate() {
    throw new UnsupportedOperationException();
  }

  public SQLFunctionRegistry getSqlFunctionRegistry() {
    throw new UnsupportedOperationException();
  }

  public ShardedSessionFactory getSessionFactory(List<ShardId> shardIds,
      ShardStrategyFactory shardStrategyFactory) {
    throw new UnsupportedOperationException();
  }
}

