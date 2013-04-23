/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.criterion;

import java.io.Serializable;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.ResultTransformer;

/**
 * Models a detached form of a Criteria (not associated with a Session).
 *
 * Some applications need to create criteria queries in "detached mode", where the Hibernate Session is
 * not available.  Applications would create a DetachableCriteria to describe the query, and then later
 * associated it with a Session to obtain the "executable" Criteria:
 * <code>
 *     DetachedCriteria detached = new DetachedCriteria();
 *     ...
 *     Criteria criteria = detached.getExecutableCriteria( session );
 *     ...
 *     criteria.list();
 * </code>
 *
 * All methods have the same semantics and behavior as the corresponding methods of the Criteria interface.
 *
 * @author Gavin King
 *
 * @see org.hibernate.Criteria
 */
public class DetachedCriteria implements CriteriaSpecification, Serializable {
	private final CriteriaImpl impl;
	private final Criteria criteria;

	protected DetachedCriteria(String entityName) {
		impl = new CriteriaImpl( entityName, null );
		criteria = impl;
	}

	protected DetachedCriteria(String entityName, String alias) {
		impl = new CriteriaImpl( entityName, alias, null );
		criteria = impl;
	}

	protected DetachedCriteria(CriteriaImpl impl, Criteria criteria) {
		this.impl = impl;
		this.criteria = criteria;
	}

	/**
	 * Get an executable instance of Criteria to actually run the query.
	 *
	 * @param session The session to associate the built Criteria with
	 *
	 * @return The "executable" Criteria
	 */
	public Criteria getExecutableCriteria(Session session) {
		impl.setSession( (SessionImplementor) session );
		return impl;
	}

	/**
	 * Obtain the alias associated with this DetachedCriteria
	 *
	 * @return The alias
	 */
	public String getAlias() {
		return criteria.getAlias();
	}

	/**
	 * Retrieve the CriteriaImpl used internally to hold the DetachedCriteria state
	 *
	 * @return The internally maintained CriteriaImpl
	 */
	CriteriaImpl getCriteriaImpl() {
		return impl;
	}

	/**
	 * Static builder to create a DetachedCriteria for the given entity.
	 *
	 * @param entityName The name of the entity to create a DetachedCriteria for
	 *
	 * @return The DetachedCriteria
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static DetachedCriteria forEntityName(String entityName) {
		return new DetachedCriteria( entityName );
	}

	/**
	 * Static builder to create a DetachedCriteria for the given entity.
	 *
	 * @param entityName The name of the entity to create a DetachedCriteria for
	 * @param alias The alias to apply to the entity
	 *
	 * @return The DetachedCriteria
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static DetachedCriteria forEntityName(String entityName, String alias) {
		return new DetachedCriteria( entityName, alias );
	}

	/**
	 * Static builder to create a DetachedCriteria for the given entity, by its Class.
	 *
	 * @param clazz The entity class
	 *
	 * @return The DetachedCriteria
	 */
	public static DetachedCriteria forClass(Class clazz) {
		return new DetachedCriteria( clazz.getName() );
	}

	/**
	 * Static builder to create a DetachedCriteria for the given entity, by its Class.
	 *
	 * @param clazz The entity class
	 * @param alias The alias to apply to the entity
	 *
	 * @return The DetachedCriteria
	 */
	public static DetachedCriteria forClass(Class clazz, String alias) {
		return new DetachedCriteria( clazz.getName() , alias );
	}

	/**
	 * Add a restriction
	 *
	 * @param criterion The restriction
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria add(Criterion criterion) {
		criteria.add( criterion );
		return this;
	}

	/**
	 * Adds an ordering
	 *
	 * @param order The ordering
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria addOrder(Order order) {
		criteria.addOrder( order );
		return this;
	}

	/**
	 * Set the fetch mode for a given association
	 *
	 * @param associationPath The association path
	 * @param mode The fetch mode to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria setFetchMode(String associationPath, FetchMode mode) {
		criteria.setFetchMode( associationPath, mode );
		return this;
	}

	/**
	 * Set the projection to use.
	 *
	 * @param projection The projection to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria setProjection(Projection projection) {
		criteria.setProjection( projection );
		return this;
	}

	/**
	 * Set the result transformer to use.
	 *
	 * @param resultTransformer The result transformer to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria setResultTransformer(ResultTransformer resultTransformer) {
		criteria.setResultTransformer( resultTransformer );
		return this;
	}

	/**
	 * Creates an association path alias within this DetachedCriteria.  The alias can then be used in further
	 * alias creations or restrictions, etc.
	 *
	 * @param associationPath The association path
	 * @param alias The alias to apply to that association path
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria createAlias(String associationPath, String alias) {
		criteria.createAlias( associationPath, alias );
		return this;
	}

	/**
	 * Creates an association path alias within this DetachedCriteria specifying the type of join.  The alias
	 * can then be used in further alias creations or restrictions, etc.
	 *
	 * @param associationPath The association path
	 * @param alias The alias to apply to that association path
	 * @param joinType The type of join to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria createAlias(String associationPath, String alias, JoinType joinType) {
		criteria.createAlias( associationPath, alias, joinType );
		return this;
	}

	/**
	 * Creates an association path alias within this DetachedCriteria specifying the type of join.  The alias
	 * can then be used in further alias creations or restrictions, etc.
	 *
	 * @param associationPath The association path
	 * @param alias The alias to apply to that association path
	 * @param joinType The type of join to use
	 * @param withClause An additional restriction on the join
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria createAlias(String associationPath, String alias, JoinType joinType, Criterion withClause) {
		criteria.createAlias( associationPath, alias, joinType, withClause );
		return this;
	}

	/**
	 * Deprecated!
	 *
	 * @param associationPath The association path
	 * @param alias The alias to apply to that association path
	 * @param joinType The type of join to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated use {@link #createAlias(String, String, JoinType)}
	 */
	@Deprecated
	public DetachedCriteria createAlias(String associationPath, String alias, int joinType) {
		return createAlias( associationPath, alias, JoinType.parse( joinType ) );
	}

	/**
	 * Deprecated!
	 *
	 * @param associationPath The association path
	 * @param alias The alias to apply to that association path
	 * @param joinType The type of join to use
	 * @param withClause An additional restriction on the join
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated use {@link #createAlias(String, String, JoinType, Criterion)}
	 */
	@Deprecated
	public DetachedCriteria createAlias(String associationPath, String alias, int joinType, Criterion withClause) {
		return createAlias( associationPath, alias, JoinType.parse( joinType ), withClause );
	}

	/**
	 * Creates an nested DetachedCriteria representing the association path.
	 *
	 * @param associationPath The association path
	 * @param alias The alias to apply to that association path
	 *
	 * @return the newly created, nested DetachedCriteria
	 */
	public DetachedCriteria createCriteria(String associationPath, String alias) {
		return new DetachedCriteria( impl, criteria.createCriteria( associationPath, alias ) );
	}

	/**
	 * Creates an nested DetachedCriteria representing the association path.
	 *
	 * @param associationPath The association path
	 *
	 * @return the newly created, nested DetachedCriteria
	 */
	public DetachedCriteria createCriteria(String associationPath) {
		return new DetachedCriteria( impl, criteria.createCriteria( associationPath ) );
	}

	/**
	 * Creates an nested DetachedCriteria representing the association path, specifying the type of join to use.
	 *
	 * @param associationPath The association path
	 * @param joinType The type of join to use
	 *
	 * @return the newly created, nested DetachedCriteria
	 */
	public DetachedCriteria createCriteria(String associationPath, JoinType joinType) {
		return new DetachedCriteria( impl, criteria.createCriteria( associationPath, joinType ) );
	}

	/**
	 * Creates an nested DetachedCriteria representing the association path, specifying the type of join to use.
	 *
	 * @param associationPath The association path
	 * @param alias The alias to associate with this "join".
	 * @param joinType The type of join to use
	 *
	 * @return the newly created, nested DetachedCriteria
	 */
	public DetachedCriteria createCriteria(String associationPath, String alias, JoinType joinType) {
		return new DetachedCriteria( impl, criteria.createCriteria( associationPath, alias, joinType ) );
	}

	/**
	 * Creates an nested DetachedCriteria representing the association path, specifying the type of join to use and
	 * an additional join restriction.
	 *
	 * @param associationPath The association path
	 * @param alias The alias to associate with this "join".
	 * @param joinType The type of join to use
	 * @param withClause The additional join restriction
	 *
	 * @return the newly created, nested DetachedCriteria
	 */
	public DetachedCriteria createCriteria(String associationPath, String alias, JoinType joinType, Criterion withClause)  {
		return new DetachedCriteria(impl, criteria.createCriteria( associationPath, alias, joinType, withClause ) );
	}

	/**
	 * Deprecated!
	 *
	 * @param associationPath The association path
	 * @param joinType The type of join to use
	 *
	 * @return the newly created, nested DetachedCriteria
	 *
	 * @deprecated use {@link #createCriteria(String, JoinType)}
	 */
	@Deprecated
	public DetachedCriteria createCriteria(String associationPath, int joinType) {
		return createCriteria( associationPath, JoinType.parse( joinType ) );
	}

	/**
	 * Deprecated!
	 *
	 * @param associationPath The association path
	 * @param alias The alias
	 * @param joinType The type of join to use
	 *
	 * @return the newly created, nested DetachedCriteria
	 *
	 * @deprecated use {@link #createCriteria(String, String, JoinType)}
	 */
	@Deprecated
	public DetachedCriteria createCriteria(String associationPath, String alias, int joinType) {
		return createCriteria( associationPath, alias, JoinType.parse( joinType ) );
	}

	/**
	 * Deprecated!
	 *
	 * @param associationPath The association path
	 * @param alias The alias to associate with this "join".
	 * @param joinType The type of join to use
	 * @param withClause The additional join restriction
	 *
	 * @return the newly created, nested DetachedCriteria
	 *
	 * @deprecated use {@link #createCriteria(String, String, JoinType, Criterion)}
	 */
	@Deprecated
	public DetachedCriteria createCriteria(String associationPath, String alias, int joinType, Criterion withClause) {
		return createCriteria( associationPath, alias, JoinType.parse( joinType ), withClause );
	}

	/**
	 * Set the SQL comment to use.
	 *
	 * @param comment The SQL comment to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria setComment(String comment) {
		criteria.setComment( comment );
		return this;
	}

	/**
	 * Set the lock mode to use.
	 *
	 * @param lockMode The lock mode to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria setLockMode(LockMode lockMode) {
		criteria.setLockMode( lockMode );
		return this;
	}

	/**
	 * Set an alias-specific lock mode.  The specified lock mode applies only to that alias.
	 *
	 * @param alias The alias to apply the lock to
	 * @param lockMode The lock mode to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public DetachedCriteria setLockMode(String alias, LockMode lockMode) {
		criteria.setLockMode( alias, lockMode );
		return this;
	}

	@Override
	public String toString() {
		return "DetachableCriteria(" + criteria.toString() + ')';
	}

}
