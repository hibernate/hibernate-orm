/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaQueryStructure;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmNode;
import org.hibernate.query.sqm.tree.cte.SqmCteConsumer;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromClauseContainer;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClauseContainer;
import org.hibernate.type.StandardBasicTypes;

/**
 * Defines the commonality between a root query and a subquery.
 *
 * @author Steve Ebersole
 */
public class SqmQuerySpec<T> implements SqmCteConsumer, SqmNode, SqmFromClauseContainer, SqmWhereClauseContainer, JpaQueryStructure<T> {
	private final NodeBuilder nodeBuilder;

	private SqmFromClause fromClause;
	private SqmSelectClause selectClause;
	private SqmWhereClause whereClause;

	private List<SqmExpression<?>> groupByClauseExpressions = Collections.emptyList();
	private SqmPredicate havingClausePredicate;

	private SqmOrderByClause orderByClause;

	private SqmExpression<?> limitExpression;
	private SqmExpression<?> offsetExpression;

	public SqmQuerySpec(NodeBuilder nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}

	@Override
	public SqmFromClause getFromClause() {
		return fromClause;
	}

	public void setFromClause(SqmFromClause fromClause) {
		this.fromClause = fromClause;
	}

	public SqmSelectClause getSelectClause() {
		return selectClause;
	}

	public void setSelectClause(SqmSelectClause selectClause) {
		this.selectClause = selectClause;
	}

	@Override
	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	public void setWhereClause(SqmWhereClause whereClause) {
		this.whereClause = whereClause;
	}

	@Override
	public void applyPredicate(SqmPredicate predicate) {
		if ( predicate == null ) {
			return;
		}

		if ( whereClause == null ) {
			whereClause = new SqmWhereClause( nodeBuilder() );
		}

		whereClause.applyPredicate( predicate );
	}

	public List<SqmExpression<?>> getGroupByClauseExpressions() {
		return groupByClauseExpressions;
	}

	public void setGroupByClauseExpressions(List<SqmExpression<?>> groupByClauseExpressions) {
		this.groupByClauseExpressions = groupByClauseExpressions == null
				? Collections.emptyList()
				: groupByClauseExpressions;
	}

	public SqmPredicate getHavingClausePredicate() {
		return havingClausePredicate;
	}

	public void setHavingClausePredicate(SqmPredicate havingClausePredicate) {
		this.havingClausePredicate = havingClausePredicate;
	}

	public SqmOrderByClause getOrderByClause() {
		return orderByClause;
	}

	public void setOrderByClause(SqmOrderByClause orderByClause) {
		this.orderByClause = orderByClause;
	}

	public SqmExpression<?> getLimitExpression() {
		return limitExpression;
	}

	public void setLimitExpression(SqmExpression<?> limitExpression) {
		if ( limitExpression != null ) {
			limitExpression.applyInferableType( StandardBasicTypes.INTEGER );
		}
		this.limitExpression = limitExpression;
	}

	public SqmExpression<?> getOffsetExpression() {
		return offsetExpression;
	}

	public void setOffsetExpression(SqmExpression<?> offsetExpression) {
		if ( offsetExpression != null ) {
			offsetExpression.applyInferableType( StandardBasicTypes.INTEGER );
		}
		this.offsetExpression = offsetExpression;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public boolean isDistinct() {
		assert getSelectClause() != null;
		return getSelectClause().isDistinct();
	}

	@Override
	public SqmQuerySpec<T> setDistinct(boolean distinct) {
		assert getSelectClause() != null;
		getSelectClause().makeDistinct( distinct );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaSelection<T> getSelection() {
		assert getSelectClause() != null;
		return (JpaSelection<T>) getSelectClause().resolveJpaSelection();
	}

	@Override
	public SqmQuerySpec<T> setSelection(JpaSelection<T> selection) {
		assert getSelectClause() != null;
		// NOTE : this call comes from JPA which inherently supports just a
		// single (possibly "compound") selection
		getSelectClause().setSelection( (SqmSelectableNode) selection );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set getRoots() {
		assert getFromClause() != null;
		return new HashSet<>( getFromClause().getRoots() );
	}

	@Override
	public SqmQuerySpec<T> addRoot(JpaRoot<?> root) {
		if ( getFromClause() == null ) {
			setFromClause( new SqmFromClause() );
		}
		getFromClause().addRoot( (SqmRoot) root );
		return this;
	}

	@Override
	public SqmPredicate getRestriction() {
		if ( getWhereClause() == null ) {
			return null;
		}
		return getWhereClause().getPredicate();
	}

	@Override
	public SqmQuerySpec<T> setRestriction(JpaPredicate restriction) {
		if ( getWhereClause() == null ) {
			setWhereClause( new SqmWhereClause( nodeBuilder() ) );
		}
		getWhereClause().setPredicate( (SqmPredicate) restriction );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setRestriction(Expression<Boolean> restriction) {
		if ( getWhereClause() == null ) {
			setWhereClause( new SqmWhereClause( nodeBuilder() ) );
		}
		getWhereClause().setPredicate( nodeBuilder.wrap( restriction ) );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setRestriction(Predicate... restrictions) {
		if ( getWhereClause() == null ) {
			setWhereClause( new SqmWhereClause( nodeBuilder() ) );
		}
		getWhereClause().applyPredicates( (SqmPredicate[]) restrictions );
		return null;
	}

	@Override
	public List<SqmExpression<?>> getGroupingExpressions() {
		return groupByClauseExpressions;
	}

	@Override
	public SqmQuerySpec<T> setGroupingExpressions(List<? extends JpaExpression<?>> groupExpressions) {
		this.groupByClauseExpressions = new ArrayList<>( groupExpressions.size() );
		for ( JpaExpression<?> groupExpression : groupExpressions ) {
			this.groupByClauseExpressions.add( (SqmExpression<?>) groupExpression );
		}
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupingExpressions(JpaExpression<?>... groupExpressions) {
		this.groupByClauseExpressions = new ArrayList<>( groupExpressions.length );
		for ( JpaExpression<?> groupExpression : groupExpressions ) {
			this.groupByClauseExpressions.add( (SqmExpression<?>) groupExpression );
		}
		return this;
	}

	@Override
	public SqmPredicate getGroupRestriction() {
		return havingClausePredicate;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(JpaPredicate restriction) {
		havingClausePredicate = (SqmPredicate) restriction;
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(Expression<Boolean> restriction) {
		havingClausePredicate = nodeBuilder.wrap( restriction );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(Predicate... restrictions) {
		havingClausePredicate = nodeBuilder.wrap( restrictions );
		return this;
	}

	@Override
	public List<SqmSortSpecification> getSortSpecifications() {
		if ( getOrderByClause() == null ) {
			return Collections.emptyList();
		}

		return getOrderByClause().getSortSpecifications();
	}

	@Override
	public SqmQuerySpec<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		if ( getOrderByClause() == null ) {
			setOrderByClause( new SqmOrderByClause() );
		}

		//noinspection unchecked
		getOrderByClause().setSortSpecifications( (List) sortSpecifications );

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<?> getLimit() {
		return getLimitExpression();
	}

	@Override
	public SqmQuerySpec<T> setLimit(JpaExpression<?> limit) {
		setLimitExpression( (SqmExpression<?>) limit );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<?> getOffset() {
		return getOffsetExpression();
	}

	@Override
	public SqmQuerySpec<T> setOffset(JpaExpression<?> offset) {
		setOffsetExpression( (SqmExpression<?>) offset );
		return this;
	}
}
