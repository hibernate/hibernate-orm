/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityGraph;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Metamodel;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
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
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

/**
 * Defines the internal contract between the <tt>SessionFactory</tt> and other parts of
 * Hibernate such as implementors of <tt>Type</tt>.
 *
 * @see org.hibernate.SessionFactory
 * @see org.hibernate.internal.SessionFactoryImpl
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionFactoryImplementor extends Mapping, SessionFactory, QueryParameterBindingTypeResolver {
	/**
	 * Get the UUID for this SessionFactory.  The value is generated as a {@link java.util.UUID}, but kept
	 * as a String.
	 *
	 * @return The UUID for this SessionFactory.
	 *
	 * @see org.hibernate.internal.SessionFactoryRegistry#getSessionFactory
	 */
	String getUuid();

	/**
	 * Access to the name (if one) assigned to the SessionFactory
	 *
	 * @return The name for the SessionFactory
	 */
	String getName();

	@Override
	SessionBuilderImplementor withOptions();

	/**
	 * Get a non-transactional "current" session (used by hibernate-envers)
	 */
	Session openTemporarySession() throws HibernateException;

	@Override
	CacheImplementor getCache();

	@Override
	StatisticsImplementor getStatistics();

	/**
	 * Access to the ServiceRegistry for this SessionFactory.
	 *
	 * @return The factory's ServiceRegistry
	 */
	ServiceRegistryImplementor getServiceRegistry();

	/**
	 * Get the factory scoped interceptor for this factory.
	 *
	 * @return The factory scope interceptor, or null if none.
	 *
	 * @deprecated (since 5.2) if access to the SessionFactory-scoped Interceptor is needed, use
	 * {@link SessionFactoryOptions#getInterceptor()} instead.  However, generally speaking this access
	 * is not needed.
	 */
	@Deprecated
	Interceptor getInterceptor();

	/**
	 * Access to the cachres of HQL/JPQL and native query plans.
	 *
	 * @return The query plan cache
	 */
	QueryPlanCache getQueryPlanCache();

	/**
	 * Provides access to the named query repository
	 *
	 * @return The repository for named query definitions
	 */
	NamedQueryRepository getNamedQueryRepository();

	/**
	 * Retrieve fetch profile by name.
	 *
	 * @param name The name of the profile to retrieve.
	 * @return The profile definition
	 */
	FetchProfile getFetchProfile(String name);

	/**
	 * Retrieve the {@link Type} resolver associated with this factory.
	 *
	 * @return The type resolver
	 */
	TypeResolver getTypeResolver();

	/**
	 * Get the identifier generator for the hierarchy
	 */
	IdentifierGenerator getIdentifierGenerator(String rootEntityName);


	EntityNotFoundDelegate getEntityNotFoundDelegate();

	SQLFunctionRegistry getSqlFunctionRegistry();


	void addObserver(SessionFactoryObserver observer);

	/**
	 * @todo make a Service ?
	 */
	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	/**
	 * @todo make a Service ?
	 */
	CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	/**
	 * @deprecated (since 5.2) use {@link #getMetamodel()} -> {@link MetamodelImplementor#getEntityNameResolvers()}
	 */
	@Deprecated
	default Iterable<EntityNameResolver> iterateEntityNameResolvers() {
		return getMetamodel().getEntityNameResolvers();
	}

	/**
	 * Contract for resolving this SessionFactory on deserialization
	 */
	interface DeserializationResolver<T extends SessionFactoryImplementor> extends Serializable {
		T resolve();
	}

	DeserializationResolver getDeserializationResolver();



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * Get the return types of a query
	 *
	 * @deprecated No replacement.
	 */
	@Deprecated
	default Type[] getReturnTypes(String queryString) {
		throw new UnsupportedOperationException( "Concept of query return org.hibernate.type.Types is no longer supported" );
	}

	/**
	 * Get the return aliases of a query
	 *
	 * @deprecated No replacement.
	 */
	@Deprecated
	default String[] getReturnAliases(String queryString) {
		throw new UnsupportedOperationException( "Access to of query return aliases via Sessionfactory is no longer supported" );
	}



	/**
	 * @deprecated Just use {@link #getStatistics} (with covariant return here as {@link StatisticsImplementor}).
	 */
	@Deprecated
	default StatisticsImplementor getStatisticsImplementor() {
		return getStatistics();
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NamedQueryRepository

	/**
	 * @deprecated (since 5.2) Use {@link NamedQueryRepository#getNamedQueryDefinition(java.lang.String)} instead.
	 */
	@Deprecated
	default NamedQueryDefinition getNamedQuery(String queryName) {
		return getNamedQueryRepository().getNamedQueryDefinition( queryName );
	}

	/**
	 * @deprecated (since 5.2) Use {@link NamedQueryRepository#registerNamedQueryDefinition} instead.
	 */
	@Deprecated
	default void registerNamedQueryDefinition(String name, NamedQueryDefinition definition) {
		getNamedQueryRepository().registerNamedQueryDefinition( name, definition );
	}

	/**
	 * @deprecated (since 5.2) Use {@link NamedQueryRepository#getNamedSQLQueryDefinition} instead.
	 */
	@Deprecated
	default NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
		return getNamedQueryRepository().getNamedSQLQueryDefinition( queryName );
	}

	/**
	 * @deprecated (since 5.2) Use {@link NamedQueryRepository#registerNamedSQLQueryDefinition} instead.
	 */
	@Deprecated
	default void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition) {
		getNamedQueryRepository().registerNamedSQLQueryDefinition( name, definition );
	}

	/**
	 * @deprecated (since 5.2) Use {@link NamedQueryRepository#getResultSetMappingDefinition} instead.
	 */
	@Deprecated
	default ResultSetMappingDefinition getResultSetMapping(String name) {
		return getNamedQueryRepository().getResultSetMappingDefinition( name );
	}

	/**
	 * Get the JdbcServices.
	 *
	 * @return the JdbcServices
	 */
	JdbcServices getJdbcServices();

	/**
	 * Get the SQL dialect.
	 * <p/>
	 * Shorthand for {@code getJdbcServices().getDialect()}
	 *
	 * @return The dialect
	 *
	 * @deprecated (since 5.2) instead, use this factory's {{@link #getServiceRegistry()}} ->
	 * {@link JdbcServices#getDialect()}
	 */
	@Deprecated
	default Dialect getDialect() {
		if ( getServiceRegistry() == null ) {
			throw new IllegalStateException( "Cannot determine dialect because serviceRegistry is null." );
		}

		return getServiceRegistry().getService( JdbcServices.class ).getDialect();
	}

	/**
	 * Retrieves the SQLExceptionConverter in effect for this SessionFactory.
	 *
	 * @return The SQLExceptionConverter for this SessionFactory.
	 *
	 * @deprecated since 5.0; use {@link JdbcServices#getSqlExceptionHelper()} ->
	 * {@link SqlExceptionHelper#getSqlExceptionConverter()} instead as obtained from {@link #getServiceRegistry()}
	 */
	@Deprecated
	default SQLExceptionConverter getSQLExceptionConverter() {
		return getServiceRegistry().getService( JdbcServices.class ).getSqlExceptionHelper().getSqlExceptionConverter();
	}

	/**
	 * Retrieves the SqlExceptionHelper in effect for this SessionFactory.
	 *
	 * @return The SqlExceptionHelper for this SessionFactory.
	 *
	 * @deprecated since 5.0; use {@link JdbcServices#getSqlExceptionHelper()} instead as
	 * obtained from {@link #getServiceRegistry()}
	 */
	@Deprecated
	default SqlExceptionHelper getSQLExceptionHelper() {
		return getServiceRegistry().getService( JdbcServices.class ).getSqlExceptionHelper();
	}

	/**
	 * @deprecated since 5.0; use {@link #getSessionFactoryOptions()} instead
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	Settings getSettings();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// map these to Metamodel


	@Override
	MetamodelImplementor getMetamodel();

	/**
	 * @deprecated (since 5.2) Use {@link MetamodelImplementor#entityPersister(Class)} instead.
	 */
	@Deprecated
	default EntityPersister getEntityPersister(String entityName) throws MappingException {
		return getMetamodel().entityPersister( entityName );
	}

	/**
	 * @deprecated (since 5.2) Use {@link MetamodelImplementor#entityPersisters} instead.
	 */
	@Deprecated
	default Map<String,EntityPersister> getEntityPersisters() {
		return getMetamodel().entityPersisters();
	}

	/**
	 * @deprecated (since 5.2) Use {@link MetamodelImplementor#collectionPersister(String)} instead.
	 */
	@Deprecated
	default CollectionPersister getCollectionPersister(String role) throws MappingException {
		return getMetamodel().collectionPersister( role );
	}

	/**
	 * @deprecated (since 5.2) Use {@link MetamodelImplementor#collectionPersisters} instead.
	 */
	@Deprecated
	default Map<String, CollectionPersister> getCollectionPersisters() {
		return getMetamodel().collectionPersisters();
	}

	/**
	 * @deprecated (since 5.2) Use {@link MetamodelImplementor#collectionPersisters} instead.
	 * Retrieves a set of all the collection roles in which the given entity
	 * is a participant, as either an index or an element.
	 *
	 * @param entityName The entity name for which to get the collection roles.
	 * @return set of all the collection roles in which the given entityName participates.
	 */
	@Deprecated
	default Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return getMetamodel().getCollectionRolesByEntityParticipant( entityName );
	}

	/**
	 * @deprecated (since 5.2) Use {@link MetamodelImplementor#locateEntityPersister(Class)} instead.
	 */
	@Deprecated
	default EntityPersister locateEntityPersister(Class byClass) {
		return getMetamodel().locateEntityPersister( byClass );
	}

	/**
	 * @deprecated (since 5.2) Use {@link MetamodelImplementor#locateEntityPersister(String)} instead.
	 */
	@Deprecated
	default EntityPersister locateEntityPersister(String byName) {
		return getMetamodel().locateEntityPersister( byName );
	}

	/**
	 * Get the names of all persistent classes that implement/extend the given interface/class
	 *
	 * @deprecated Use {@link Metamodel#getImplementors(java.lang.String)} instead
	 */
	@Deprecated
	default String[] getImplementors(String entityName) {
		return getMetamodel().getImplementors( entityName );
	}

	/**
	 * Get a class name, using query language imports
	 *
	 * @deprecated Use {@link Metamodel#getImportedClassName(java.lang.String)} instead
	 */
	@Deprecated
	default String getImportedClassName(String name) {
		return getMetamodel().getImportedClassName( name );
	}

	EntityGraph findEntityGraphByName(String name);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Move to CacheImplementor calls

	/**
	 * Get a named second-level cache region
	 *
	 * @param regionName The name of the region to retrieve.
	 *
	 * @return The name of the region
	 *
	 * @deprecated (since 5.2) Use this factory's {@link #getCache()} reference
	 * to access Region via {@link CacheImplementor#determineEntityRegionAccessStrategy} or
	 * {@link CacheImplementor#determineCollectionRegionAccessStrategy} instead.
	 */
	@Deprecated
	default Region getSecondLevelCacheRegion(String regionName) {
		final EntityRegionAccessStrategy entityRegionAccess = getCache().getEntityRegionAccess( regionName );
		if ( entityRegionAccess != null ) {
			return entityRegionAccess.getRegion();
		}

		final CollectionRegionAccessStrategy collectionRegionAccess = getCache().getCollectionRegionAccess( regionName );
		if ( collectionRegionAccess != null ) {
			return collectionRegionAccess.getRegion();
		}

		return null;
	}

	/**
	 * Find the "access strategy" for the named cache region.
	 *
	 * @param regionName The name of the region
	 *
	 * @return That region's "access strategy"
	 *
	 *
	 * @deprecated (since 5.2) Use this factory's {@link #getCache()} reference
	 * to access {@link CacheImplementor#determineEntityRegionAccessStrategy} or
	 * {@link CacheImplementor#determineCollectionRegionAccessStrategy} instead.
	 */
	@Deprecated
	default RegionAccessStrategy getSecondLevelCacheRegionAccessStrategy(String regionName) {
		final EntityRegionAccessStrategy entityRegionAccess = getCache().getEntityRegionAccess( regionName );
		if ( entityRegionAccess != null ) {
			return entityRegionAccess;
		}

		final CollectionRegionAccessStrategy collectionRegionAccess = getCache().getCollectionRegionAccess( regionName );
		if ( collectionRegionAccess != null ) {
			return collectionRegionAccess;
		}

		return null;
	}

	/**
	 * Get a named natural-id cache region
	 *
	 * @param regionName The name of the region to retrieve.
	 *
	 * @return The region
	 *
	 * @deprecated (since 5.2) Use this factory's {@link #getCache()} ->
	 * {@link CacheImplementor#getNaturalIdCacheRegionAccessStrategy(String)} ->
	 * {@link NaturalIdRegionAccessStrategy#getRegion()} instead.
	 */
	@Deprecated
	default Region getNaturalIdCacheRegion(String regionName) {
		return getCache().getNaturalIdCacheRegionAccessStrategy( regionName ).getRegion();
	}

	/**
	 * Find the "access strategy" for the named naturalId cache region.
	 *
	 * @param regionName The region name
	 *
	 * @return That region's "access strategy"
	 *
	 * @deprecated (since 5.2) Use this factory's {@link #getCache()} ->
	 * {@link CacheImplementor#getNaturalIdCacheRegionAccessStrategy(String)} instead.
	 */
	@Deprecated
	default RegionAccessStrategy getNaturalIdCacheRegionAccessStrategy(String regionName) {
		return getCache().getNaturalIdCacheRegionAccessStrategy( regionName );
	}

	/**
	 * Get a map of all the second level cache regions currently maintained in
	 * this session factory.  The map is structured with the region name as the
	 * key and the {@link Region} instances as the values.
	 *
	 * @return The map of regions
	 *
	 * @deprecated (since 5.2) with no direct replacement; use this factory's {@link #getCache()} reference
	 * to access cache objects as needed.
	 */
	@Deprecated
	Map getAllSecondLevelCacheRegions();

	/**
	 * Get the default query cache.
	 *
	 * @deprecated Use {@link CacheImplementor#getDefaultQueryCache()} instead
	 */
	@Deprecated
	default QueryCache getQueryCache() {
		return getCache().getDefaultQueryCache();
	}

	/**
	 * Get a particular named query cache, or the default cache
	 *
	 * @param regionName the name of the cache region, or null for the default query cache
	 *
	 * @return the existing cache, or a newly created cache if none by that region name
	 *
	 * @deprecated Use {@link CacheImplementor#getQueryCache(String)} instead
	 */
	@Deprecated
	default QueryCache getQueryCache(String regionName) {
		return getCache().getQueryCache( regionName );
	}

	/**
	 * Get the cache of table update timestamps
	 *
	 * @deprecated Use {@link CacheImplementor#getUpdateTimestampsCache()} instead
	 */
	@Deprecated
	default UpdateTimestampsCache getUpdateTimestampsCache() {
		return getCache().getUpdateTimestampsCache();
	}

}
