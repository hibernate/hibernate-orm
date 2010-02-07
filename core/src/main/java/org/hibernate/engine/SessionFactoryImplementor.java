/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.sql.Connection;

import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.engine.query.QueryPlanCache;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.UpdateTimestampsCache;
import org.hibernate.cache.Region;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.stat.StatisticsImplementor;
import org.hibernate.type.Type;

/**
 * Defines the internal contract between the <tt>SessionFactory</tt> and other parts of
 * Hibernate such as implementors of <tt>Type</tt>.
 *
 * @see org.hibernate.SessionFactory
 * @see org.hibernate.impl.SessionFactoryImpl
 * @author Gavin King
 */
public interface SessionFactoryImplementor extends Mapping, SessionFactory {

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
	 * Get the persister object for a collection role.
	 *
	 * @param role The role (name) of the collection for which to retrieve the
	 * persister.
	 * @return The persister
	 * @throws MappingException Indicates persister could not be found with that role.
	 */
	public CollectionPersister getCollectionPersister(String role) throws MappingException;

	/**
	 * Get the SQL dialect.
	 * <p/>
	 * Shorthand for {@link #getSettings()}.{@link Settings#getDialect()}
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
	 */
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
	 * Get the JTA transaction manager
	 */
	public TransactionManager getTransactionManager();


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
	public NamedSQLQueryDefinition getNamedSQLQuery(String queryName);
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
	 */
	public SQLExceptionConverter getSQLExceptionConverter();

	public Settings getSettings();

	/**
	 * Get a nontransactional "current" session for Hibernate EntityManager
	 */
	public org.hibernate.classic.Session openTemporarySession() throws HibernateException;

	/**
	 * Open a session conforming to the given parameters.  Used mainly by
	 * {@link org.hibernate.context.JTASessionContext} for current session processing.
	 *
	 * @param connection The external jdbc connection to use, if one (i.e., optional).
	 * @param flushBeforeCompletionEnabled Should the session be auto-flushed
	 * prior to transaction completion?
	 * @param autoCloseSessionEnabled Should the session be auto-closed after
	 * transaction completion?
	 * @param connectionReleaseMode The release mode for managed jdbc connections.
	 * @return An appropriate session.
	 * @throws HibernateException
	 */
	public org.hibernate.classic.Session openSession(
			final Connection connection,
			final boolean flushBeforeCompletionEnabled,
			final boolean autoCloseSessionEnabled,
			final ConnectionReleaseMode connectionReleaseMode) throws HibernateException;

	/**
	 * Retrieves a set of all the collection roles in which the given entity
	 * is a participant, as either an index or an element.
	 *
	 * @param entityName The entity name for which to get the collection roles.
	 * @return set of all the collection roles in which the given entityName participates.
	 */
	public Set getCollectionRolesByEntityParticipant(String entityName);

	public EntityNotFoundDelegate getEntityNotFoundDelegate();

	public SQLFunctionRegistry getSqlFunctionRegistry();

	/**
	 * Retrieve fetch profile by name.
	 *
	 * @param name The name of the profile to retrieve.
	 * @return The profile definition
	 */
	public FetchProfile getFetchProfile(String name);

}
