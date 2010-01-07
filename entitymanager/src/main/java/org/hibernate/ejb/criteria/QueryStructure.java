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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.EntityType;

import org.hibernate.ejb.criteria.path.RootImpl;

/**
 * Models basic query structure.  Used as a delegate in implementing both
 * {@link org.hibernate.criterion.CriteriaQuery} and
 * {@link javax.persistence.criteria.Subquery}.
 * <p/>
 * Note the <tt>ORDER BY</tt> specs are neglected here.  That's because it is not valid
 * for a subquery to define an <tt>ORDER BY</tt> clause.  So we just handle them on the
 * root query directly...
 *
 * @author Steve Ebersole
 */
public class QueryStructure<T> implements Serializable {
	private final AbstractQuery<T> owner;
	private final CriteriaBuilderImpl criteriaBuilder;

	public QueryStructure(AbstractQuery<T> owner, CriteriaBuilderImpl criteriaBuilder) {
		this.owner = owner;
		this.criteriaBuilder = criteriaBuilder;
	}

	private boolean distinct;
	private Selection<? extends T> selection;
	private Set<Root<?>> roots = new HashSet<Root<?>>();
	private Predicate restriction;
	private List<Expression<?>> groupings = Collections.emptyList();
	private Predicate having;
	private List<Subquery<?>> subqueries;


	// PARAMETERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<ParameterExpression<?>> getParameters() {
		final Set<ParameterExpression<?>> parameters = new LinkedHashSet<ParameterExpression<?>>();
		final ParameterRegistry registry = new ParameterRegistry() {
			public void registerParameter(ParameterExpression<?> parameter) {
				parameters.add( parameter );
			}
		};

		ParameterContainer.Helper.possibleParameter(selection, registry);
		ParameterContainer.Helper.possibleParameter(restriction, registry);
		ParameterContainer.Helper.possibleParameter(having, registry);
		if ( subqueries != null ) {
			for ( Subquery subquery : subqueries ) {
				ParameterContainer.Helper.possibleParameter(subquery, registry);
			}
		}

		// both group-by and having expressions can (though unlikely) contain parameters...
		ParameterContainer.Helper.possibleParameter(having, registry);
		if ( groupings != null ) {
			for ( Expression<?> grouping : groupings ) {
				ParameterContainer.Helper.possibleParameter(grouping, registry);
			}
		}

		return parameters;
	}


	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public Selection<? extends T> getSelection() {
		return selection;
	}

	public void setSelection(Selection<? extends T> selection) {
		this.selection = selection;
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<Root<?>> getRoots() {
		return roots;
	}

	public <X> Root<X> from(Class<X> entityClass) {
		EntityType<X> entityType = criteriaBuilder.getEntityManagerFactory()
				.getMetamodel()
				.entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( entityClass + " is not an entity" );
		}
		return from( entityType );
	}

	public <X> Root<X> from(EntityType<X> entityType) {
		RootImpl<X> root = new RootImpl<X>( criteriaBuilder, entityType );
		roots.add( root );
		return root;
	}


	// RESTRICTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Predicate getRestriction() {
		return restriction;
	}

	public void setRestriction(Predicate restriction) {
		this.restriction = restriction;
	}


	// GROUPINGS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Expression<?>> getGroupings() {
		return groupings;
	}

	public void setGroupings(List<Expression<?>> groupings) {
		this.groupings = groupings;
	}

	public void setGroupings(Expression<?>... groupings) {
		if ( groupings != null && groupings.length > 0 ) {
			this.groupings = Arrays.asList( groupings );
		}
		else {
			this.groupings = Collections.emptyList();
		}
	}

	public Predicate getHaving() {
		return having;
	}

	public void setHaving(Predicate having) {
		this.having = having;
	}


	// SUBQUERIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Subquery<?>> getSubqueries() {
		return subqueries;
	}

	public List<Subquery<?>> internalGetSubqueries() {
		if ( subqueries == null ) {
			subqueries = new ArrayList<Subquery<?>>();
		}
		return subqueries;
	}

	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		CriteriaSubqueryImpl<U> subquery = new CriteriaSubqueryImpl<U>( criteriaBuilder, subqueryType, owner );
		internalGetSubqueries().add( subquery );
		return subquery;
	}

	@SuppressWarnings({ "unchecked" })
	public void render(StringBuilder jpaqlQuery, CriteriaQueryCompiler.RenderingContext renderingContext) {
		jpaqlQuery.append( "select " );
		if ( isDistinct() ) {
			jpaqlQuery.append( "distinct " );
		}
		if ( getSelection() == null ) {
			// we should have only a single root (query validation should have checked this...)
			final Root root = getRoots().iterator().next();
			jpaqlQuery.append( ( (Renderable) root ).renderProjection( renderingContext) );
		}
		else {
			jpaqlQuery.append( ( (Renderable) getSelection() ).renderProjection( renderingContext ) );
		}

		jpaqlQuery.append( " from " );
		String sep = "";
		for ( Root root : getRoots() ) {
			( (FromImplementor) root ).prepareAlias( renderingContext );
			jpaqlQuery.append( sep );
			jpaqlQuery.append( ( (FromImplementor) root ).renderTableExpression( renderingContext ) );
			sep = ", ";
		}

		for ( Root root : getRoots() ) {
			renderJoins( jpaqlQuery, renderingContext, root.getJoins() );
			renderFetches( jpaqlQuery, renderingContext, root.getFetches() );
		}

		if ( getRestriction() != null) {
			jpaqlQuery.append( " where " )
					.append( ( (Renderable) getRestriction() ).render( renderingContext ) );
		}

		if ( ! getGroupings().isEmpty() ) {
			jpaqlQuery.append( " group by " );
			sep = "";
			for ( Expression grouping : getGroupings() ) {
				jpaqlQuery.append( sep )
						.append( ( (Renderable) grouping ).render( renderingContext ) );
				sep = ", ";
			}

			if ( getHaving() != null ) {
				jpaqlQuery.append( " having " )
						.append( ( (Renderable) getHaving() ).render( renderingContext ) );
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	private void renderJoins(
			StringBuilder jpaqlQuery,
			CriteriaQueryCompiler.RenderingContext renderingContext,
			Collection<Join<?,?>> joins) {
		if ( joins == null ) {
			return;
		}

		for ( Join join : joins ) {
			( (FromImplementor) join ).prepareAlias( renderingContext );
			jpaqlQuery.append( renderJoinType( join.getJoinType() ) )
					.append( ( (FromImplementor) join ).renderTableExpression( renderingContext ) );
			renderJoins( jpaqlQuery, renderingContext, join.getJoins() );
			renderFetches( jpaqlQuery, renderingContext, join.getFetches() );
		}
	}

	private String renderJoinType(JoinType joinType) {
		switch ( joinType ) {
			case INNER: {
				return " inner join ";
			}
			case LEFT: {
				return " left join ";
			}
			case RIGHT: {
				return " right join ";
			}
		}
		throw new IllegalStateException( "Unknown join type " + joinType );
	}

	@SuppressWarnings({ "unchecked" })
	private void renderFetches(
			StringBuilder jpaqlQuery,
			CriteriaQueryCompiler.RenderingContext renderingContext,
			Collection<Fetch> fetches) {
		if ( fetches == null ) {
			return;
		}

		for ( Fetch fetch : fetches ) {
			( (FromImplementor) fetch ).prepareAlias( renderingContext );
			jpaqlQuery.append( renderJoinType( fetch.getJoinType() ) )
					.append( "fetch " )
					.append( ( (FromImplementor) fetch ).renderTableExpression( renderingContext ) );

			renderFetches( jpaqlQuery, renderingContext, fetch.getFetches() );
		}
	}
}
