/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

import java.util.Collection;

import org.hibernate.MappingException;

/**
 * Represents the abstract notion of a query whose results are affected by the data stored
 * in a given set of named <em>query spaces</em>. A query space is usually, but not always,
 * a relational database table, in which case the name of the space is simply the table name.
 * <p>
 * Each {@linkplain jakarta.persistence.Entity entity type} is understood to store its state
 * in one or more query spaces. Usually, the query spaces are automatically determined by
 * the mapping, but sometimes they must be specified explicitly using
 * {@link org.hibernate.annotations.Synchronize @Synchronize}.
 * <p>
 * Query spaces mediate the interaction between query execution and synchronization of
 * in-memory state with the database:
 * <ul>
 * <li>
 * When {@linkplain org.hibernate.FlushMode#AUTO auto-flush} is enabled, in-memory changes
 * to every dirty entity whose state belongs to any query space which affects a given query
 * must be flushed before the query is executed.
 * <li>
 * Conversely, when changes to an entity whose state is stored in a given query space are
 * flushed to the database, every {@linkplain SelectionQuery#setCacheable cached query
 * result set} for a query affected by that query space must be immediately invalidated.
 * </ul>
 * <p>
 * Typically, query spaces are independent (non-overlapping), and Hibernate always treats
 * them as such. Overlapping or hierarchical query spaces are in principle meaningful, but
 * any such relationship between query spaces is the responsibility of the client.
 * <p>
 * A query space name is not always a table name. In principle, it's permitted to be any
 * arbitrary string which uniquely identifies an abstract location where state is stored
 * persistently. It's even possible that the data in a single table is segmented in such
 * a way that the table is effectively the union of multiple independent query spaces.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.annotations.Synchronize
 * @see org.hibernate.jpa.HibernateHints#HINT_NATIVE_SPACES
 */
public interface SynchronizeableQuery {
	/**
	 * Obtain the list of query spaces the query is synchronized on.
	 *
	 * @return The list of query spaces upon which the query is synchronized.
	 */
	Collection<String> getSynchronizedQuerySpaces();

	/**
	 * Adds a query space.
	 *
	 * @param querySpace The query space to be auto-flushed for this query.
	 *
	 * @return {@code this}, for method chaining
	 */
	SynchronizeableQuery addSynchronizedQuerySpace(String querySpace);

	/**
	 * Adds an entity name for (a) auto-flush checking and (b) query result cache invalidation checking.  Same as
	 * {@link #addSynchronizedQuerySpace} for all tables associated with the given entity.
	 *
	 * @param entityName The name of the entity upon whose defined query spaces we should additionally synchronize.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @throws MappingException Indicates the given name could not be resolved as an entity
	 */
	SynchronizeableQuery addSynchronizedEntityName(String entityName) throws MappingException;

	/**
	 * Adds an entity for (a) auto-flush checking and (b) query result cache invalidation checking.  Same as
	 * {@link #addSynchronizedQuerySpace} for all tables associated with the given entity.
	 *
	 * @param entityClass The class of the entity upon whose defined query spaces we should additionally synchronize.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @throws MappingException Indicates the given class could not be resolved as an entity
	 */
	SynchronizeableQuery addSynchronizedEntityClass(Class<?> entityClass) throws MappingException;
}
