/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.path.RootImpl;
import org.hibernate.query.criteria.internal.path.RootImpl.TreatedRoot;
import org.hibernate.sql.ast.Clause;

/**
 * Models basic query structure.  Used as a delegate in implementing both
 * {@link jakarta.persistence.criteria.CriteriaQuery} and
 * {@link jakarta.persistence.criteria.Subquery}.
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
	private final boolean isSubQuery;

	public QueryStructure(AbstractQuery<T> owner, CriteriaBuilderImpl criteriaBuilder) {
		this.owner = owner;
		this.criteriaBuilder = criteriaBuilder;
		this.isSubQuery = Subquery.class.isInstance( owner );
	}

	private boolean distinct;
	private Selection<? extends T> selection;
	private Set<Root<?>> roots = new LinkedHashSet<Root<?>>();
	private Set<FromImplementor> correlationRoots;
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


	// CORRELATION ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void addCorrelationRoot(FromImplementor fromImplementor) {
		if ( !isSubQuery ) {
			throw new IllegalStateException( "Query is not identified as sub-query" );
		}
		if ( correlationRoots == null ) {
			correlationRoots = new HashSet<FromImplementor>();
		}
		correlationRoots.add( fromImplementor );
	}

	public Set<Join<?, ?>> collectCorrelatedJoins() {
		if ( !isSubQuery ) {
			throw new IllegalStateException( "Query is not identified as sub-query" );
		}
		final Set<Join<?, ?>> correlatedJoins;
		if ( correlationRoots != null ) {
			correlatedJoins = new HashSet<Join<?,?>>();
			for ( FromImplementor<?,?> correlationRoot : correlationRoots ) {
				if (correlationRoot instanceof Join<?,?> && correlationRoot.isCorrelated()) {
					correlatedJoins.add( (Join<?,?>) correlationRoot );
				}
				correlatedJoins.addAll( correlationRoot.getJoins() );
			}
		}
		else {
			correlatedJoins = Collections.emptySet();
		}
		return correlatedJoins;
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


	// SUB-QUERIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
	public void render(StringBuilder jpaqlQuery, RenderingContext renderingContext) {
		renderSelectClause( jpaqlQuery, renderingContext );

		renderFromClause( jpaqlQuery, renderingContext );

		renderWhereClause( jpaqlQuery, renderingContext );

		renderGroupByClause( jpaqlQuery, renderingContext );
	}

	protected void renderSelectClause(StringBuilder jpaqlQuery, RenderingContext renderingContext) {
		renderingContext.getClauseStack().push( Clause.SELECT );

		try {
			jpaqlQuery.append( "select " );

			if ( isDistinct() ) {
				jpaqlQuery.append( "distinct " );
			}

			if ( getSelection() == null ) {
				jpaqlQuery.append( locateImplicitSelection().render( renderingContext ) );
			}
			else {
				jpaqlQuery.append( ( (Renderable) getSelection() ).render( renderingContext ) );
			}
		}
		finally {
			renderingContext.getClauseStack().pop();
		}
	}

	private FromImplementor locateImplicitSelection() {
		FromImplementor implicitSelection = null;

		if ( ! isSubQuery ) {
			// we should have only a single root (query validation should have checked this...)
			implicitSelection = (FromImplementor) getRoots().iterator().next();
		}
		else {
			// we should only have a single "root" which can act as the implicit selection
			final Set<Join<?, ?>> correlatedJoins = collectCorrelatedJoins();
			if ( correlatedJoins != null ) {
				if ( correlatedJoins.size() == 1 ) {
					implicitSelection = (FromImplementor) correlatedJoins.iterator().next();
				}
			}
		}

		if ( implicitSelection == null ) {
			throw new IllegalStateException( "No explicit selection and an implicit one could not be determined" );
		}

		return implicitSelection;
	}

	@SuppressWarnings({ "unchecked" })
	private void renderFromClause(StringBuilder jpaqlQuery, RenderingContext renderingContext) {
		renderingContext.getClauseStack().push( Clause.FROM );

		try {
			jpaqlQuery.append( " from " );
			String sep = "";
			for ( Root root : getRoots() ) {
				( (FromImplementor) root ).prepareAlias( renderingContext );
				jpaqlQuery.append( sep );
				sep = ", ";
				jpaqlQuery.append( ( (FromImplementor) root ).renderTableExpression( renderingContext ) );
			}

			for ( Root root : getRoots() ) {
				renderJoins( jpaqlQuery, renderingContext, root.getJoins() );
				if ( root instanceof RootImpl ) {
					Set<TreatedRoot> treats = ( (RootImpl) root ).getTreats();
					for ( TreatedRoot treat : treats ) {
						renderJoins( jpaqlQuery, renderingContext, treat.getJoins() );
						renderFetches( jpaqlQuery, renderingContext, treat.getFetches() );
					}
				}
				renderFetches( jpaqlQuery, renderingContext, root.getFetches() );
			}

			if ( isSubQuery ) {
				if ( correlationRoots != null ) {
					for ( FromImplementor<?, ?> correlationRoot : correlationRoots ) {
						final FromImplementor correlationParent = correlationRoot.getCorrelationParent();
						correlationParent.prepareAlias( renderingContext );
						final String correlationRootAlias = correlationParent.getAlias();
						for ( Join<?, ?> correlationJoin : correlationRoot.getJoins() ) {
							final JoinImplementor correlationJoinImpl = (JoinImplementor) correlationJoin;
							// IMPL NOTE: reuse the sep from above!
							jpaqlQuery.append( sep );
							correlationJoinImpl.prepareAlias( renderingContext );
							jpaqlQuery.append( correlationRootAlias )
									.append( '.' )
									.append( correlationJoinImpl.getAttribute().getName() )
									.append( " as " )
									.append( correlationJoinImpl.getAlias() );
							sep = ", ";
							renderJoins( jpaqlQuery, renderingContext, correlationJoinImpl.getJoins() );
						}
					}
				}
			}
		}
		finally {
			renderingContext.getClauseStack().pop();
		}
	}

	protected void renderWhereClause(StringBuilder jpaqlQuery, RenderingContext renderingContext) {
		if ( getRestriction() == null ) {
			return;
		}

		renderingContext.getClauseStack().push( Clause.WHERE );
		try {
			jpaqlQuery.append( " where " )
					.append( ( (Renderable) getRestriction() ).render( renderingContext ) );
		}
		finally {
			renderingContext.getClauseStack().pop();
		}
	}

	protected void renderGroupByClause(StringBuilder jpaqlQuery, RenderingContext renderingContext) {
		if ( getGroupings().isEmpty() ) {
			return;
		}

		renderingContext.getClauseStack().push( Clause.GROUP );
		try {
			jpaqlQuery.append( " group by " );
			String sep = "";
			for ( Expression grouping : getGroupings() ) {
				jpaqlQuery.append( sep )
						.append( ( (Renderable) grouping ).render( renderingContext ) );
				sep = ", ";
			}

			renderHavingClause( jpaqlQuery, renderingContext );
		}
		finally {
			renderingContext.getClauseStack().pop();
		}
	}

	private void renderHavingClause(StringBuilder jpaqlQuery, RenderingContext renderingContext) {
		if ( getHaving() == null ) {
			return;
		}

		renderingContext.getClauseStack().push( Clause.HAVING );
		try {
			jpaqlQuery.append( " having " ).append( ( (Renderable) getHaving() ).render( renderingContext ) );
		}
		finally {
			renderingContext.getClauseStack().pop();
		}
	}

	@SuppressWarnings({ "unchecked" })
	private void renderJoins(
			StringBuilder jpaqlQuery,
			RenderingContext renderingContext,
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
			RenderingContext renderingContext,
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
