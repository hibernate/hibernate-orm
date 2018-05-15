/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import javax.naming.Referenceable;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.envers.internal.AuditReaderFactory;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.stat.Statistics;

/**
 * The main contract here is the creation of {@link Session} instances.  Usually
 * an application has a single {@link SessionFactory} instance and threads
 * servicing client requests obtain {@link Session} instances from this factory.
 * <p/>
 * The internal state of a {@link SessionFactory} is immutable.  Once it is created
 * this internal state is set.  This internal state includes all of the metadata
 * about Object/Relational Mapping.
 * <p/>
 * Implementors <strong>must</strong> be threadsafe.
 *
 * @see org.hibernate.cfg.Configuration
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionFactory extends EntityManagerFactory, AuditReaderFactory, Referenceable, Serializable, java.io.Closeable {
	/**
	 * Get the special options used to build the factory.
	 *
	 * @return The special options used to build the factory.
	 */
	SessionFactoryOptions getSessionFactoryOptions();

	@Override
	HibernateCriteriaBuilder getCriteriaBuilder();

	@Override
	Metamodel getMetamodel();

	/**
	 * Obtain a {@link Session} builder.
	 *
	 * @return The session builder
	 */
	SessionBuilder withOptions();

	/**
	 * Open a {@link Session}.
	 * <p/>
	 * JDBC {@link Connection connection(s} will be obtained from the
	 * configured {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} as needed
	 * to perform requested work.
	 *
	 * @return The created session.
	 *
	 * @throws HibernateException Indicates a problem opening the session; pretty rare here.
	 */
	Session openSession() throws HibernateException;

	/**
	 * Obtains the current session.  The definition of what exactly "current"
	 * means controlled by the {@link org.hibernate.context.spi.CurrentSessionContext} impl configured
	 * for use.
	 * <p/>
	 * Note that for backwards compatibility, if a {@link org.hibernate.context.spi.CurrentSessionContext}
	 * is not configured but JTA is configured this will default to the {@link org.hibernate.context.internal.JTASessionContext}
	 * impl.
	 *
	 * @return The current session.
	 *
	 * @throws HibernateException Indicates an issue locating a suitable current session.
	 */
	Session getCurrentSession() throws HibernateException;

	/**
	 * Obtain a {@link StatelessSession} builder.
	 *
	 * @return The stateless session builder
	 */
	StatelessSessionBuilder withStatelessOptions();

	/**
	 * Open a new stateless session.
	 *
	 * @return The created stateless session.
	 */
	StatelessSession openStatelessSession();

	/**
	 * Open a new stateless session, utilizing the specified JDBC
	 * {@link Connection}.
	 *
	 * @param connection Connection provided by the application.
	 *
	 * @return The created stateless session.
	 */
	StatelessSession openStatelessSession(Connection connection);

	/**
	 * Retrieve the statistics fopr this factory.
	 *
	 * @return The statistics.
	 */
	Statistics getStatistics();

	/**
	 * Destroy this <tt>SessionFactory</tt> and release all resources (caches,
	 * connection pools, etc).
	 * <p/>
	 * It is the responsibility of the application to ensure that there are no
	 * open {@link Session sessions} before calling this method as the impact
	 * on those {@link Session sessions} is indeterminate.
	 * <p/>
	 * No-ops if already {@link #isClosed closed}.
	 *
	 * @throws HibernateException Indicates an issue closing the factory.
	 */
	void close() throws HibernateException;

	/**
	 * Is this factory already closed?
	 *
	 * @return True if this factory is already closed; false otherwise.
	 */
	boolean isClosed();

	/**
	 * Obtain direct access to the underlying cache regions.
	 *
	 * @return The direct cache access API.
	 */
	@Override
	Cache getCache();

	/**
	 * Obtain a set of the names of all filters defined on this SessionFactory.
	 *
	 * @return The set of filter names.
	 */
	Set getDefinedFilterNames();

	/**
	 * Obtain the definition of a filter by name.
	 *
	 * @param filterName The name of the filter for which to obtain the definition.
	 * @return The filter definition.
	 * @throws HibernateException If no filter defined with the given name.
	 */
	FilterDefinition getFilterDefinition(String filterName) throws HibernateException;

	/**
	 * Determine if this session factory contains a fetch profile definition
	 * registered under the given name.
	 *
	 * @param name The name to check
	 * @return True if there is such a fetch profile; false otherwise.
	 */
	boolean containsFetchProfileDefinition(String name);

	/**
	 * Find all  {@code EntityGraph}s associated with a given entity type.
	 *
	 * @param entityClass the entity type for which to find all {@code EntityGraph}s.
	 *
	 * @return A list of {@code EntityGraph} instances associated with the given entity type. The empty list is
	 * returned in case there are not entity graphs.
	 */
	<T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass);
}
