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

package org.hibernate.shards.defaultmock;

import org.hibernate.Cache;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cfg.Settings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author maxr@google.com (Max Ross)
 */
public class SessionFactoryDefaultMock implements SessionFactoryImplementor {

    @Override
    public SessionFactoryOptions getSessionFactoryOptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionBuilderImplementor withOptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session openSession() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session getCurrentSession() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatelessSessionBuilder withStatelessOptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatelessSession openStatelessSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatelessSession openStatelessSession(Connection connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassMetadata getClassMetadata(Class entityClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassMetadata getClassMetadata(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CollectionMetadata getCollectionMetadata(String roleName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ClassMetadata> getAllClassMetadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map getAllCollectionMetadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Statistics getStatistics() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cache getCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void evict(Class persistentClass) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void evict(Class persistentClass, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void evictEntity(String entityName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void evictEntity(String entityName, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void evictCollection(String roleName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void evictCollection(String roleName, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void evictQueries(String cacheRegion) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void evictQueries() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set getDefinedFilterNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsFetchProfileDefinition(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeHelper getTypeHelper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeResolver getTypeResolver() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties getProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityPersister getEntityPersister(String entityName) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, EntityPersister> getEntityPersisters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CollectionPersister getCollectionPersister(String role) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, CollectionPersister> getCollectionPersisters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JdbcServices getJdbcServices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dialect getDialect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Interceptor getInterceptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryPlanCache getQueryPlanCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type[] getReturnTypes(String queryString) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getReturnAliases(String queryString) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public ConnectionProvider getConnectionProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getImplementors(String className) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getImportedClassName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryCache getQueryCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryCache getQueryCache(String regionName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public UpdateTimestampsCache getUpdateTimestampsCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatisticsImplementor getStatisticsImplementor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamedQueryDefinition getNamedQuery(String queryName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSetMappingDefinition getResultSetMapping(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Region getSecondLevelCacheRegion(String regionName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Region getNaturalIdCacheRegion(String regionName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map getAllSecondLevelCacheRegions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLExceptionConverter getSQLExceptionConverter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SqlExceptionHelper getSQLExceptionHelper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Settings getSettings() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session openTemporarySession() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityNotFoundDelegate getEntityNotFoundDelegate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLFunctionRegistry getSqlFunctionRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FetchProfile getFetchProfile(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceRegistryImplementor getServiceRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addObserver(SessionFactoryObserver observer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getIdentifierType(String className) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIdentifierPropertyName(String className) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reference getReference() throws NamingException {
        throw new UnsupportedOperationException();
    }
}
