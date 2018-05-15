/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query;

import java.util.List;

import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.criteria.JoinType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * A simplified API for retrieving audit entities by composing {@link AuditCriterion} objects.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 *
 * @see org.hibernate.Criteria
 */
public interface AuditQuery {
	/**
	 * Get the results.
	 *
	 * @return The list of matched query results.
	 * @throws AuditException Indicates a problem getting the results.
	 */
	List getResultList() throws AuditException;

	/**
	 * Convenience method to return a single instance that matches the query,
	 * or null if the query results no results.
	 *
	 * @return The single result or {@code null}.
	 * @throws AuditException If there is more than one matching result.
	 * @throws NonUniqueResultException If there is more than one
	 * @throws NoResultException If there is no results.
	 */
	Object getSingleResult() throws AuditException, NonUniqueResultException, NoResultException;

	/**
	 * Traverse audit relation by name, specifying a join type.
	 *
	 * @param associationName The association to traverse
	 * @param joinType The join type to be used when traversing
	 * @return this (for method chaining)
	 */
	AuditAssociationQuery<? extends AuditQuery> traverseRelation(String associationName, JoinType joinType);

	/**
	 * Traverse audit relation by name, specifying both a join type and alias.
	 *
	 * @param associationName The association to traverse
	 * @param joinType The join type to be used when traversing
	 * @param alias The join alias
	 * @return this (for method chaining)
	 */
	AuditAssociationQuery<? extends AuditQuery> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias);

	/**
	 * Add a {@link AuditCriterion} to constrain the results to be retrieved.
	 *
	 * @param criterion The criterion object representing the restriction to be applied.
	 * @return this (for method chaining)
	 */
	AuditQuery add(AuditCriterion criterion);

	/**
	 * Used to specify that the query results will be a propagation (scalar in nature).
	 * Implicitly specifies the {@link org.hibernate.Criteria#PROJECTION} result transformer.
	 *
	 * @param projection The projection representing the overall "shape" of the new query results.
	 * @return this (for method chaining)
	 */
	AuditQuery addProjection(AuditProjection projection);

	/**
	 * Add an {@link AuditOrder} to the result set.
	 *
	 * @param order The object representing ordering to be applied to the results.
	 * @return this (for method chaining)
	 */
	AuditQuery addOrder(AuditOrder order);

	/**
	 * Set a limit on the number of objects to be retrieved.
	 *
	 * @param maxResults the maximum number of results
	 * @return this (for method chaining)
	 */
	AuditQuery setMaxResults(int maxResults);

	/**
	 * Set the first result to be retrieved.
	 *
	 * @param firstResult the first result to retrieve, numbered from <tt>0</tt>
	 * @return this (for method chaining)
	 */
	AuditQuery setFirstResult(int firstResult);

	/**
	 * Enable caching of this query result, provided query caching is enabled
	 * for the underlying session factory.
	 *
	 * @param cacheable Should the result be considered cacheable; default is {@code false}.
	 * @return this (for method chaining)
	 */
	AuditQuery setCacheable(boolean cacheable);

	/**
	 * Set the name of the cache region to use for query result caching.
	 *
	 * @param cacheRegion the name of a query cache region or <tt>null</tt> for the default query mode.
	 * @return this (for method chaining)
	 */
	AuditQuery setCacheRegion(String cacheRegion);

	/**
	 * Override the cache mode for this particular query.
	 *
	 * @param cacheMode The cache mode to use.
	 * @return this (for method chaining)
	 */
	AuditQuery setCacheMode(CacheMode cacheMode);

	/**
	 * Add a comment to the generated SQL.
	 *
	 * @param comment a human-readable string
	 * @return this (for method chaining)
	 */
	AuditQuery setComment(String comment);

	/**
	 * Override the flush mode for this particular query.
	 *
	 * @param flushMode The flush mode to use.
	 * @return this (for method chaining)
	 */
	AuditQuery setFlushMode(FlushMode flushMode);

	/**
	 * Set the flush mode type to be used for the query execution.  The flush mode type
	 * applies to the query regardless of the flush mode type in use for the session.
	 *
	 * @param flushMode flush mode
	 * @return this (for method chaining)
	 */
	AuditQuery setFlushMode(FlushModeType flushMode);

	/**
	 * Set a timeout for the underlying JDBC query.
	 *
	 * @param timeout The timeout value to apply.
	 * @return this (for method chaining)
	 */
	AuditQuery setTimeout(int timeout);

	/**
	 * Set the lock mode of the current entity.
	 *
	 * @param lockMode The lock mode to be applied.
	 * @return this (for method chaining)
	 */
	AuditQuery setLockMode(LockMode lockMode);

	/**
	 * Get the alias of the entity encapsulated by this criteria instance.
	 *
	 * @return The alias for the encapsulating entity.
	 */
	String getAlias();
}
