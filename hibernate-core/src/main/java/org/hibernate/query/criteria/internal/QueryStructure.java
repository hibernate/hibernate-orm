/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.hibernate.query.criteria.FromClauseImpl;
import org.hibernate.query.criteria.JpaFromImplementor;
import org.hibernate.query.criteria.SelectClauseImpl;
import org.hibernate.query.criteria.internal.path.RootImpl;
import org.hibernate.query.sqm.produce.spi.criteria.JpaExpression;
import org.hibernate.query.sqm.produce.spi.criteria.JpaOrder;
import org.hibernate.query.sqm.produce.spi.criteria.JpaPredicate;
import org.hibernate.query.sqm.produce.spi.criteria.JpaQuerySpec;
import org.hibernate.query.sqm.produce.spi.criteria.JpaSubquery;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaFromClause;
import org.hibernate.query.sqm.produce.spi.criteria.select.JpaSelectClause;
import org.hibernate.sql.NotYetImplementedException;

/**
 * Models basic query structure.  Used as a delegate in implementing both
 * {@link javax.persistence.criteria.CriteriaQuery} and
 * {@link javax.persistence.criteria.Subquery}.
 * <p/>
 * Note the <tt>ORDER BY</tt> specs are neglected here.  That's because it is not valid
 * for a subquery to define an <tt>ORDER BY</tt> clause.  So we just handle them on the
 * root query directly...
 *
 * @author Steve Ebersole
 */
public class QueryStructure<T> implements JpaQuerySpec<T> {

	// todo (6.0) : things that contain a QueryStructure ought to use SqmQuerySpec instead
	//		to fit with having SQM simply act as our JPA criteria impls.
	//
	//		this class will be deleted in the temporary cleanup for compilation until
	//		Christian finishes that work

	private final AbstractQuery<T> owner;
	private final CriteriaBuilderImpl criteriaBuilder;
	private final boolean isSubQuery;

	public QueryStructure(AbstractQuery<T> owner, CriteriaBuilderImpl criteriaBuilder) {
		this.owner = owner;
		this.criteriaBuilder = criteriaBuilder;
		this.isSubQuery = Subquery.class.isInstance( owner );
	}

	private SelectClauseImpl<T> jpaSelectClause = new SelectClauseImpl<>();
	private FromClauseImpl fromClause = new FromClauseImpl();
	private JpaPredicate restriction;
	private List<JpaExpression<?>> groupings = Collections.emptyList();
	private JpaPredicate having;
	private List<JpaOrder> jpaOrderByList;
	private List<JpaSubquery<?>> subqueries;


	// PARAMETERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<ParameterExpression<?>> getParameters() {
		final Set<ParameterExpression<?>> parameters = new LinkedHashSet<>();
		final ParameterRegistry registry = parameters::add;

		ParameterContainer.Helper.possibleParameter(getSelection(), registry);
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
		return jpaSelectClause.isDistinct();
	}

	public void setDistinct(boolean distinct) {
		jpaSelectClause.setDistinct( distinct );
	}

	@Override
	public JpaSelectClause<T> getSelectClause() {
		return jpaSelectClause;
	}

	public Selection<? extends T> getSelection() {
		return getSelectClause().getSelection();
	}

	public void setSelection(Selection<? extends T> selection) {
		throw new NotYetImplementedException(  );
//		this.selection = selection;
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<Root<?>> getRoots() {
		return fromClause.getRoots().stream().map( jpaRoot -> (Root<?>) jpaRoot ).collect( Collectors.toSet() );
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
		fromClause.addRoot( root );
		return root;
	}

	@Override
	public JpaFromClause getFromClause() {
		return fromClause;
	}


	// CORRELATION ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void addCorrelationRoot(JpaFromImplementor fromImplementor) {
		if ( !isSubQuery ) {
			throw new IllegalStateException( "Query is not identified as sub-query" );
		}

		throw new NotYetImplementedException(  );

//		if ( correlationRoots == null ) {
//			correlationRoots = new HashSet<JpaFromImplementor>();
//		}
//		correlationRoots.add( fromImplementor );
	}

	public Set<Join<?, ?>> collectCorrelatedJoins() {
		if ( !isSubQuery ) {
			throw new IllegalStateException( "Query is not identified as sub-query" );
		}

		throw new NotYetImplementedException(  );

//		final Set<Join<?, ?>> correlatedJoins;
//		if ( correlationRoots != null ) {
//			correlatedJoins = new HashSet<Join<?,?>>();
//			for ( JpaFromImplementor<?,?> correlationRoot : correlationRoots ) {
//				if (correlationRoot instanceof Join<?,?> && correlationRoot.isCorrelated()) {
//					correlatedJoins.add( (Join<?,?>) correlationRoot );
//				}
//				correlatedJoins.addAll( correlationRoot.getJoins() );
//			}
//		}
//		else {
//			correlatedJoins = Collections.emptySet();
//		}
//		return correlatedJoins;
	}


	// RESTRICTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public JpaPredicate getRestriction() {
		return restriction;
	}

	public void setRestriction(Predicate restriction) {
		this.restriction = (JpaPredicate) restriction;
	}


	// GROUPINGS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Expression<?>> getGroupings() {
		return groupings.stream().map( jpaExpression -> (Expression<?>) jpaExpression ).collect( Collectors.toList() );
	}

	public void setGroupings(List<Expression<?>> groupings) {
		this.groupings = groupings.stream().map( expression -> (JpaExpression<?>) expression ).collect( Collectors.toList() );
	}

	public void setGroupings(Expression<?>... groupings) {
		if ( groupings != null && groupings.length > 0 ) {
			this.groupings = Arrays.asList( groupings ).stream().map( expression -> (JpaExpression<?>) expression ).collect( Collectors.toList() );
		}
		else {
			this.groupings = Collections.emptyList();
		}
	}

	public Predicate getHaving() {
		return having;
	}

	public void setHaving(Predicate having) {
		this.having = (JpaPredicate) having;
	}

	@Override
	public List<JpaOrder> getOrderList() {
		return jpaOrderByList;
	}

	// SUB-QUERIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Subquery<?>> getSubqueries() {
		return subqueries.stream().map( jpaSubquery -> (Subquery<?>) jpaSubquery ).collect( Collectors.toList() );
	}

	public List<Subquery<?>> internalGetSubqueries() {
		if ( subqueries == null ) {
			subqueries = new ArrayList<>();
		}
		return subqueries.stream().map( jpaSubquery -> (Subquery<?>) jpaSubquery ).collect( Collectors.toList() );
	}

	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		CriteriaSubqueryImpl<U> subquery = new CriteriaSubqueryImpl<U>(
				criteriaBuilder,
				criteriaBuilder.resolveJavaTypeDescriptor( subqueryType ),
				owner
		);
		internalGetSubqueries().add( subquery );
		return subquery;
	}
}
