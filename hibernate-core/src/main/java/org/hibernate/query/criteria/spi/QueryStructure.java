/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaQueryStructure;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;

import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class QueryStructure<T> extends AbstractNode implements JpaQueryStructure<T> {

	private boolean distinct;
	private SelectionImplementor<T> selection;

	private final Set<RootImplementor<?>> roots = new LinkedHashSet<>();

	private PredicateImplementor restriction;

	private List<SortSpecification> sortSpecifications;

	private List<ExpressionImplementor<?>> grouping;
	private PredicateImplementor having;

	private ExpressionImplementor limit;
	private ExpressionImplementor offset;


	public QueryStructure(CriteriaNodeBuilder builder) {
		super( builder );
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitQueryStructure( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Select clause

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	@Override
	public QueryStructure setDistinct(boolean distinct) {
		this.distinct = distinct;
		return this;
	}

	@Override
	public SelectionImplementor<T> getSelection() {
		return selection;
	}

	@Override
	public QueryStructure setSelection(JpaSelection<T> selection) {
		return setSelection( (SelectionImplementor<T>) selection );
	}

	public QueryStructure setSelection(SelectionImplementor<T> selection) {
		this.selection = selection;
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// From clause

	@Override
	public Set<? extends RootImplementor<?>> getRoots() {
		return roots;
	}

	@Override
	public QueryStructure addRoot(JpaRoot<?> root) {
		return addRoot( (RootImplementor<?>) root );
	}

	@SuppressWarnings("WeakerAccess")
	public QueryStructure addRoot(RootImplementor<?> root) {
		roots.add( root );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Where clause

	@Override
	public PredicateImplementor getRestriction() {
		return restriction;
	}

	public void visitRestriction(Consumer<PredicateImplementor> consumer) {
		if ( restriction != null ) {
			consumer.accept( restriction );
		}
	}

	@Override
	public QueryStructure<T> setRestriction(JpaPredicate restriction) {
		this.restriction = (PredicateImplementor) restriction;
		return this;
	}

	@Override
	public QueryStructure<T> setRestriction(Expression<Boolean> restriction) {
		this.restriction = nodeBuilder().wrap( restriction );
		return this;
	}

	@Override
	public QueryStructure<T> setRestriction(Predicate... restrictions) {
		this.restriction = nodeBuilder().wrap( restrictions );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping (group-by / having) clause

	@Override
	public List<? extends ExpressionImplementor<?>> getGroupingExpressions() {
		return grouping;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryStructure<T> setGroupingExpressions(List<? extends JpaExpression<?>> grouping) {
		this.grouping = (List) grouping;
		return this;
	}

	@Override
	public QueryStructure<T> setGroupingExpressions(JpaExpression<?>... grouping) {
		return null;
	}

	@Override
	public PredicateImplementor getGroupRestriction() {
		return having;
	}

	@Override
	public QueryStructure<T> setGroupRestriction(JpaPredicate restrictions) {
		this.having = (PredicateImplementor) restrictions;
		return this;
	}

	@Override
	public QueryStructure<T> setGroupRestriction(Expression<Boolean> restriction) {
		this.having = nodeBuilder().wrap( restriction );
		return this;
	}

	@Override
	public QueryStructure<T> setGroupRestriction(Predicate... restrictions) {
		this.having = nodeBuilder().wrap( restrictions );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordering clause

	@Override
	public List<? extends SortSpecification> getSortSpecifications() {
		return coalesce( sortSpecifications, Collections.emptyList() );
	}

	public void visitSortSpecifications(Consumer<SortSpecification> sortSpec) {
		if ( sortSpecifications != null ) {
			sortSpecifications.forEach( sortSpec );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryStructure<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		this.sortSpecifications = (List) sortSpecifications;
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit clause

	@Override
	@SuppressWarnings("unchecked")
	public <X> ExpressionImplementor<X> getLimit() {
		return limit;
	}

	@Override
	public QueryStructure<T> setLimit(JpaExpression<?> limit) {
		this.limit = (ExpressionImplementor) limit;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> ExpressionImplementor<X> getOffset() {
		return offset;
	}

	@Override
	public QueryStructure<T> setOffset(JpaExpression offset) {
		this.offset = (ExpressionImplementor) offset;
		return this;
	}
}
