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
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromClauseContainer;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClauseContainer;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Defines the commonality between a root query and a subquery.
 *
 * @author Steve Ebersole
 */
public class SqmQuerySpec<T> implements SqmNode, SqmFromClauseContainer, SqmWhereClauseContainer, JpaQueryStructure<T> {
	private final NodeBuilder nodeBuilder;

	private SqmFromClause fromClause;
	private SqmSelectClause selectClause;
	private SqmWhereClause whereClause;

	private SqmGroupByClause groupByClause;
	private SqmHavingClause havingClause;

	private SqmOrderByClause orderByClause;

	private SqmExpression limitExpression;
	private SqmExpression offsetExpression;

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

	public SqmGroupByClause getGroupByClause() {
		return groupByClause;
	}

	public void setGroupByClause(SqmGroupByClause groupByClause) {
		this.groupByClause = groupByClause;
	}

	public SqmHavingClause getHavingClause() {
		return havingClause;
	}

	public void setHavingClause(SqmHavingClause havingClause) {
		this.havingClause = havingClause;
	}

	public SqmOrderByClause getOrderByClause() {
		return orderByClause;
	}

	public void setOrderByClause(SqmOrderByClause orderByClause) {
		this.orderByClause = orderByClause;
	}

	public SqmExpression getLimitExpression() {
		return limitExpression;
	}

	public void setLimitExpression(SqmExpression<?> limitExpression) {
		if ( limitExpression != null ) {
			limitExpression.applyInferableType( StandardSpiBasicTypes.INTEGER );
		}
		this.limitExpression = limitExpression;
	}

	public SqmExpression getOffsetExpression() {
		return offsetExpression;
	}

	public void setOffsetExpression(SqmExpression<?> offsetExpression) {
		if ( offsetExpression != null ) {
			offsetExpression.applyInferableType( StandardSpiBasicTypes.INTEGER );
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
	public SqmQuerySpec setDistinct(boolean distinct) {
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
	public SqmQuerySpec setSelection(JpaSelection<T> selection) {
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
	public SqmQuerySpec addRoot(JpaRoot<?> root) {
		assert getFromClause() != null;
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
	public List<SqmExpression> getGroupingExpressions() {
		if ( getGroupByClause() == null ) {
			return Collections.emptyList();
		}

		final List<SqmExpression> list = new ArrayList<>();
		getGroupByClause().visitGroupings(
				sqmGrouping -> list.add( sqmGrouping.getExpression() )
		);
		return list;
	}

	@Override
	public SqmQuerySpec<T> setGroupingExpressions(List<? extends JpaExpression<?>> groupExpressions) {
		if ( getGroupByClause() == null ) {
			setGroupByClause( new SqmGroupByClause() );
		}
		else {
			getGroupByClause().clearGroupings();
		}

		for ( JpaExpression<?> groupExpression : groupExpressions ) {
			getGroupByClause().addGrouping( (SqmExpression) groupExpression );
		}

		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupingExpressions(JpaExpression<?>... groupExpressions) {
		if ( getGroupByClause() == null ) {
			setGroupByClause( new SqmGroupByClause() );
		}
		else {
			getGroupByClause().clearGroupings();
		}

		for ( JpaExpression<?> groupExpression : groupExpressions ) {
			getGroupByClause().addGrouping( (SqmExpression) groupExpression );
		}

		return this;
	}

	@Override
	public SqmPredicate getGroupRestriction() {
		if ( getHavingClause() == null ) {
			return null;
		}
		return getHavingClause().getPredicate();
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(JpaPredicate restriction) {
		if ( getHavingClause() == null ) {
			setHavingClause( new SqmHavingClause( (SqmPredicate) restriction ) );
		}
		else {
			getHavingClause().setPredicate( (SqmPredicate) restriction );
		}
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(Expression<Boolean> restriction) {
		final SqmPredicate predicate = nodeBuilder.wrap( restriction );
		if ( getHavingClause() == null ) {
			setHavingClause( new SqmHavingClause( predicate ));
		}
		else {
			getHavingClause().setPredicate( predicate );
		}
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(Predicate... restrictions) {
		final SqmPredicate predicate = nodeBuilder.wrap( restrictions );
		if ( getHavingClause() == null ) {
			setHavingClause( new SqmHavingClause( predicate ));
		}
		else {
			getHavingClause().setPredicate( predicate );
		}
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
	public SqmExpression getLimit() {
		return getLimitExpression();
	}

	@Override
	public SqmQuerySpec<T> setLimit(JpaExpression<?> limit) {
		setLimitExpression( (SqmExpression) limit );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression getOffset() {
		return getOffsetExpression();
	}

	@Override
	public SqmQuerySpec<T> setOffset(JpaExpression offset) {
		setOffsetExpression( (SqmExpression) offset );
		return this;
	}
}
