/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
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
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.FastSessionServices;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.service.spi.ServiceRegistryImplementor;
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

	protected SessionFactoryImplementor delegate() {
		return delegate;
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
	public StatisticsImplementor getStatistics() {
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
	public CacheImplementor getCache() {
		return delegate.getCache();
	}

	@Override
	public PersistenceUnitUtil getPersistenceUnitUtil() {
		return delegate.getPersistenceUnitUtil();
	}

	@Override
	public void addNamedQuery(String name, Query query) {
		delegate.addNamedQuery( name, query );
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return delegate.unwrap( cls );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		delegate.addNamedEntityGraph( graphName, entityGraph );
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

	/**
	 * Retrieve the {@link Type} resolver associated with this factory.
	 *
	 * @return The type resolver
	 *
	 * @deprecated (since 5.3) No replacement, access to and handling of Types will be much different in 6.0
	 */
	@Deprecated
	public TypeResolver getTypeResolver() {
		return delegate.getTypeResolver();
	}

	@Override
	public Map<String, Object> getProperties() {
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
	public RootGraphImplementor findEntityGraphByName(String name) {
		return delegate.findEntityGraphByName( name );
	}

	@Override
	public StatisticsImplementor getStatisticsImplementor() {
		return delegate.getStatistics();
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
		delegate.registerNamedSQLQueryDefinition( name, definition );
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
	public FastSessionServices getFastSessionServices() {
		return delegate.getFastSessionServices();
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
	public String getUuid() {
		return delegate.getUuid();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public Reference getReference() throws NamingException {
		return delegate.getReference();
	}

	@Override
	public <T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		return delegate.findEntityGraphsByJavaType( entityClass );
	}

	@Override
	public EntityManager createEntityManager() {
		return delegate.createEntityManager();
	}

	@Override
	public EntityManager createEntityManager(Map map) {
		return delegate.createEntityManager( map );
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType) {
		return delegate.createEntityManager( synchronizationType );
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
		return delegate.createEntityManager( synchronizationType, map );
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	@Override
	public MetamodelImplementor getMetamodel() {
		return delegate.getMetamodel();
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public Type resolveParameterBindType(Object bindValue) {
		return delegate.resolveParameterBindType( bindValue );
	}

	@Override
	public Type resolveParameterBindType(Class clazz) {
		return delegate.resolveParameterBindType( clazz );
	}
}
