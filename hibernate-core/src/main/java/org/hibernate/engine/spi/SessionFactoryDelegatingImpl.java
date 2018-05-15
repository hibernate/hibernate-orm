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

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.Settings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.envers.AuditReader;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.ResultSetMappingDescriptor;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

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
	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public EntityTypeDescriptor getEntityPersister(String entityName) throws MappingException {
		return delegate.getEntityPersister( entityName );
	}

	@Override
	public PersistentCollectionDescriptor getCollectionPersister(String role) throws MappingException {
		return delegate.getCollectionPersister( role );
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
	public QueryEngine getQueryEngine() {
		return delegate.getQueryEngine();
	}

	@Override
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
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
	public RootGraphImplementor findEntityGraphByName(String name) {
		return delegate.findEntityGraphByName( name );
	}

	@Override
	public ResultSetMappingDescriptor getResultSetMapping(String name) {
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
	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return delegate.getSqmFunctionRegistry();
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
	public EntityTypeDescriptor locateEntityPersister(Class byClass) {
		return delegate.locateEntityPersister( byClass );
	}

	@Override
	public EntityTypeDescriptor locateEntityPersister(String byName) {
		return delegate.locateEntityPersister( byName );
	}

	@Override
	public DeserializationResolver getDeserializationResolver() {
		return delegate.getDeserializationResolver();
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
	public TypeConfiguration getTypeConfiguration() {
		return delegate.getTypeConfiguration();
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
	public HibernateCriteriaBuilder getCriteriaBuilder() {
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
	public AllowableParameterType resolveParameterBindType(Object bindValue) {
		return delegate.resolveParameterBindType( bindValue );
	}

	@Override
	public AllowableParameterType resolveParameterBindType(Class clazz) {
		return delegate.resolveParameterBindType( clazz );
	}

	@Override
	public AuditReader openAuditReader() {
		return delegate.openAuditReader();
	}
}
