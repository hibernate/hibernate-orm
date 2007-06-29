//$Id: SessionFactory.java 8754 2005-12-05 23:36:59Z steveebersole $
package org.hibernate;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

import javax.naming.Referenceable;

import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;
import org.hibernate.engine.FilterDefinition;

/**
 * Creates <tt>Session</tt>s. Usually an application has a single <tt>SessionFactory</tt>.
 * Threads servicing client requests obtain <tt>Session</tt>s from the factory.<br>
 * <br>
 * Implementors must be threadsafe.<br>
 * <br>
 * <tt>SessionFactory</tt>s are immutable. The behaviour of a <tt>SessionFactory</tt> is
 * controlled by properties supplied at configuration time. These properties are defined
 * on <tt>Environment</tt>.
 *
 * @see Session
 * @see org.hibernate.cfg.Environment
 * @see org.hibernate.cfg.Configuration
 * @see org.hibernate.connection.ConnectionProvider
 * @see org.hibernate.transaction.TransactionFactory
 * @author Gavin King
 */
public interface SessionFactory extends Referenceable, Serializable {

	/**
	 * Open a <tt>Session</tt> on the given connection.
	 * <p>
	 * Note that the second-level cache will be disabled if you
	 * supply a JDBC connection. Hibernate will not be able to track
	 * any statements you might have executed in the same transaction.
	 * Consider implementing your own <tt>ConnectionProvider</tt>.
	 *
	 * @param connection a connection provided by the application.
	 * @return Session
	 */
	public org.hibernate.classic.Session openSession(Connection connection);

	/**
	 * Create database connection and open a <tt>Session</tt> on it, specifying an
	 * interceptor.
	 *
	 * @param interceptor a session-scoped interceptor
	 * @return Session
	 * @throws HibernateException
	 */
	public org.hibernate.classic.Session openSession(Interceptor interceptor) throws HibernateException;

	/**
	 * Open a <tt>Session</tt> on the given connection, specifying an interceptor.
	 * <p>
	 * Note that the second-level cache will be disabled if you
	 * supply a JDBC connection. Hibernate will not be able to track
	 * any statements you might have executed in the same transaction.
	 * Consider implementing your own <tt>ConnectionProvider</tt>.
	 *
	 * @param connection a connection provided by the application.
	 * @param interceptor a session-scoped interceptor
	 * @return Session
	 */
	public org.hibernate.classic.Session openSession(Connection connection, Interceptor interceptor);

	/**
	 * Create database connection and open a <tt>Session</tt> on it.
	 *
	 * @return Session
	 * @throws HibernateException
	 */
	public org.hibernate.classic.Session openSession() throws HibernateException;

	/**
	 * Obtains the current session.  The definition of what exactly "current"
	 * means controlled by the {@link org.hibernate.context.CurrentSessionContext} impl configured
	 * for use.
	 * <p/>
	 * Note that for backwards compatibility, if a {@link org.hibernate.context.CurrentSessionContext}
	 * is not configured but a JTA {@link org.hibernate.transaction.TransactionManagerLookup}
	 * is configured this will default to the {@link org.hibernate.context.JTASessionContext}
	 * impl.
	 *
	 * @return The current session.
	 * @throws HibernateException Indicates an issue locating a suitable current session.
	 */
	public org.hibernate.classic.Session getCurrentSession() throws HibernateException;

	/**
	 * Get the <tt>ClassMetadata</tt> associated with the given entity class
	 *
	 * @see org.hibernate.metadata.ClassMetadata
	 */
	public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException;

	/**
	 * Get the <tt>ClassMetadata</tt> associated with the given entity name
	 *
	 * @see org.hibernate.metadata.ClassMetadata
	 * @since 3.0
	 */
	public ClassMetadata getClassMetadata(String entityName) throws HibernateException;

	/**
	 * Get the <tt>CollectionMetadata</tt> associated with the named collection role
	 *
	 * @see org.hibernate.metadata.CollectionMetadata
	 */
	public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException;


	/**
	 * Get all <tt>ClassMetadata</tt> as a <tt>Map</tt> from entityname <tt>String</tt>
	 * to metadata object
	 *
	 * @see org.hibernate.metadata.ClassMetadata
	 * @return a map from <tt>String</tt> an entity name to <tt>ClassMetaData</tt>
	 * @since 3.0 changed key from <tt>Class</tt> to <tt>String</tt>
	 */
	public Map getAllClassMetadata() throws HibernateException;

	/**
	 * Get all <tt>CollectionMetadata</tt> as a <tt>Map</tt> from role name
	 * to metadata object
	 *
	 * @see org.hibernate.metadata.CollectionMetadata
	 * @return a map from <tt>String</tt> to <tt>CollectionMetadata</tt>
	 */
	public Map getAllCollectionMetadata() throws HibernateException;

	/**
	 * Get the statistics for this session factory
	 */
	public Statistics getStatistics();

	/**
	 * Destroy this <tt>SessionFactory</tt> and release all resources (caches,
	 * connection pools, etc). It is the responsibility of the application
	 * to ensure that there are no open <tt>Session</tt>s before calling
	 * <tt>close()</tt>.
	 */
	public void close() throws HibernateException;

	/**
	 * Was this <tt>SessionFactory</tt> already closed?
	 */
	public boolean isClosed();

	/**
	 * Evict all entries from the second-level cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evict(Class persistentClass) throws HibernateException;
	/**
	 * Evict an entry from the second-level  cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evict(Class persistentClass, Serializable id) throws HibernateException;
	/**
	 * Evict all entries from the second-level cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evictEntity(String entityName) throws HibernateException;
	/**
	 * Evict an entry from the second-level  cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evictEntity(String entityName, Serializable id) throws HibernateException;
	/**
	 * Evict all entries from the second-level cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evictCollection(String roleName) throws HibernateException;
	/**
	 * Evict an entry from the second-level cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evictCollection(String roleName, Serializable id) throws HibernateException;

	/**
	 * Evict any query result sets cached in the default query cache region.
	 */
	public void evictQueries() throws HibernateException;
	/**
	 * Evict any query result sets cached in the named query cache region.
	 */
	public void evictQueries(String cacheRegion) throws HibernateException;
	/**
	 * Get a new stateless session.
	 */
	public StatelessSession openStatelessSession();
	/**
	 * Get a new stateless session for the given JDBC connection.
	 */
	public StatelessSession openStatelessSession(Connection connection);

	/**
	 * Obtain a set of the names of all filters defined on this SessionFactory.
	 *
	 * @return The set of filter names.
	 */
	public Set getDefinedFilterNames();

	/**
	 * Obtain the definition of a filter by name.
	 *
	 * @param filterName The name of the filter for which to obtain the definition.
	 * @return The filter definition.
	 * @throws HibernateException If no filter defined with the given name.
	 */
	public FilterDefinition getFilterDefinition(String filterName) throws HibernateException;
}
