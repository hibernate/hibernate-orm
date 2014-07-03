/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.engine.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;

import org.hibernate.Cache;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cfg.Settings;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.NamedQueryRepository;
import org.hibernate.loader.custom.CustomQuery;
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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for setting and getting the enum/boolean values stored in the compressed state int.
 *
 * @author Gunnar Morling
 */
public class DefaultEntityEntryTest {

	@Test
	public void packedAttributesAreSetByConstructor() {
		EntityEntry entityEntry = createEntityEntry();

		assertEquals( LockMode.OPTIMISTIC, entityEntry.getLockMode() );
		Assert.assertEquals( Status.MANAGED, entityEntry.getStatus() );
		assertEquals( true, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
		assertEquals( true, entityEntry.isLoadedWithLazyPropertiesUnfetched() );
	}

	@Test
	public void testLockModeCanBeSetAndDoesNotAffectOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();

		assertEquals( LockMode.OPTIMISTIC, entityEntry.getLockMode() );
		assertEquals( Status.MANAGED, entityEntry.getStatus() );
		assertEquals( true, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
		assertEquals( true, entityEntry.isLoadedWithLazyPropertiesUnfetched() );

		// When
		entityEntry.setLockMode( LockMode.PESSIMISTIC_READ );

		// Then
		assertEquals( LockMode.PESSIMISTIC_READ, entityEntry.getLockMode() );
		assertEquals( Status.MANAGED, entityEntry.getStatus() );
		assertEquals( true, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
		assertEquals( true, entityEntry.isLoadedWithLazyPropertiesUnfetched() );
	}

	@Test
	public void testStatusCanBeSetAndDoesNotAffectOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();

		// When
		entityEntry.setStatus( Status.DELETED );

		// Then
		assertEquals( LockMode.OPTIMISTIC, entityEntry.getLockMode() );
		assertEquals( Status.DELETED, entityEntry.getStatus() );
		assertEquals( true, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
		assertEquals( true, entityEntry.isLoadedWithLazyPropertiesUnfetched() );
	}

	@Test
	public void testSetDeletedState() {
		// Given
		EntityEntry entityEntry = createEntityEntry();
		assertNull( entityEntry.getDeletedState() );

		//When
		entityEntry.setDeletedState( entityEntry.getLoadedState() );

		//Then
		assertNotNull( entityEntry.getDeletedState() );

	}

	@Test
	public void testPostDeleteSetsStatusAndExistsInDatabaseWithoutAffectingOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();

		// When
		entityEntry.postDelete();

		// Then
		assertEquals( LockMode.OPTIMISTIC, entityEntry.getLockMode() );
		assertEquals( Status.GONE, entityEntry.getStatus() );
		assertEquals( false, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
		assertEquals( true, entityEntry.isLoadedWithLazyPropertiesUnfetched() );
	}

	@Test
	public void testSerializationAndDeserializationKeepCorrectPackedAttributes() throws Exception {
		EntityEntry entityEntry = createEntityEntry();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		entityEntry.serialize(oos);
		oos.flush();

		InputStream is = new ByteArrayInputStream( baos.toByteArray() );
		EntityEntry deserializedEntry = DefaultEntityEntry.deserialize(new ObjectInputStream( is ), new StatefulPersistenceContext( new SessionImplementorMock() ) );

		assertEquals( LockMode.OPTIMISTIC, deserializedEntry.getLockMode() );
		assertEquals( Status.MANAGED, deserializedEntry.getStatus() );
		assertEquals( true, deserializedEntry.isExistsInDatabase() );
		assertEquals( true, deserializedEntry.isBeingReplicated() );
		assertEquals( true, deserializedEntry.isLoadedWithLazyPropertiesUnfetched() );
	}

	private EntityEntry createEntityEntry() {
		return new DefaultEntityEntry(
				Status.MANAGED,                        // status
				new Object[]{},                        // loadedState
				1L,                                    // rowId
				42L,                                   // id
				23L,                                   // version
				LockMode.OPTIMISTIC,                   // lockMode
				true,                                  // existsInDatabase
				null,                                  // persister
				true,                                  // disableVersionIncrement
				true,                                  // lazyPropertiesAreUnfetched
				new StatefulPersistenceContext( new SessionImplementorMock() ) // persistenceContext)
		);
	}

	private static final class SessionImplementorMock implements SessionImplementor {
		private final SessionFactoryImplementor factory;

		public SessionImplementorMock() {
			this.factory = new SessionFactoryMock();
		}

		@Override
		public String getTenantIdentifier() {
			return null;
		}

		@Override
		public JdbcConnectionAccess getJdbcConnectionAccess() {
			return null;
		}

		@Override
		public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
			return null;
		}

		@Override
		public CacheKey generateCacheKey(Serializable id, Type type, String entityOrRoleName) {
			return null;
		}

		@Override
		public Interceptor getInterceptor() {
			return null;
		}

		@Override
		public void setAutoClear(boolean enabled) {

		}

		@Override
		public void disableTransactionAutoJoin() {

		}

		@Override
		public boolean isTransactionInProgress() {
			return false;
		}

		@Override
		public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException {

		}

		@Override
		public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable)
				throws HibernateException {
			return null;
		}

		@Override
		public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
			return null;
		}

		@Override
		public long getTimestamp() {
			return 0;
		}

		@Override
		public SessionFactoryImplementor getFactory() {
			return factory;
		}

		@Override
		public List list(String query, QueryParameters queryParameters) throws HibernateException {
			return null;
		}

		@Override
		public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
			return null;
		}

		@Override
		public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
			return null;
		}

		@Override
		public ScrollableResults scroll(Criteria criteria, ScrollMode scrollMode) {
			return null;
		}

		@Override
		public List list(Criteria criteria) {
			return null;
		}

		@Override
		public List listFilter(Object collection, String filter, QueryParameters queryParameters)
				throws HibernateException {
			return null;
		}

		@Override
		public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
				throws HibernateException {
			return null;
		}

		@Override
		public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
			return null;
		}

		@Override
		public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
			return null;
		}

		@Override
		public Serializable getContextEntityIdentifier(Object object) {
			return null;
		}

		@Override
		public String bestGuessEntityName(Object object) {
			return null;
		}

		@Override
		public String guessEntityName(Object entity) throws HibernateException {
			return null;
		}

		@Override
		public Object instantiate(String entityName, Serializable id) throws HibernateException {
			return null;
		}

		@Override
		public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
				throws HibernateException {
			return null;
		}

		@Override
		public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
				throws HibernateException {
			return null;
		}

		@Override
		public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
			return null;
		}

		@Override
		public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
				throws HibernateException {
			return null;
		}

		@Override
		public Object getFilterParameterValue(String filterParameterName) {
			return null;
		}

		@Override
		public Type getFilterParameterType(String filterParameterName) {
			return null;
		}

		@Override
		public Map getEnabledFilters() {
			return null;
		}

		@Override
		public int getDontFlushFromFind() {
			return 0;
		}

		@Override
		public PersistenceContext getPersistenceContext() {
			return null;
		}

		@Override
		public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
			return 0;
		}

		@Override
		public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters)
				throws HibernateException {
			return 0;
		}

		@Override
		public CacheMode getCacheMode() {
			return null;
		}

		@Override
		public void setCacheMode(CacheMode cm) {

		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public boolean isConnected() {
			return false;
		}

		@Override
		public FlushMode getFlushMode() {
			return null;
		}

		@Override
		public void setFlushMode(FlushMode fm) {

		}

		@Override
		public Connection connection() {
			return null;
		}

		@Override
		public void flush() {

		}

		@Override
		public Query getNamedQuery(String name) {
			return null;
		}

		@Override
		public Query getNamedSQLQuery(String name) {
			return null;
		}

		@Override
		public boolean isEventSource() {
			return false;
		}

		@Override
		public void afterScrollOperation() {

		}

		@Override
		public String getFetchProfile() {
			return null;
		}

		@Override
		public void setFetchProfile(String name) {

		}

		@Override
		public TransactionCoordinator getTransactionCoordinator() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public LoadQueryInfluencers getLoadQueryInfluencers() {
			return null;
		}

		@Override
		public Query createQuery(NamedQueryDefinition namedQueryDefinition) {
			return null;
		}

		@Override
		public SQLQuery createSQLQuery(NamedSQLQueryDefinition namedQueryDefinition) {
			return null;
		}

		@Override
		public SessionEventListenerManager getEventListenerManager() {
			return null;
		}

		@Override
		public <T> T execute(Callback<T> callback) {
			return null;
		}
	}

	private static final class SessionFactoryMock implements SessionFactoryImplementor {

		@Override
		public SessionFactoryOptions getSessionFactoryOptions() {
			return null;
		}

		@Override
		public SessionBuilderImplementor withOptions() {
			return null;
		}

		@Override
		public TypeResolver getTypeResolver() {
			return null;
		}

		@Override
		public Properties getProperties() {
			return null;
		}

		@Override
		public EntityPersister getEntityPersister(String entityName) throws MappingException {
			return null;
		}

		@Override
		public Map<String, EntityPersister> getEntityPersisters() {
			return null;
		}

		@Override
		public CollectionPersister getCollectionPersister(String role) throws MappingException {
			return null;
		}

		@Override
		public Map<String, CollectionPersister> getCollectionPersisters() {
			return null;
		}

		@Override
		public JdbcServices getJdbcServices() {
			return null;
		}

		@Override
		public Dialect getDialect() {
			return null;
		}

		@Override
		public Interceptor getInterceptor() {
			return null;
		}

		@Override
		public QueryPlanCache getQueryPlanCache() {
			return null;
		}

		@Override
		public Type[] getReturnTypes(String queryString) throws HibernateException {
			return new Type[0];
		}

		@Override
		public String[] getReturnAliases(String queryString) throws HibernateException {
			return new String[0];
		}

		@Override
		public ConnectionProvider getConnectionProvider() {
			return null;
		}

		@Override
		public String[] getImplementors(String className) throws MappingException {
			return new String[0];
		}

		@Override
		public String getImportedClassName(String name) {
			return null;
		}

		@Override
		public QueryCache getQueryCache() {
			return null;
		}

		@Override
		public QueryCache getQueryCache(String regionName) throws HibernateException {
			return null;
		}

		@Override
		public UpdateTimestampsCache getUpdateTimestampsCache() {
			return null;
		}

		@Override
		public StatisticsImplementor getStatisticsImplementor() {
			return null;
		}

		@Override
		public NamedQueryDefinition getNamedQuery(String queryName) {
			return null;
		}

		@Override
		public void registerNamedQueryDefinition(String name, NamedQueryDefinition definition) {

		}

		@Override
		public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
			return null;
		}

		@Override
		public void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition) {

		}

		@Override
		public ResultSetMappingDefinition getResultSetMapping(String name) {
			return null;
		}

		@Override
		public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
			return null;
		}

		@Override
		public Region getSecondLevelCacheRegion(String regionName) {
			return null;
		}

		@Override
		public Region getNaturalIdCacheRegion(String regionName) {
			return null;
		}

		@Override
		public Map getAllSecondLevelCacheRegions() {
			return null;
		}

		@Override
		public SQLExceptionConverter getSQLExceptionConverter() {
			return null;
		}

		@Override
		public SqlExceptionHelper getSQLExceptionHelper() {
			return null;
		}

		@Override
		public Settings getSettings() {
			return null;
		}

		@Override
		public Session openTemporarySession() throws HibernateException {
			return null;
		}

		@Override
		public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
			return null;
		}

		@Override
		public EntityNotFoundDelegate getEntityNotFoundDelegate() {
			return null;
		}

		@Override
		public SQLFunctionRegistry getSqlFunctionRegistry() {
			return null;
		}

		@Override
		public FetchProfile getFetchProfile(String name) {
			return null;
		}

		@Override
		public ServiceRegistryImplementor getServiceRegistry() {
			return null;
		}

		@Override
		public void addObserver(SessionFactoryObserver observer) {

		}

		@Override
		public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
			return null;
		}

		@Override
		public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
			return null;
		}

		@Override
		public NamedQueryRepository getNamedQueryRepository() {
			return null;
		}

		@Override
		public Iterable<EntityNameResolver> iterateEntityNameResolvers() {
			return null;
		}

		@Override
		public Session openSession() throws HibernateException {
			return null;
		}

		@Override
		public Session getCurrentSession() throws HibernateException {
			return null;
		}

		@Override
		public StatelessSessionBuilder withStatelessOptions() {
			return null;
		}

		@Override
		public StatelessSession openStatelessSession() {
			return null;
		}

		@Override
		public StatelessSession openStatelessSession(Connection connection) {
			return null;
		}

		@Override
		public ClassMetadata getClassMetadata(Class entityClass) {
			return null;
		}

		@Override
		public ClassMetadata getClassMetadata(String entityName) {
			return null;
		}

		@Override
		public CollectionMetadata getCollectionMetadata(String roleName) {
			return null;
		}

		@Override
		public Map<String, ClassMetadata> getAllClassMetadata() {
			return null;
		}

		@Override
		public Map getAllCollectionMetadata() {
			return null;
		}

		@Override
		public Statistics getStatistics() {
			return null;
		}

		@Override
		public void close() throws HibernateException {

		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public Cache getCache() {
			return null;
		}

		@Override
		public void evict(Class persistentClass) throws HibernateException {

		}

		@Override
		public void evict(Class persistentClass, Serializable id) throws HibernateException {

		}

		@Override
		public void evictEntity(String entityName) throws HibernateException {

		}

		@Override
		public void evictEntity(String entityName, Serializable id) throws HibernateException {

		}

		@Override
		public void evictCollection(String roleName) throws HibernateException {

		}

		@Override
		public void evictCollection(String roleName, Serializable id) throws HibernateException {

		}

		@Override
		public void evictQueries(String cacheRegion) throws HibernateException {

		}

		@Override
		public void evictQueries() throws HibernateException {

		}

		@Override
		public Set getDefinedFilterNames() {
			return null;
		}

		@Override
		public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
			return null;
		}

		@Override
		public boolean containsFetchProfileDefinition(String name) {
			return false;
		}

		@Override
		public TypeHelper getTypeHelper() {
			return null;
		}

		@Override
		public Reference getReference() throws NamingException {
			return null;
		}

		@Override
		public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
			return null;
		}

		@Override
		public Type getIdentifierType(String className) throws MappingException {
			return null;
		}

		@Override
		public String getIdentifierPropertyName(String className) throws MappingException {
			return null;
		}

		@Override
		public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
			return null;
		}
	}
}
