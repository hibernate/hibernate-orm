/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

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
import org.hibernate.cfg.Settings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
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
	public SessionBuilderImplementor withOptions();

	/**
	 * Retrieve the {@link Type} resolver associated with this factory.
	 *
	 * @return The type resolver
	 */
	public TypeResolver getTypeResolver();

	/**
	 * Get a copy of the Properties used to configure this session factory.
	 *
	 * @return The properties.
	 */
	public Properties getProperties();

	/**
	 * Get the persister for the named entity
	 *
	 * @param entityName The name of the entity for which to retrieve the persister.
	 * @return The persister
	 * @throws MappingException Indicates persister could not be found with that name.
	 */
	public EntityPersister getEntityPersister(String entityName) throws MappingException;

	/**
	 * Get all entity persisters as a Map, which entity name its the key and the persister is the value.
	 *
	 * @return The Map contains all entity persisters.
	 */
	public Map<String,EntityPersister> getEntityPersisters();

	/**
	 * Get the persister object for a collection role.
	 *
	 * @param role The role (name) of the collection for which to retrieve the
	 * persister.
	 * @return The persister
	 * @throws MappingException Indicates persister could not be found with that role.
	 */
	public CollectionPersister getCollectionPersister(String role) throws MappingException;

	/**
	 * Get all collection persisters as a Map, which collection role as the key and the persister is the value.
	 *
	 * @return The Map contains all collection persisters.
	 */
	public Map<String, CollectionPersister> getCollectionPersisters();

	/**
	 * Get the JdbcServices.
	 * @return the JdbcServices
	 */
	public JdbcServices getJdbcServices();

	/**
	 * Get the SQL dialect.
	 * <p/>
	 * Shorthand for {@code getJdbcServices().getDialect()}
	 *
	 * @return The dialect
	 */
	public Dialect getDialect();

	/**
	 * Get the factory scoped interceptor for this factory.
	 *
	 * @return The factory scope interceptor, or null if none.
	 */
	public Interceptor getInterceptor();

	public QueryPlanCache getQueryPlanCache();

	/**
	 * Get the return types of a query
	 */
	public Type[] getReturnTypes(String queryString) throws HibernateException;

	/**
	 * Get the return aliases of a query
	 */
	public String[] getReturnAliases(String queryString) throws HibernateException;

	/**
	 * Get the connection provider
	 *
	 * @deprecated Access to connections via {@link org.hibernate.engine.jdbc.spi.JdbcConnectionAccess} should
	 * be preferred over access via {@link ConnectionProvider}, whenever possible.
	 * {@link org.hibernate.engine.jdbc.spi.JdbcConnectionAccess} is tied to the Hibernate Session to
	 * properly account for contextual information.  See {@link SessionImplementor#getJdbcConnectionAccess()}
	 */
	@Deprecated
	public ConnectionProvider getConnectionProvider();
	/**
	 * Get the names of all persistent classes that implement/extend the given interface/class
	 */
	public String[] getImplementors(String className) throws MappingException;
	/**
	 * Get a class name, using query language imports
	 */
	public String getImportedClassName(String name);

	/**
	 * Get the default query cache
	 */
	public QueryCache getQueryCache();
	/**
	 * Get a particular named query cache, or the default cache
	 * @param regionName the name of the cache region, or null for the default query cache
	 * @return the existing cache, or a newly created cache if none by that region name
	 */
	public QueryCache getQueryCache(String regionName) throws HibernateException;

	/**
	 * Get the cache of table update timestamps
	 */
	public UpdateTimestampsCache getUpdateTimestampsCache();
	/**
	 * Statistics SPI
	 */
	public StatisticsImplementor getStatisticsImplementor();

	public NamedQueryDefinition getNamedQuery(String queryName);

	public void registerNamedQueryDefinition(String name, NamedQueryDefinition definition);

	public NamedSQLQueryDefinition getNamedSQLQuery(String queryName);

	public void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition);

	public ResultSetMappingDefinition getResultSetMapping(String name);

	/**
	 * Get the identifier generator for the hierarchy
	 */
	public IdentifierGenerator getIdentifierGenerator(String rootEntityName);

	/**
	 * Get a named second-level cache region
	 *
	 * @param regionName The name of the region to retrieve.
	 * @return The region
	 */
	public Region getSecondLevelCacheRegion(String regionName);
	
	/**
	 * Get a named naturalId cache region
	 *
	 * @param regionName The name of the region to retrieve.
	 * @return The region
	 */
	public Region getNaturalIdCacheRegion(String regionName);

	/**
	 * Get a map of all the second level cache regions currently maintained in
	 * this session factory.  The map is structured with the region name as the
	 * key and the {@link Region} instances as the values.
	 *
	 * @return The map of regions
	 */
	public Map getAllSecondLevelCacheRegions();

	/**
	 * Retrieves the SQLExceptionConverter in effect for this SessionFactory.
	 *
	 * @return The SQLExceptionConverter for this SessionFactory.
	 *
	 */
	public SQLExceptionConverter getSQLExceptionConverter();
	   // TODO: deprecate???

	/**
	 * Retrieves the SqlExceptionHelper in effect for this SessionFactory.
	 *
	 * @return The SqlExceptionHelper for this SessionFactory.
	 */
	public SqlExceptionHelper getSQLExceptionHelper();

	public Settings getSettings();

	/**
	 * Get a nontransactional "current" session for Hibernate EntityManager
	 */
	public Session openTemporarySession() throws HibernateException;

	/**
	 * Retrieves a set of all the collection roles in which the given entity
	 * is a participant, as either an index or an element.
	 *
	 * @param entityName The entity name for which to get the collection roles.
	 * @return set of all the collection roles in which the given entityName participates.
	 */
	public Set<String> getCollectionRolesByEntityParticipant(String entityName);

	public EntityNotFoundDelegate getEntityNotFoundDelegate();

	public SQLFunctionRegistry getSqlFunctionRegistry();

	/**
	 * Retrieve fetch profile by name.
	 *
	 * @param name The name of the profile to retrieve.
	 * @return The profile definition
	 */
	public FetchProfile getFetchProfile(String name);

	public ServiceRegistryImplementor getServiceRegistry();

	public void addObserver(SessionFactoryObserver observer);

	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	/**
	 * Provides access to the named query repository
	 *
	 * @return The repository for named query definitions
	 */
	public NamedQueryRepository getNamedQueryRepository();

	Iterable<EntityNameResolver> iterateEntityNameResolvers();
}
