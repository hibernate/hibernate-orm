/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
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
import org.hibernate.internal.NamedQueryRepository;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
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
 * @author Gavin King
 */
public interface SessionFactoryImplementor extends Mapping, SessionFactory {
	@Override
	SessionBuilderImplementor withOptions();

	/**
	 * Retrieve the {@link Type} resolver associated with this factory.
	 *
	 * @return The type resolver
	 */
	TypeResolver getTypeResolver();

	/**
	 * Get a copy of the Properties used to configure this session factory.
	 *
	 * @return The properties.
	 */
	Properties getProperties();

	/**
	 * Get the persister for the named entity
	 *
	 * @param entityName The name of the entity for which to retrieve the persister.
	 * @return The persister
	 * @throws MappingException Indicates persister could not be found with that name.
	 */
	EntityPersister getEntityPersister(String entityName) throws MappingException;

	/**
	 * Get all entity persisters as a Map, which entity name its the key and the persister is the value.
	 *
	 * @return The Map contains all entity persisters.
	 */
	Map<String,EntityPersister> getEntityPersisters();

	/**
	 * Get the persister object for a collection role.
	 *
	 * @param role The role (name) of the collection for which to retrieve the
	 * persister.
	 * @return The persister
	 * @throws MappingException Indicates persister could not be found with that role.
	 */
	CollectionPersister getCollectionPersister(String role) throws MappingException;

	/**
	 * Get all collection persisters as a Map, which collection role as the key and the persister is the value.
	 *
	 * @return The Map contains all collection persisters.
	 */
	Map<String, CollectionPersister> getCollectionPersisters();

	/**
	 * Get the JdbcServices.
	 *
	 * @return the JdbcServices
	 *
	 * @deprecated since 5.0; use {@link #getServiceRegistry()} instead to locate the JdbcServices
	 */
	@Deprecated
	JdbcServices getJdbcServices();

	/**
	 * Get the SQL dialect.
	 * <p/>
	 * Shorthand for {@code getJdbcServices().getDialect()}
	 *
	 * @return The dialect
	 */
	Dialect getDialect();

	/**
	 * Get the factory scoped interceptor for this factory.
	 *
	 * @return The factory scope interceptor, or null if none.
	 */
	Interceptor getInterceptor();

	QueryPlanCache getQueryPlanCache();

	/**
	 * Get the return types of a query
	 */
	Type[] getReturnTypes(String queryString) throws HibernateException;

	/**
	 * Get the return aliases of a query
	 */
	String[] getReturnAliases(String queryString) throws HibernateException;

	/**
	 * Get the names of all persistent classes that implement/extend the given interface/class
	 */
	String[] getImplementors(String className) throws MappingException;
	/**
	 * Get a class name, using query language imports
	 */
	String getImportedClassName(String name);

	/**
	 * Get the default query cache
	 */
	QueryCache getQueryCache();
	/**
	 * Get a particular named query cache, or the default cache
	 * @param regionName the name of the cache region, or null for the default query cache
	 * @return the existing cache, or a newly created cache if none by that region name
	 */
	QueryCache getQueryCache(String regionName) throws HibernateException;

	/**
	 * Get the cache of table update timestamps
	 */
	UpdateTimestampsCache getUpdateTimestampsCache();
	/**
	 * Statistics SPI
	 */
	StatisticsImplementor getStatisticsImplementor();

	NamedQueryDefinition getNamedQuery(String queryName);

	void registerNamedQueryDefinition(String name, NamedQueryDefinition definition);

	NamedSQLQueryDefinition getNamedSQLQuery(String queryName);

	void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition);

	ResultSetMappingDefinition getResultSetMapping(String name);

	/**
	 * Get the identifier generator for the hierarchy
	 */
	IdentifierGenerator getIdentifierGenerator(String rootEntityName);

	/**
	 * Get a named second-level cache region
	 *
	 * @param regionName The name of the region to retrieve.
	 * @return The region
	 */
	Region getSecondLevelCacheRegion(String regionName);

	/**
	 * Get access strategy to second-level cache region
	 * @param regionName
	 * @return
	 */
	RegionAccessStrategy getSecondLevelCacheRegionAccessStrategy(String regionName);
	
	/**
	 * Get a named naturalId cache region
	 *
	 * @param regionName The name of the region to retrieve.
	 * @return The region
	 */
	Region getNaturalIdCacheRegion(String regionName);

	/**
	 * Get access strategy to naturalId cache region
	 * @param regionName
	 * @return
	 */
	RegionAccessStrategy getNaturalIdCacheRegionAccessStrategy(String regionName);

	/**
	 * Get a map of all the second level cache regions currently maintained in
	 * this session factory.  The map is structured with the region name as the
	 * key and the {@link Region} instances as the values.
	 *
	 * @return The map of regions
	 */
	Map getAllSecondLevelCacheRegions();

	/**
	 * Retrieves the SQLExceptionConverter in effect for this SessionFactory.
	 *
	 * @return The SQLExceptionConverter for this SessionFactory.
	 *
	 * @deprecated since 5.0; use {@link JdbcServices#getSqlExceptionHelper()} ->
	 * {@link SqlExceptionHelper#getSqlExceptionConverter()} instead as obtained from {@link #getServiceRegistry()}
	 */
	@Deprecated
	SQLExceptionConverter getSQLExceptionConverter();

	/**
	 * Retrieves the SqlExceptionHelper in effect for this SessionFactory.
	 *
	 * @return The SqlExceptionHelper for this SessionFactory.
	 *
	 * @deprecated since 5.0; use {@link JdbcServices#getSqlExceptionHelper()} instead as
	 * obtained from {@link #getServiceRegistry()}
	 */
	@Deprecated
	SqlExceptionHelper getSQLExceptionHelper();

	/**
	 * @deprecated since 5.0; use {@link #getSessionFactoryOptions()} instead
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	Settings getSettings();

	/**
	 * Get a non-transactional "current" session for Hibernate EntityManager
	 */
	Session openTemporarySession() throws HibernateException;

	/**
	 * Retrieves a set of all the collection roles in which the given entity
	 * is a participant, as either an index or an element.
	 *
	 * @param entityName The entity name for which to get the collection roles.
	 * @return set of all the collection roles in which the given entityName participates.
	 */
	Set<String> getCollectionRolesByEntityParticipant(String entityName);

	EntityNotFoundDelegate getEntityNotFoundDelegate();

	SQLFunctionRegistry getSqlFunctionRegistry();

	/**
	 * Retrieve fetch profile by name.
	 *
	 * @param name The name of the profile to retrieve.
	 * @return The profile definition
	 */
	FetchProfile getFetchProfile(String name);

	ServiceRegistryImplementor getServiceRegistry();

	void addObserver(SessionFactoryObserver observer);

	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	/**
	 * Provides access to the named query repository
	 *
	 * @return The repository for named query definitions
	 */
	NamedQueryRepository getNamedQueryRepository();

	Iterable<EntityNameResolver> iterateEntityNameResolvers();

	/**
	 * Locate an EntityPersister by the entity class.  The passed Class might refer to either
	 * the entity name directly, or it might name a proxy interface for the entity.  This
	 * method accounts for both, preferring the direct named entity name.
	 *
	 * @param byClass The concrete Class or proxy interface for the entity to locate the persister for.
	 *
	 * @return The located EntityPersister, never {@code null}
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	 */
	EntityPersister locateEntityPersister(Class byClass);

	/**
	 * Locate the entity persister by name.
	 *
	 * @param byName The entity name
	 *
	 * @return The located EntityPersister, never {@code null}
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	 */
	EntityPersister locateEntityPersister(String byName);

	/**
	 * Contract for resolving this SessionFactory on deserialization
	 */
	interface DeserializationResolver<T extends SessionFactoryImplementor> extends Serializable {
		T resolve();
	}

	DeserializationResolver getDeserializationResolver();
}
