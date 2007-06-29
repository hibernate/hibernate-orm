//$Id: Criteria.java 9116 2006-01-23 21:21:01Z steveebersole $
package org.hibernate;

import java.util.List;

import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.transform.ResultTransformer;

/**
 * <tt>Criteria</tt> is a simplified API for retrieving entities
 * by composing <tt>Criterion</tt> objects. This is a very
 * convenient approach for functionality like "search" screens
 * where there is a variable number of conditions to be placed
 * upon the result set.<br>
 * <br>
 * The <tt>Session</tt> is a factory for <tt>Criteria</tt>.
 * <tt>Criterion</tt> instances are usually obtained via
 * the factory methods on <tt>Restrictions</tt>. eg.
 * <pre>
 * List cats = session.createCriteria(Cat.class)
 *     .add( Restrictions.like("name", "Iz%") )
 *     .add( Restrictions.gt( "weight", new Float(minWeight) ) )
 *     .addOrder( Order.asc("age") )
 *     .list();
 * </pre>
 * You may navigate associations using <tt>createAlias()</tt> or
 * <tt>createCriteria()</tt>.
 * <pre>
 * List cats = session.createCriteria(Cat.class)
 *     .createCriteria("kittens")
 *         .add( Restrictions.like("name", "Iz%") )
 *     .list();
 * </pre>
 * <pre>
 * List cats = session.createCriteria(Cat.class)
 *     .createAlias("kittens", "kit")
 *     .add( Restrictions.like("kit.name", "Iz%") )
 *     .list();
 * </pre>
 * You may specify projection and aggregation using <tt>Projection</tt>
 * instances obtained via the factory methods on <tt>Projections</tt>.
 * <pre>
 * List cats = session.createCriteria(Cat.class)
 *     .setProjection( Projections.projectionList()
 *         .add( Projections.rowCount() )
 *         .add( Projections.avg("weight") )
 *         .add( Projections.max("weight") )
 *         .add( Projections.min("weight") )
 *         .add( Projections.groupProperty("color") )
 *     )
 *     .addOrder( Order.asc("color") )
 *     .list();
 * </pre>
 *
 * @see Session#createCriteria(java.lang.Class)
 * @see org.hibernate.criterion.Restrictions
 * @see org.hibernate.criterion.Projections
 * @see org.hibernate.criterion.Order
 * @see org.hibernate.criterion.Criterion
 * @see org.hibernate.criterion.Projection
 * @see org.hibernate.criterion.DetachedCriteria a disconnected version of this API
 * @author Gavin King
 */
public interface Criteria extends CriteriaSpecification {

	/**
	 * Get the alias of the entity encapsulated by this criteria instance.
	 *
	 * @return The alias for the encapsulated entity.
	 */
	public String getAlias();

	/**
	 * Used to specify that the query results will be a projection (scalar in
	 * nature).  Implicitly specifies the {@link #PROJECTION} result transformer.
	 * <p/>
	 * The individual components contained within the given
	 * {@link Projection projection} determines the overall "shape" of the
	 * query result.
	 *
	 * @param projection The projection representing the overall "shape" of the
	 * query results.
	 * @return this (for method chaining)
	 */
	public Criteria setProjection(Projection projection);

	/**
	 * Add a {@link Criterion restriction} to constrain the results to be
	 * retrieved.
	 *
	 * @param criterion The {@link Criterion criterion} object representing the
	 * restriction to be applied.
	 * @return this (for method chaining)
	 */
	public Criteria add(Criterion criterion);
	
	/**
	 * Add an {@link Order ordering} to the result set.
	 *
	 * @param order The {@link Order order} object representing an ordering
	 * to be applied to the results.
	 * @return this (for method chaining)
	 */
	public Criteria addOrder(Order order);

	/**
	 * Specify an association fetching strategy for an association or a
	 * collection of values.
	 *
	 * @param associationPath a dot seperated property path
	 * @param mode The fetch mode for the referenced association
	 * @return this (for method chaining)
	 */
	public Criteria setFetchMode(String associationPath, FetchMode mode) throws HibernateException;

	/**
	 * Set the lock mode of the current entity
	 *
	 * @param lockMode The lock mode to be applied
	 * @return this (for method chaining)
	 */
	public Criteria setLockMode(LockMode lockMode);

	/**
	 * Set the lock mode of the aliased entity
	 *
	 * @param alias The previously assigned alias representing the entity to
	 * which the given lock mode should apply.
	 * @param lockMode The lock mode to be applied
	 * @return this (for method chaining)
	 */
	public Criteria setLockMode(String alias, LockMode lockMode);

	/**
	 * Join an association, assigning an alias to the joined association.
	 * <p/>
	 * Functionally equivalent to {@link #createAlias(String, String, int)} using
	 * {@link #INNER_JOIN} for the joinType.
	 *
	 * @param associationPath A dot-seperated property path
	 * @param alias The alias to assign to the joined association (for later reference).
	 * @return this (for method chaining)
	 */
	public Criteria createAlias(String associationPath, String alias) throws HibernateException;

	/**
	 * Join an association using the specified join-type, assigning an alias
	 * to the joined association.
	 * <p/>
	 * The joinType is expected to be one of {@link #INNER_JOIN} (the default),
	 * {@link #FULL_JOIN}, or {@link #LEFT_JOIN}.
	 *
	 * @param associationPath A dot-seperated property path
	 * @param alias The alias to assign to the joined association (for later reference).
	 * @param joinType The type of join to use.
	 * @return this (for method chaining)
	 */
	public Criteria createAlias(String associationPath, String alias, int joinType) throws HibernateException;

	/**
	 * Create a new <tt>Criteria</tt>, "rooted" at the associated entity.
	 * <p/>
	 * Functionally equivalent to {@link #createCriteria(String, int)} using
	 * {@link #INNER_JOIN} for the joinType.
	 *
	 * @param associationPath A dot-seperated property path
	 * @return the created "sub criteria"
	 */
	public Criteria createCriteria(String associationPath) throws HibernateException;

	/**
	 * Create a new <tt>Criteria</tt>, "rooted" at the associated entity, using the
	 * specified join type.
	 *
	 * @param associationPath A dot-seperated property path
	 * @param joinType The type of join to use.
	 * @return the created "sub criteria"
	 */
	public Criteria createCriteria(String associationPath, int joinType) throws HibernateException;

	/**
	 * Create a new <tt>Criteria</tt>, "rooted" at the associated entity,
	 * assigning the given alias.
	 * <p/>
	 * Functionally equivalent to {@link #createCriteria(String, String, int)} using
	 * {@link #INNER_JOIN} for the joinType.
	 *
	 * @param associationPath A dot-seperated property path
	 * @param alias The alias to assign to the joined association (for later reference).
	 * @return the created "sub criteria"
	 */
	public Criteria createCriteria(String associationPath, String alias) throws HibernateException;

	/**
	 * Create a new <tt>Criteria</tt>, "rooted" at the associated entity,
	 * assigning the given alias and using the specified join type.
	 *
	 * @param associationPath A dot-seperated property path
	 * @param alias The alias to assign to the joined association (for later reference).
	 * @param joinType The type of join to use.
	 * @return the created "sub criteria"
	 */
	public Criteria createCriteria(String associationPath, String alias, int joinType) throws HibernateException;

	/**
	 * Set a strategy for handling the query results. This determines the
	 * "shape" of the query result.
	 *
	 * @param resultTransformer The transformer to apply
	 * @return this (for method chaining)
	 *
	 * @see #ROOT_ENTITY
	 * @see #DISTINCT_ROOT_ENTITY
	 * @see #ALIAS_TO_ENTITY_MAP
	 * @see #PROJECTION
	 */
	public Criteria setResultTransformer(ResultTransformer resultTransformer);

	/**
	 * Set a limit upon the number of objects to be retrieved.
	 *
	 * @param maxResults the maximum number of results
	 * @return this (for method chaining)
	 */
	public Criteria setMaxResults(int maxResults);
	
	/**
	 * Set the first result to be retrieved.
	 *
	 * @param firstResult the first result to retrieve, numbered from <tt>0</tt>
	 * @return this (for method chaining)
	 */
	public Criteria setFirstResult(int firstResult);
	
	/**
	 * Set a fetch size for the underlying JDBC query.
	 *
	 * @param fetchSize the fetch size
	 * @return this (for method chaining)
	 *
	 * @see java.sql.Statement#setFetchSize
	 */
	public Criteria setFetchSize(int fetchSize);

	/**
	 * Set a timeout for the underlying JDBC query.
	 *
	 * @param timeout The timeout value to apply.
	 * @return this (for method chaining)
	 *
	 * @see java.sql.Statement#setQueryTimeout
	 */
	public Criteria setTimeout(int timeout);

	/**
	 * Enable caching of this query result, provided query caching is enabled
	 * for the underlying session factory.
	 *
	 * @param cacheable Should the result be considered cacheable; default is
	 * to not cache (false).
	 * @return this (for method chaining)
	 */
	public Criteria setCacheable(boolean cacheable);

	/**
	 * Set the name of the cache region to use for query result caching.
	 *
	 * @param cacheRegion the name of a query cache region, or <tt>null</tt>
	 * for the default query cache
	 * @return this (for method chaining)
	 *
	 * @see #setCacheable
	 */
	public Criteria setCacheRegion(String cacheRegion);

	/**
	 * Add a comment to the generated SQL.
	 *
	 * @param comment a human-readable string
	 * @return this (for method chaining)
	 */
	public Criteria setComment(String comment);

	/**
	 * Override the flush mode for this particular query.
	 *
	 * @param flushMode The flush mode to use.
	 * @return this (for method chaining)
	 */
	public Criteria setFlushMode(FlushMode flushMode);

	/**
	 * Override the cache mode for this particular query.
	 *
	 * @param cacheMode The cache mode to use.
	 * @return this (for method chaining)
	 */
	public Criteria setCacheMode(CacheMode cacheMode);

	/**
	 * Get the results.
	 *
	 * @return The list of matched query results.
	 */
	public List list() throws HibernateException;
	
	/**
	 * Get the results as an instance of {@link ScrollableResults}
	 *
	 * @return The {@link ScrollableResults} representing the matched
	 * query results.
	 */
	public ScrollableResults scroll() throws HibernateException;

	/**
	 * Get the results as an instance of {@link ScrollableResults} based on the
	 * given scroll mode.
	 *
	 * @param scrollMode Indicates the type of underlying database cursor to
	 * request.
	 * @return The {@link ScrollableResults} representing the matched
	 * query results.
	 */
	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException;

	/**
	 * Convenience method to return a single instance that matches
	 * the query, or null if the query returns no results.
	 *
	 * @return the single result or <tt>null</tt>
	 * @throws HibernateException if there is more than one matching result
	 */
	public Object uniqueResult() throws HibernateException;
	
}