/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.ejb.criteria;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.persistence.criteria.From;
import javax.persistence.metamodel.EntityType;
import org.hibernate.ejb.criteria.FromImpl.JoinScope;
import org.hibernate.ejb.criteria.expression.ExpressionImpl;

/**
 * The Hibernate implementation of the JPA {@link Subquery} contract.  Mostlty a set of delegation to its internal
 * {@link QueryStructure}.
 *
 * @author Steve Ebersole
 */
public class CriteriaSubqueryImpl<T> extends ExpressionImpl<T> implements Subquery<T> {
	private final AbstractQuery<?> parent;
	private final QueryStructure<T> queryStructure;

	private Expression<T> selection;
	private Set<Join<?, ?>> correlatedJoins = new HashSet<Join<?,?>>();

	private final FromImpl.JoinScope joinScope = new FromImpl.JoinScope() {
		public void addJoin(Join join) {
			correlatedJoins.add( join );
		}

		public void addFetch(Fetch fetch) {
			throw new UnsupportedOperationException( "Cannot define fetch from a subquery correlation" );
		}

		public boolean isCorrelated() {
			return true;
		}

		public From getCorrelationParent() {
			return null;
		}
	};

	public CriteriaSubqueryImpl(
			QueryBuilderImpl queryBuilder,
			Class<T> javaType,
			AbstractQuery<?> parent) {
		super(queryBuilder, javaType);
		this.parent = parent;
		this.queryStructure = new QueryStructure<T>( this, queryBuilder );
	}

	/**
	 * Get the scope used to scope joins to this subquery.
	 *
	 * @return The subquery's join scope.
	 */
	public JoinScope getJoinScope() {
		return joinScope;
	}

	/**
	 * {@inheritDoc}
	 */
	public AbstractQuery<?> getParent() {
		return parent;
	}

	/**
	 * {@inheritDoc}
	 */
	public void registerParameters(ParameterRegistry registry) {
		for ( ParameterExpression param : queryStructure.getParameters() ) {
			registry.registerParameter( param );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<T> getResultType() {
		return getJavaType();
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public Set<Root<?>> getRoots() {
		return queryStructure.getRoots();
	}

	/**
	 * {@inheritDoc}
	 */
	public <X> Root<X> from(EntityType<X> entityType) {
		return queryStructure.from( entityType );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X> Root<X> from(Class<X> entityClass) {
		return queryStructure.from( entityClass );
	}


	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Subquery<T> distinct(boolean applyDistinction) {
		queryStructure.setDistinction( applyDistinction );
		return this;
	}

	public boolean isDistinct() {
		return queryStructure.isDistinction();
	}

	public Expression<T> getSelection() {
		return selection;
	}

	public Subquery<T> select(Expression<T> expression) {
		queryStructure.setSelection( selection );
		this.selection = expression;
		return this;
	}


	// RESTRICTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public Predicate getRestriction() {
		return queryStructure.getRestriction();
	}

	/**
	 * {@inheritDoc}
	 */
	public Subquery<T> where(Expression<Boolean> expression) {
		queryStructure.setRestriction( queryBuilder().wrap( expression ) );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Subquery<T> where(Predicate... predicates) {
		// TODO : assuming this should be a conjuntion, but the spec does not say specifically...
		queryStructure.setRestriction( queryBuilder().and( predicates ) );
		return this;
	}



	// GROUPING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public List<Expression<?>> getGroupList() {
		return queryStructure.getGroupings();
	}

	/**
	 * {@inheritDoc}
	 */
	public Subquery<T> groupBy(Expression<?>... groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Subquery<T> groupBy(List<Expression<?>> groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate getGroupRestriction() {
		return queryStructure.getHaving();
	}

	/**
	 * {@inheritDoc}
	 */
	public Subquery<T> having(Expression<Boolean> expression) {
		queryStructure.setHaving( queryBuilder().wrap( expression ) );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Subquery<T> having(Predicate... predicates) {
		queryStructure.setHaving( queryBuilder().and( predicates ) );
		return this;
	}


	// CORRELATIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
    public Set<Join<?, ?>> getCorrelatedJoins() {
		return correlatedJoins;
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> Root<Y> correlate(Root<Y> source) {
		return ( ( RootImpl<Y> ) source ).correlateTo( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> Join<X, Y> correlate(Join<X, Y> source) {
		return ( ( JoinImplementors.JoinImplementor<X,Y> ) source ).correlateTo( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> CollectionJoin<X, Y> correlate(CollectionJoin<X, Y> source) {
		return ( ( JoinImplementors.CollectionJoinImplementor<X,Y> ) source ).correlateTo( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> SetJoin<X, Y> correlate(SetJoin<X, Y> source) {
		return ( ( JoinImplementors.SetJoinImplementor<X,Y> ) source ).correlateTo( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> ListJoin<X, Y> correlate(ListJoin<X, Y> source) {
		return ( ( JoinImplementors.ListJoinImplementor<X,Y> ) source ).correlateTo( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, K, V> MapJoin<X, K, V> correlate(MapJoin<X, K, V> source) {
		return ( ( JoinImplementors.MapJoinImplementor<X, K, V> ) source ).correlateTo( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		return queryStructure.subquery( subqueryType );
	}

}
