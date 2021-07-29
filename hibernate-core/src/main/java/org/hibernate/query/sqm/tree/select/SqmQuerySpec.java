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

import org.hibernate.query.FetchClauseType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaQueryStructure;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmNode;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromClauseContainer;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClauseContainer;

/**
 * Defines the commonality between a root query and a subquery.
 *
 * @author Steve Ebersole
 */
public class SqmQuerySpec<T> extends SqmQueryPart<T>
		implements SqmNode, SqmFromClauseContainer, SqmWhereClauseContainer, JpaQueryStructure<T> {
	private SqmFromClause fromClause;
	private SqmSelectClause selectClause;
	private SqmWhereClause whereClause;

	private boolean hasPositionalGroupItem;
	private List<SqmExpression<?>> groupByClauseExpressions = Collections.emptyList();
	private SqmPredicate havingClausePredicate;

	public SqmQuerySpec(NodeBuilder nodeBuilder) {
		super( nodeBuilder );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQuerySpec( this );
	}

	@Override
	public SqmQuerySpec<T> getFirstQuerySpec() {
		return this;
	}

	@Override
	public SqmQuerySpec<T> getLastQuerySpec() {
		return this;
	}

	@Override
	public boolean isSimpleQueryPart() {
		return true;
	}

	@Override
	public SqmFromClause getFromClause() {
		return fromClause;
	}

	public void setFromClause(SqmFromClause fromClause) {
		this.fromClause = fromClause;
	}

	public boolean containsCollectionFetches() {
		final List<SqmFrom<?, ?>> fromNodes = new ArrayList<>( fromClause.getRoots() );
		while ( !fromNodes.isEmpty() ) {
			final SqmFrom<?, ?> fromNode = fromNodes.remove( fromNodes.size() - 1 );
			for ( SqmJoin<?, ?> sqmJoin : fromNode.getSqmJoins() ) {
				if ( sqmJoin instanceof SqmAttributeJoin<?, ?> ) {
					final SqmAttributeJoin<?, ?> join = (SqmAttributeJoin<?, ?>) sqmJoin;
					if ( join.isFetched() && join.getAttribute().isCollection() ) {
						return true;
					}
				}
				fromNodes.add( sqmJoin );
			}
		}
		return false;
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

	public boolean hasPositionalGroupItem() {
		return hasPositionalGroupItem;
	}

	public List<SqmExpression<?>> getGroupByClauseExpressions() {
		return groupByClauseExpressions;
	}

	public void setGroupByClauseExpressions(final List<SqmExpression<?>> lgroupByClauseExpressions) {
		this.groupByClauseExpressions = lgroupByClauseExpressions == null
				? Collections.emptyList()
				: lgroupByClauseExpressions;

		for ( int i = 0; i < groupByClauseExpressions.size(); i++ ) {
			final SqmExpression<?> groupItem = groupByClauseExpressions.get( i );
			if ( groupItem instanceof SqmAliasedNodeRef ) {
				hasPositionalGroupItem = true;
			}
		}
	}

	public SqmPredicate getHavingClausePredicate() {
		return havingClausePredicate;
	}

	public void setHavingClausePredicate(SqmPredicate havingClausePredicate) {
		this.havingClausePredicate = havingClausePredicate;
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
	public Set<SqmRoot<?>> getRoots() {
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
		getWhereClause().setPredicate( nodeBuilder().wrap( restriction ) );
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
		havingClausePredicate = nodeBuilder().wrap( restriction );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(Predicate... restrictions) {
		havingClausePredicate = nodeBuilder().wrap( restrictions );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		super.setSortSpecifications( sortSpecifications );
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

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<?> getFetch() {
		return getFetchExpression();
	}

	@Override
	public SqmQuerySpec<T> setFetch(JpaExpression<?> fetch) {
		setFetchExpression( (SqmExpression<?>) fetch );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setFetch(JpaExpression<?> fetch, FetchClauseType fetchClauseType) {
		setFetchExpression( (SqmExpression<?>) fetch, fetchClauseType );
		return this;
	}

	@Override
	public void validateQueryGroupFetchStructure() {
		// No-op
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		if ( selectClause != null ) {
			sb.append( "select " );
			if ( selectClause.isDistinct() ) {
				sb.append( "distinct " );
			}
			final List<SqmSelection> selections = selectClause.getSelections();
			selections.get( 0 ).appendHqlString( sb );
			for ( int i = 1; i < selections.size(); i++ ) {
				sb.append( ", " );
				selections.get( i ).appendHqlString( sb );
			}
		}
		if ( fromClause != null ) {
			sb.append( " from " );
			String separator = "";
			for ( SqmRoot<?> root : fromClause.getRoots() ) {
				sb.append( separator );
				if ( root.isCorrelated() ) {
					if ( root.containsOnlyInnerJoins() ) {
						appendJoins( root, root.getCorrelationParent().getExplicitAlias(), sb );
					}
					else {
						sb.append( root.getCorrelationParent().getExplicitAlias() );
						if ( root.getExplicitAlias() != null ) {
							sb.append( ' ' ).append( root.getExplicitAlias() );
						}
						appendJoins( root, sb );
					}
				}
				else {
					sb.append( root.getEntityName() );
					if ( root.getExplicitAlias() != null ) {
						sb.append( ' ' ).append( root.getExplicitAlias() );
					}
					appendJoins( root, sb );
				}
				separator = ", ";
			}
		}
		if ( whereClause != null && whereClause.getPredicate() != null ) {
			sb.append( " where " );
			whereClause.getPredicate().appendHqlString( sb );
		}
		if ( !groupByClauseExpressions.isEmpty() ) {
			sb.append( " group by " );
			groupByClauseExpressions.get( 0 ).appendHqlString( sb );
			for ( int i = 1; i < groupByClauseExpressions.size(); i++ ) {
				sb.append( ", " );
				groupByClauseExpressions.get( i ).appendHqlString( sb );
			}
		}
		if ( havingClausePredicate != null ) {
			sb.append( " having " );
			havingClausePredicate.appendHqlString( sb );
		}

		super.appendHqlString( sb );
	}

	private void appendJoins(SqmFrom<?, ?> sqmFrom, StringBuilder sb) {
		for ( SqmJoin<?, ?> sqmJoin : sqmFrom.getSqmJoins() ) {
			switch ( sqmJoin.getSqmJoinType() ) {
				case LEFT:
					sb.append( " left join " );
					break;
				case RIGHT:
					sb.append( " right join " );
					break;
				case INNER:
					sb.append( " join " );
					break;
				case FULL:
					sb.append( " full join " );
					break;
				case CROSS:
					sb.append( " cross join " );
					break;
			}
			if ( sqmJoin instanceof SqmAttributeJoin<?, ?> ) {
				final SqmAttributeJoin<?, ?> attributeJoin = (SqmAttributeJoin<?, ?>) sqmJoin;
				sb.append( sqmFrom.getExplicitAlias() ).append( '.' );
				sb.append( (attributeJoin).getAttribute().getName() );
				if ( sqmJoin.getExplicitAlias() != null ) {
					sb.append( ' ' ).append( sqmJoin.getExplicitAlias() );
				}
				if ( attributeJoin.getJoinPredicate() != null ) {
					sb.append( " on " );
					attributeJoin.getJoinPredicate().appendHqlString( sb );
				}
				appendJoins( sqmJoin, sb );
			}
			else if ( sqmJoin instanceof SqmCrossJoin<?> ) {
				sb.append( ( (SqmCrossJoin<?>) sqmJoin ).getEntityName() );
				if ( sqmJoin.getExplicitAlias() != null ) {
					sb.append( ' ' ).append( sqmJoin.getExplicitAlias() );
				}
				appendJoins( sqmJoin, sb );
			}
			else if ( sqmJoin instanceof SqmEntityJoin<?> ) {
				final SqmEntityJoin<?> sqmEntityJoin = (SqmEntityJoin<?>) sqmJoin;
				sb.append( (sqmEntityJoin).getEntityName() );
				if ( sqmJoin.getExplicitAlias() != null ) {
					sb.append( ' ' ).append( sqmJoin.getExplicitAlias() );
				}
				if ( sqmEntityJoin.getJoinPredicate() != null ) {
					sb.append( " on " );
					sqmEntityJoin.getJoinPredicate().appendHqlString( sb );
				}
				appendJoins( sqmJoin, sb );
			}
			else {
				throw new UnsupportedOperationException( "Unsupported join: " + sqmJoin );
			}
		}
	}

	private void appendJoins(SqmFrom<?, ?> sqmFrom, String correlationPrefix, StringBuilder sb) {
		String separator = "";
		for ( SqmJoin<?, ?> sqmJoin : sqmFrom.getSqmJoins() ) {
			assert sqmJoin instanceof SqmAttributeJoin<?, ?>;
			sb.append( separator );
			sb.append( correlationPrefix ).append( '.' );
			sb.append( ( (SqmAttributeJoin<?, ?>) sqmJoin ).getAttribute().getName() );
			if ( sqmJoin.getExplicitAlias() != null ) {
				sb.append( ' ' ).append( sqmJoin.getExplicitAlias() );
			}
			appendJoins( sqmJoin, sb );
			separator = ", ";
		}
	}
}
