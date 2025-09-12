/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.creation.spi.SessionBuilderImplementor;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.spi.SqlTranslationEngine;
import org.hibernate.relational.SchemaManager;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Base delegating implementation of the {@link SessionFactory} and
 * {@link SessionFactoryImplementor} contracts for intended for easier
 * implementation of {@link SessionFactory}.
 *
 * @author Steve Ebersole
 */
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
	public SessionImplementor openSession() throws HibernateException {
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
	public SchemaManager getSchemaManager() {
		return delegate.getSchemaManager();
	}

	@Override
	public RuntimeMetamodelsImplementor getRuntimeMetamodels() {
		return delegate.getRuntimeMetamodels();
	}

	@Override
	public EventEngine getEventEngine() {
		return delegate.getEventEngine();
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
	public PersistenceUnitTransactionType getTransactionType() {
		return delegate.getTransactionType();
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
	public void runInTransaction(Consumer<EntityManager> work) {
		delegate.runInTransaction( work );
	}

	@Override
	public <R> R callInTransaction(Function<EntityManager, R> work) {
		return delegate.callInTransaction( work );
	}

	@Override
	public Set<String> getDefinedFilterNames() {
		return delegate.getDefinedFilterNames();
	}

	@Override @Deprecated
	public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
		return delegate.getFilterDefinition( filterName );
	}

	@Override
	public Collection<FilterDefinition> getAutoEnabledFilters() {
		return delegate.getAutoEnabledFilters();
	}

	@Override
	public boolean containsFetchProfileDefinition(String name) {
		return delegate.containsFetchProfileDefinition( name );
	}

	@Override
	public Set<String> getDefinedFetchProfileNames() {
		return delegate.getDefinedFetchProfileNames();
	}

	@Override @Deprecated
	public Generator getGenerator(String rootEntityName) {
		return delegate.getGenerator( rootEntityName );
	}

	@Override
	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public JdbcServices getJdbcServices() {
		return delegate.getJdbcServices();
	}

	@Override
	public SqlStringGenerationContext getSqlStringGenerationContext() {
		return delegate.getSqlStringGenerationContext();
	}

	@Override
	public RootGraph<Map<String, ?>> createGraphForDynamicEntity(String entityName) {
		return delegate.createGraphForDynamicEntity( entityName );
	}

	@Override
	public RootGraphImplementor<?> findEntityGraphByName(String name) {
		return delegate.findEntityGraphByName( name );
	}

	@Override
	public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
		return delegate.getNamedQueries( resultType );
	}

	@Override
	public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityType) {
		return delegate.getNamedEntityGraphs( entityType );
	}

	@Override
	public String bestGuessEntityName(Object object) {
		return delegate.bestGuessEntityName( object );
	}

	@Override
	public SessionImplementor openTemporarySession() throws HibernateException {
		return delegate.openTemporarySession();
	}

	@Deprecated
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return delegate.getMappingMetamodel().getCollectionRolesByEntityParticipant( entityName );
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return delegate.getEntityNotFoundDelegate();
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return delegate.getFetchProfile( name );
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return delegate.getJpaMetamodel();
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
	public CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver() {
		return delegate.getCurrentTenantIdentifierResolver();
	}

	@Override
	public JavaType<Object> getTenantIdentifierJavaType() {
		return delegate.getTenantIdentifierJavaType();
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
	public String getJndiName() {
		return delegate.getJndiName();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return delegate.getTypeConfiguration();
	}

	@Override
	public QueryEngine getQueryEngine() {
		return delegate.getQueryEngine();
	}

	@Override
	public SqlTranslationEngine getSqlTranslationEngine() {
		return delegate.getSqlTranslationEngine();
	}

	@Override
	public Reference getReference() throws NamingException {
		return delegate.getReference();
	}

	@Override
	public Session createEntityManager() {
		return delegate.createEntityManager();
	}

	@Override
	public Session createEntityManager(Map map) {
		return delegate.createEntityManager( map );
	}

	@Override
	public Session createEntityManager(SynchronizationType synchronizationType) {
		return delegate.createEntityManager( synchronizationType );
	}

	@Override
	public Session createEntityManager(SynchronizationType synchronizationType, Map map) {
		return delegate.createEntityManager( synchronizationType, map );
	}

	@Override
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	@Override @Deprecated
	public MappingMetamodel getMetamodel() {
		return (MappingMetamodel) delegate.getMetamodel();
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public WrapperOptions getWrapperOptions() {
		return delegate.getWrapperOptions();
	}

	@Override
	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		return delegate.findEntityGraphsByType(entityClass);
	}

	@Override
	public EventListenerGroups getEventListenerGroups() {
		return delegate.getEventListenerGroups();
	}

	@Override
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return delegate.getParameterMarkerStrategy();
	}

	@Override
	public JdbcValuesMappingProducerProvider getJdbcValuesMappingProducerProvider() {
		return delegate.getJdbcValuesMappingProducerProvider();
	}

	@Override
	public EntityCopyObserverFactory getEntityCopyObserver() {
		return delegate.getEntityCopyObserver();
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return delegate.getClassLoaderService();
	}

	@Override
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return delegate.getManagedBeanRegistry();
	}

	@Override
	public EventListenerRegistry getEventListenerRegistry() {
		return delegate.getEventListenerRegistry();
	}

	@Override
	public <R> TypedQueryReference<R> addNamedQuery(String name, TypedQuery<R> query) {
		return delegate.addNamedQuery( name, query );
	}

	@Override
	public Object resolveTenantIdentifier() {
		return delegate.resolveTenantIdentifier();
	}
}
