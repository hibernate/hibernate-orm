/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.*;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.NamedQueryRepository;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

/**
 * Base delegating implementation of the SessionFactory and SessionFactoryImplementor
 * contracts for intended for easier implementation of SessionFactory.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({"deprecation", "unused"})
public class SessionFactoryDelegatingImpl implements SessionFactoryImplementor, SessionFactory {
	private final SessionFactoryImplementor delegate;

	public SessionFactoryDelegatingImpl(SessionFactoryImplementor delegate) {
		this.delegate = delegate;
	}

	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		return delegate.getSessionFactoryOptions();
	}

	@Override
	public SessionBuilderImplementor withOptions() {
		return delegate.withOptions();
	}

	@Override
	public Session openSession() throws HibernateException {
		return delegate.openSession();
	}

	@Override
	public Session getCurrentSession() throws HibernateException {
		return delegate.getCurrentSession();
	}

	@Override
	public StatelessSessionBuilder withStatelessOptions() {
		return delegate.withStatelessOptions();
	}

	@Override
	public StatelessSession openStatelessSession() {
		return delegate.openStatelessSession();
	}

	@Override
	public StatelessSession openStatelessSession(Connection connection) {
		return delegate.openStatelessSession( connection );
	}

	@Override
	public ClassMetadata getClassMetadata(Class entityClass) {
		return delegate.getClassMetadata( entityClass );
	}

	@Override
	public ClassMetadata getClassMetadata(String entityName) {
		return delegate.getClassMetadata( entityName );
	}

	@Override
	public CollectionMetadata getCollectionMetadata(String roleName) {
		return delegate.getCollectionMetadata( roleName );
	}

	@Override
	public Map<String, ClassMetadata> getAllClassMetadata() {
		return delegate.getAllClassMetadata();
	}

	@Override
	public Map getAllCollectionMetadata() {
		return delegate.getAllCollectionMetadata();
	}

	@Override
	public Statistics getStatistics() {
		return delegate.getStatistics();
	}

	@Override
	public void close() throws HibernateException {
		delegate.close();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public Cache getCache() {
		return delegate.getCache();
	}

	@Override
	public Set getDefinedFilterNames() {
		return delegate.getDefinedFilterNames();
	}

	@Override
	public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
		return delegate.getFilterDefinition( filterName );
	}

	@Override
	public boolean containsFetchProfileDefinition(String name) {
		return delegate.containsFetchProfileDefinition( name );
	}

	@Override
	public TypeHelper getTypeHelper() {
		return delegate.getTypeHelper();
	}

	@Override
	public TypeResolver getTypeResolver() {
		return delegate.getTypeResolver();
	}

	@Override
	public Properties getProperties() {
		return delegate.getProperties();
	}

	@Override
	public EntityPersister getEntityPersister(String entityName) throws MappingException {
		return delegate.getEntityPersister( entityName );
	}

	@Override
	public Map<String, EntityPersister> getEntityPersisters() {
		return delegate.getEntityPersisters();
	}

	@Override
	public CollectionPersister getCollectionPersister(String role) throws MappingException {
		return delegate.getCollectionPersister( role );
	}

	@Override
	public Map<String, CollectionPersister> getCollectionPersisters() {
		return delegate.getCollectionPersisters();
	}

	@Override
	public JdbcServices getJdbcServices() {
		return delegate.getJdbcServices();
	}

	@Override
	public Dialect getDialect() {
		return delegate.getDialect();
	}

	@Override
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	@Override
	public QueryPlanCache getQueryPlanCache() {
		return delegate.getQueryPlanCache();
	}

	@Override
	public Type[] getReturnTypes(String queryString) throws HibernateException {
		return delegate.getReturnTypes( queryString );
	}

	@Override
	public String[] getReturnAliases(String queryString) throws HibernateException {
		return delegate.getReturnAliases( queryString );
	}

	@Override
	public String[] getImplementors(String className) throws MappingException {
		return delegate.getImplementors( className );
	}

	@Override
	public String getImportedClassName(String name) {
		return delegate.getImportedClassName( name );
	}

	@Override
	public QueryCache getQueryCache() {
		return delegate.getQueryCache();
	}

	@Override
	public QueryCache getQueryCache(String regionName) throws HibernateException {
		return delegate.getQueryCache( regionName );
	}

	@Override
	public UpdateTimestampsCache getUpdateTimestampsCache() {
		return delegate.getUpdateTimestampsCache();
	}

	@Override
	public StatisticsImplementor getStatisticsImplementor() {
		return delegate.getStatisticsImplementor();
	}

	@Override
	public NamedQueryDefinition getNamedQuery(String queryName) {
		return delegate.getNamedQuery( queryName );
	}

	@Override
	public void registerNamedQueryDefinition(String name, NamedQueryDefinition definition) {
		delegate.registerNamedQueryDefinition( name, definition );
	}

	@Override
	public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
		return delegate.getNamedSQLQuery( queryName );
	}

	@Override
	public void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition) {
		delegate.registerNamedQueryDefinition( name, definition );
	}

	@Override
	public ResultSetMappingDefinition getResultSetMapping(String name) {
		return delegate.getResultSetMapping( name );
	}

	@Override
	public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
		return delegate.getIdentifierGenerator( rootEntityName );
	}

	@Override
	public Region getSecondLevelCacheRegion(String regionName) {
		return delegate.getSecondLevelCacheRegion( regionName );
	}

	@Override
	public RegionAccessStrategy getSecondLevelCacheRegionAccessStrategy(String regionName) {
		return delegate.getSecondLevelCacheRegionAccessStrategy(regionName);
	}

	@Override
	public Region getNaturalIdCacheRegion(String regionName) {
		return delegate.getNaturalIdCacheRegion( regionName );
	}

	@Override
	public RegionAccessStrategy getNaturalIdCacheRegionAccessStrategy(String regionName) {
		return delegate.getNaturalIdCacheRegionAccessStrategy(regionName);
	}

	@Override
	public Map getAllSecondLevelCacheRegions() {
		return delegate.getAllSecondLevelCacheRegions();
	}

	@Override
	public SQLExceptionConverter getSQLExceptionConverter() {
		return delegate.getSQLExceptionConverter();
	}

	@Override
	public SqlExceptionHelper getSQLExceptionHelper() {
		return delegate.getSQLExceptionHelper();
	}

	@Override
	public Settings getSettings() {
		return delegate.getSettings();
	}

	@Override
	public Session openTemporarySession() throws HibernateException {
		return delegate.openTemporarySession();
	}

	@Override
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return delegate.getCollectionRolesByEntityParticipant( entityName );
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return delegate.getEntityNotFoundDelegate();
	}

	@Override
	public SQLFunctionRegistry getSqlFunctionRegistry() {
		return delegate.getSqlFunctionRegistry();
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return delegate.getFetchProfile( name );
	}

	@Override
	public ServiceRegistryImplementor getServiceRegistry() {
		return delegate.getServiceRegistry();
	}

	@Override
	public void addObserver(SessionFactoryObserver observer) {
		delegate.addObserver( observer );
	}

	@Override
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return delegate.getCustomEntityDirtinessStrategy();
	}

	@Override
	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
		return delegate.getCurrentTenantIdentifierResolver();
	}

	@Override
	public NamedQueryRepository getNamedQueryRepository() {
		return delegate.getNamedQueryRepository();
	}

	@Override
	public Iterable<EntityNameResolver> iterateEntityNameResolvers() {
		return delegate.iterateEntityNameResolvers();
	}

	@Override
	public EntityPersister locateEntityPersister(Class byClass) {
		return delegate.locateEntityPersister( byClass );
	}

	@Override
	public EntityPersister locateEntityPersister(String byName) {
		return delegate.locateEntityPersister( byName );
	}

	@Override
	public DeserializationResolver getDeserializationResolver() {
		return delegate.getDeserializationResolver();
	}

	@Override
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return delegate.getIdentifierGeneratorFactory();
	}

	@Override
	public Type getIdentifierType(String className) throws MappingException {
		return delegate.getIdentifierType( className );
	}

	@Override
	public String getIdentifierPropertyName(String className) throws MappingException {
		return delegate.getIdentifierPropertyName( className );
	}

	@Override
	public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
		return delegate.getReferencedPropertyType( className, propertyName );
	}

	@Override
	public Reference getReference() throws NamingException {
		return delegate.getReference();
	}
}
