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

import org.hibernate.Internal;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaQueryStructure;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmNode;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromClauseContainer;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClauseContainer;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

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

	public SqmQuerySpec(SqmQuerySpec<T> original, SqmCopyContext context) {
		super( original, context );
		if ( original.fromClause != null ) {
			this.fromClause = original.fromClause.copy( context );
		}
		if ( original.selectClause != null ) {
			this.selectClause = original.selectClause.copy( context );
		}
		if ( original.whereClause != null ) {
			this.whereClause = original.whereClause.copy( context );
		}
		this.hasPositionalGroupItem = original.hasPositionalGroupItem;
		if ( !original.groupByClauseExpressions.isEmpty() ) {
			this.groupByClauseExpressions = new ArrayList<>( original.groupByClauseExpressions.size() );
			for ( SqmExpression<?> groupByClauseExpression : original.groupByClauseExpressions ) {
				this.groupByClauseExpressions.add( groupByClauseExpression.copy( context ) );
			}
		}
		if ( original.havingClausePredicate != null ) {
			this.havingClausePredicate = original.havingClausePredicate.copy( context );
		}
	}

	@Override
	public SqmQuerySpec<T> copy(SqmCopyContext context) {
		final SqmQuerySpec<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmQuerySpec<T> querySpec = context.registerCopy( this, new SqmQuerySpec<>( nodeBuilder() ) );
		if ( fromClause != null ) {
			querySpec.fromClause = fromClause.copy( context );
		}
		if ( selectClause != null ) {
			querySpec.selectClause = selectClause.copy( context );
		}
		if ( whereClause != null ) {
			querySpec.whereClause = whereClause.copy( context );
		}
		querySpec.hasPositionalGroupItem = hasPositionalGroupItem;
		if ( !groupByClauseExpressions.isEmpty() ) {
			querySpec.groupByClauseExpressions = new ArrayList<>( groupByClauseExpressions.size() );
			for ( SqmExpression<?> groupByClauseExpression : groupByClauseExpressions ) {
				querySpec.groupByClauseExpressions.add( groupByClauseExpression.copy( context ) );
			}
		}
		if ( havingClausePredicate != null ) {
			querySpec.havingClausePredicate = havingClausePredicate.copy( context );
		}
		copyTo( querySpec, context );
		return querySpec;
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

	public boolean producesUniqueResults() {
		if ( fromClause.getRoots().size() != 1 ) {
			return false;
		}
		final SqmRoot<?> sqmRoot = fromClause.getRoots().get( 0 );
		if ( selectClause != null ) {
			final List<SqmSelection<?>> selections = selectClause.getSelections();
			if ( selections.size() != 1 || selections.get( 0 ).getSelectableNode() != sqmRoot ) {
				// If we select anything but the query root, let's be pessimistic about unique results
				return false;
			}
		}
		final List<SqmFrom<?, ?>> fromNodes = new ArrayList<>( sqmRoot.getSqmJoins().size() + 1 );
		fromNodes.add( sqmRoot );
		while ( !fromNodes.isEmpty() ) {
			final SqmFrom<?, ?> fromNode = fromNodes.remove( fromNodes.size() - 1 );
			for ( SqmJoin<?, ?> sqmJoin : fromNode.getSqmJoins() ) {
				if ( sqmJoin instanceof SqmAttributeJoin<?, ?> ) {
					final SqmAttributeJoin<?, ?> join = (SqmAttributeJoin<?, ?>) sqmJoin;
					if ( join.getAttribute().isCollection() ) {
						// Collections joins always alter cardinality
						return false;
					}
				}
				else {
					// For now, consider all non-attribute joins as cardinality altering
					return false;
				}
				fromNodes.add( sqmJoin );
			}
			fromNodes.addAll( fromNode.getSqmTreats() );
		}
		return true;
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
			fromNodes.addAll( fromNode.getSqmTreats() );
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

	public void setGroupByClauseExpressions(List<SqmExpression<?>> groupByClauseExpressions) {
		this.hasPositionalGroupItem = false;
		if ( groupByClauseExpressions == null ) {
			this.groupByClauseExpressions = Collections.emptyList();
		}
		else {
			this.groupByClauseExpressions = groupByClauseExpressions;
			for ( int i = 0; i < groupByClauseExpressions.size(); i++ ) {
				final SqmExpression<?> groupItem = groupByClauseExpressions.get( i );
				if ( groupItem instanceof SqmAliasedNodeRef ) {
					this.hasPositionalGroupItem = true;
					break;
				}
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
		final SqmSelectClause selectClause = getSelectClause();
		assert selectClause != null;
		// NOTE : this call comes from JPA which inherently supports just a
		// single (possibly "compound") selection.
		// We have this special case where we return the SqmSelectClause itself if it doesn't have exactly 1 item
		if ( selection instanceof SqmSelectClause ) {
			if ( selection != selectClause ) {
				final SqmSelectClause sqmSelectClause = (SqmSelectClause) selection;
				final List<SqmSelection<?>> selections = sqmSelectClause.getSelections();
				selectClause.setSelection( selections.get( 0 ).getSelectableNode() );
				for ( int i = 1; i < selections.size(); i++ ) {
					selectClause.addSelection( selections.get( i ) );
				}
			}
		}
		else {
			selectClause.setSelection( (SqmSelectableNode<?>) selection );
		}
		return this;
	}

	@Override
	public Set<SqmRoot<?>> getRoots() {
		assert getFromClause() != null;
		return new HashSet<>( getFromClause().getRoots() );
	}

	@Override
	public SqmQuerySpec<T> addRoot(JpaRoot<?> root) {
		if ( getFromClause() == null ) {
			setFromClause( new SqmFromClause() );
		}
		getFromClause().addRoot( (SqmRoot<?>) root );
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
		SqmWhereClause whereClause = getWhereClause();
		if ( whereClause == null ) {
			setWhereClause( whereClause = new SqmWhereClause( nodeBuilder() ) );
		}
		whereClause.setPredicate( (SqmPredicate) restriction );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setRestriction(Expression<Boolean> restriction) {
		SqmWhereClause whereClause = getWhereClause();
		if ( whereClause == null ) {
			setWhereClause( whereClause = new SqmWhereClause( nodeBuilder() ) );
		}
		whereClause.setPredicate( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setRestriction(Predicate... restrictions) {
		if ( restrictions == null ) {
			throw new IllegalArgumentException( "The predicate array cannot be null" );
		}
		else if ( restrictions.length == 0 ) {
			setWhereClause( null );
		}
		else {
			SqmWhereClause whereClause = getWhereClause();
			if ( whereClause == null ) {
				setWhereClause( whereClause = new SqmWhereClause( nodeBuilder() ) );
			}
			else {
				whereClause.setPredicate( null );
			}
			for ( Predicate restriction : restrictions ) {
				whereClause.applyPredicate( (SqmPredicate) restriction );
			}
		}
		return this;
	}

	@Override
	public List<SqmExpression<?>> getGroupingExpressions() {
		return groupByClauseExpressions;
	}

	@Override
	public SqmQuerySpec<T> setGroupingExpressions(List<? extends JpaExpression<?>> groupExpressions) {
		this.hasPositionalGroupItem = false;
		this.groupByClauseExpressions = new ArrayList<>( groupExpressions.size() );
		for ( JpaExpression<?> groupExpression : groupExpressions ) {
			if ( groupExpression instanceof SqmAliasedNodeRef ) {
				this.hasPositionalGroupItem = true;
			}
			this.groupByClauseExpressions.add( (SqmExpression<?>) groupExpression );
		}
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupingExpressions(JpaExpression<?>... groupExpressions) {
		this.hasPositionalGroupItem = false;
		this.groupByClauseExpressions = new ArrayList<>( groupExpressions.length );
		for ( JpaExpression<?> groupExpression : groupExpressions ) {
			if ( groupExpression instanceof SqmAliasedNodeRef ) {
				this.hasPositionalGroupItem = true;
			}
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
	public void validateQueryStructureAndFetchOwners() {
		validateFetchOwners();
	}

	public void validateFetchOwners() {
		if ( getFromClause() == null ) {
			return;
		}
		final Set<SqmFrom<?, ?>> selectedFromSet;
		final List<SqmRoot<?>> roots = getFromClause().getRoots();
		if ( selectClause == null || selectClause.getSelections().isEmpty() ) {
			if ( CollectionHelper.isEmpty( roots ) ) {
				throw new SemanticException( "No query roots were specified" );
			}
			else {
				selectedFromSet = Collections.singleton( roots.get( 0 ) );
			}
		}
		else {
			selectedFromSet = new HashSet<>( selectClause.getSelections().size() );
			for ( SqmSelection<?> selection : selectClause.getSelections() ) {
				collectSelectedFromSet( selectedFromSet, selection.getSelectableNode() );
			}
		}

		for ( SqmRoot<?> root : roots ) {
			validateFetchOwners( selectedFromSet, root );
		}
	}

	private void collectSelectedFromSet(Set<SqmFrom<?, ?>> selectedFromSet, SqmSelectableNode<?> selectableNode) {
		if ( selectableNode instanceof SqmJpaCompoundSelection<?> ) {
			final SqmJpaCompoundSelection<?> compoundSelection = (SqmJpaCompoundSelection<?>) selectableNode;
			for ( SqmSelectableNode<?> selectionItem : compoundSelection.getSelectionItems() ) {
				collectSelectedFromSet( selectedFromSet, selectionItem );
			}
		}
		else if ( selectableNode instanceof SqmDynamicInstantiation<?> ) {
			final SqmDynamicInstantiation<?> instantiation = (SqmDynamicInstantiation<?>) selectableNode;
			for ( SqmDynamicInstantiationArgument<?> selectionItem : instantiation.getArguments() ) {
				collectSelectedFromSet( selectedFromSet, selectionItem.getSelectableNode() );
			}
		}
		else if ( selectableNode instanceof SqmFrom<?, ?> ) {
			collectSelectedFromSet( selectedFromSet, (SqmFrom<?, ?>) selectableNode );
		}
		else if ( selectableNode instanceof SqmEntityValuedSimplePath<?> ) {
			final SqmEntityValuedSimplePath<?> path = (SqmEntityValuedSimplePath<?>) selectableNode;
			if ( CollectionPart.Nature.fromNameExact( path.getReferencedPathSource().getPathName() ) != null
					&& path.getLhs() instanceof SqmFrom<?, ?> ) {
				collectSelectedFromSet( selectedFromSet, (SqmFrom<?, ?>) path.getLhs() );
			}
		}
	}

	private void collectSelectedFromSet(Set<SqmFrom<?, ?>> selectedFromSet, SqmFrom<?, ?> sqmFrom) {
		selectedFromSet.add( sqmFrom );
		for ( SqmJoin<?, ?> sqmJoin : sqmFrom.getSqmJoins() ) {
			if ( sqmJoin.getReferencedPathSource().getSqmPathType() instanceof EmbeddableDomainType<?> ) {
				collectSelectedFromSet( selectedFromSet, sqmJoin );
			}
		}

		for ( SqmFrom<?, ?> sqmTreat : sqmFrom.getSqmTreats() ) {
			collectSelectedFromSet( selectedFromSet, sqmTreat );
		}
	}

	private void validateFetchOwners(Set<SqmFrom<?, ?>> selectedFromSet, SqmFrom<?, ?> owner) {
		for ( SqmJoin<?, ?> sqmJoin : owner.getSqmJoins() ) {
			if ( sqmJoin instanceof SqmAttributeJoin<?, ?> ) {
				final SqmAttributeJoin<?, ?> attributeJoin = (SqmAttributeJoin<?, ?>) sqmJoin;
				if ( attributeJoin.isFetched() ) {
					assertFetchOwner( selectedFromSet, owner, sqmJoin );
					// Only need to check the first level
					continue;
				}
			}
			validateFetchOwners( selectedFromSet, sqmJoin );
		}
		for ( SqmFrom<?, ?> sqmTreat : owner.getSqmTreats() ) {
			validateFetchOwners( selectedFromSet, sqmTreat );
		}
	}

	private void assertFetchOwner(Set<SqmFrom<?, ?>> selectedFromSet, SqmFrom<?, ?> owner, SqmJoin<?, ?> sqmJoin) {
		if ( !selectedFromSet.contains( owner ) ) {
			throw new SemanticException(
					"query specified join fetching, but the owner " +
							"of the fetched association was not present in the select list " +
							"[" + sqmJoin.asLoggableText() + "]"
			);
		}
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		if ( selectClause != null ) {
			sb.append( "select " );
			if ( selectClause.isDistinct() ) {
				sb.append( "distinct " );
			}
			final List<SqmSelection<?>> selections = selectClause.getSelections();
			selections.get( 0 ).appendHqlString( sb );
			for ( int i = 1; i < selections.size(); i++ ) {
				sb.append( ", " );
				selections.get( i ).appendHqlString( sb );
			}
		}
		if ( fromClause != null ) {
			sb.append( " from" );
			fromClause.appendHqlString( sb );
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

	@Internal
	public boolean groupByClauseContains(NavigablePath navigablePath, SqmToSqlAstConverter sqlAstConverter) {
		if ( groupByClauseExpressions.isEmpty() ) {
			return false;
		}
		return navigablePathsContain( sqlAstConverter.resolveMetadata(
				this,
				SqmUtil::getGroupByNavigablePaths
		), navigablePath );
	}

	@Internal
	public boolean orderByClauseContains(NavigablePath navigablePath, SqmToSqlAstConverter sqlAstConverter) {
		final SqmOrderByClause orderByClause = getOrderByClause();
		if ( orderByClause == null || orderByClause.getSortSpecifications().isEmpty() ) {
			return false;
		}
		return navigablePathsContain( sqlAstConverter.resolveMetadata(
				this,
				SqmUtil::getOrderByNavigablePaths
		), navigablePath );
	}

	private boolean navigablePathsContain(List<NavigablePath> navigablePaths, NavigablePath navigablePath) {
		for ( NavigablePath path : navigablePaths ) {
			if ( path.isParentOrEqual( navigablePath ) ) {
				return true;
			}
		}
		return false;
	}
}
