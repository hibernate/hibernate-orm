/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaQueryStructure;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmNode;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
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
import jakarta.persistence.metamodel.SingularAttribute;

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
		// Enforce non-nullness of the fromClause
		this.fromClause = new SqmFromClause();
	}

	public SqmQuerySpec(SqmQuerySpec<T> original, SqmCopyContext context) {
		super( original, context );
		this.fromClause = original.fromClause.copy( context );
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
		querySpec.fromClause = fromClause.copy( context );
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
		// Enforce non-nullness of the fromClause
		if ( fromClause != null ) {
			this.fromClause = fromClause;
		}
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
				if ( sqmJoin instanceof SqmAttributeJoin<?, ?> join ) {
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
				if ( sqmJoin instanceof SqmAttributeJoin<?, ?> join ) {
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
	public List<SqmRoot<?>> getRootList() {
		assert getFromClause() != null;
		return getFromClause().getRoots();
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
		if ( restriction == null ) {
			setWhereClause( null );
		}
		else {
			SqmWhereClause whereClause = getWhereClause();
			if ( whereClause == null ) {
				setWhereClause( whereClause = new SqmWhereClause( nodeBuilder() ) );
			}
			whereClause.setPredicate( nodeBuilder().wrap( restriction ) );
		}
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
	public SqmQuerySpec<T> setGroupingExpressions(List<? extends Expression<?>> groupExpressions) {
		this.hasPositionalGroupItem = false;
		this.groupByClauseExpressions = new ArrayList<>( groupExpressions.size() );
		for ( Expression<?> groupExpression : groupExpressions ) {
			if ( groupExpression instanceof SqmAliasedNodeRef ) {
				this.hasPositionalGroupItem = true;
			}
			this.groupByClauseExpressions.add( (SqmExpression<?>) groupExpression );
		}
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupingExpressions(Expression<?>... groupExpressions) {
		this.hasPositionalGroupItem = false;
		this.groupByClauseExpressions = new ArrayList<>( groupExpressions.length );
		for ( Expression<?> groupExpression : groupExpressions ) {
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
	public SqmQuerySpec<T> setOffset(JpaExpression<? extends Number> offset) {
		setOffsetExpression( (SqmExpression<? extends Number>) offset );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setFetch(JpaExpression<? extends Number> fetch) {
		setFetchExpression( (SqmExpression<? extends Number>) fetch );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		setFetchExpression( (SqmExpression<? extends Number>) fetch, fetchClauseType );
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
			for ( SqmFrom<?, ?> sqmTreat : root.getSqmTreats() ) {
				validateFetchOwners( selectedFromSet, sqmTreat );
			}
		}
	}

	private void collectSelectedFromSet(Set<SqmFrom<?, ?>> selectedFromSet, SqmSelectableNode<?> selectableNode) {
		if ( selectableNode instanceof SqmJpaCompoundSelection<?> compoundSelection ) {
			for ( SqmSelectableNode<?> selectionItem : compoundSelection.getSelectionItems() ) {
				collectSelectedFromSet( selectedFromSet, selectionItem );
			}
		}
		else if ( selectableNode instanceof SqmDynamicInstantiation<?> instantiation ) {
			for ( SqmDynamicInstantiationArgument<?> selectionItem : instantiation.getArguments() ) {
				collectSelectedFromSet( selectedFromSet, selectionItem.getSelectableNode() );
			}
		}
		else if ( selectableNode instanceof SqmFrom<?, ?> ) {
			collectSelectedFromSet( selectedFromSet, (SqmFrom<?, ?>) selectableNode );
		}
		else if ( selectableNode instanceof SqmEntityValuedSimplePath<?> path ) {
			if ( CollectionPart.Nature.fromNameExact( path.getReferencedPathSource().getPathName() ) != null
					&& path.getLhs() instanceof SqmFrom<?, ?> ) {
				collectSelectedFromSet( selectedFromSet, (SqmFrom<?, ?>) path.getLhs() );
			}
		}
		else if ( selectableNode instanceof SqmEmbeddedValuedSimplePath<?> path ) {
			assertEmbeddableCollections( path.getNavigablePath(), (EmbeddableDomainType<?>) path.getSqmType() );
		}
	}

	private void collectSelectedFromSet(Set<SqmFrom<?, ?>> selectedFromSet, SqmFrom<?, ?> sqmFrom) {
		selectedFromSet.add( sqmFrom );
		for ( SqmJoin<?, ?> sqmJoin : sqmFrom.getSqmJoins() ) {
			if ( sqmJoin.getReferencedPathSource().getPathType() instanceof EmbeddableDomainType<?> ) {
				collectSelectedFromSet( selectedFromSet, sqmJoin );
			}
		}

		for ( SqmFrom<?, ?> sqmTreat : sqmFrom.getSqmTreats() ) {
			collectSelectedFromSet( selectedFromSet, sqmTreat );
		}
	}

	private void validateFetchOwners(Set<SqmFrom<?, ?>> selectedFromSet, SqmFrom<?, ?> joinContainer) {
		for ( SqmJoin<?, ?> sqmJoin : joinContainer.getSqmJoins() ) {
			if ( sqmJoin instanceof SqmAttributeJoin<?, ?> attributeJoin ) {
				if ( attributeJoin.isFetched() ) {
					assertFetchOwner( selectedFromSet, attributeJoin.getLhs(), sqmJoin );
					// Only need to check the first level
					continue;
				}
			}
			for ( SqmFrom<?, ?> sqmTreat : sqmJoin.getSqmTreats() ) {
				if ( sqmTreat instanceof SqmAttributeJoin<?, ?> attributeJoin ) {
					if ( attributeJoin.isFetched() ) {
						assertFetchOwner( selectedFromSet, attributeJoin.getLhs(), attributeJoin );
						// Only need to check the first level
						continue;
					}
				}
				validateFetchOwners( selectedFromSet, sqmTreat );
			}
			validateFetchOwners( selectedFromSet, sqmJoin );
		}
	}

	private void assertFetchOwner(Set<SqmFrom<?, ?>> selectedFromSet, SqmFrom<?, ?> owner, SqmJoin<?, ?> fetchJoin) {
		if ( !selectedFromSet.contains( owner ) ) {
			throw new SemanticException(
					"Query specified join fetching, but the owner " +
							"of the fetched association was not present in the select list " +
							"[" + fetchJoin.asLoggableText() + "]"
			);
		}
	}

	private void assertEmbeddableCollections(NavigablePath navigablePath, EmbeddableDomainType<?> embeddableType) {
		if ( !embeddableType.getPluralAttributes().isEmpty() ) {
			throw new SemanticException( String.format(
					"Explicit selection of an embeddable containing associated collections is not supported: %s",
					navigablePath
			) );
		}
		else {
			for ( SingularAttribute<?, ?> attribute : embeddableType.getSingularAttributes() ) {
				if ( attribute.getType() instanceof EmbeddableDomainType<?> ) {
					assertEmbeddableCollections( navigablePath, (EmbeddableDomainType<?>) attribute.getType() );
				}
			}
		}
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( selectClause != null ) {
			hql.append( "select " );
			if ( selectClause.isDistinct() ) {
				hql.append( "distinct " );
			}
			final List<SqmSelection<?>> selections = selectClause.getSelections();
			if ( !selections.isEmpty() ) {
				selections.get( 0 ).appendHqlString( hql, context );
				for ( int i = 1; i < selections.size(); i++ ) {
					hql.append( ", " );
					selections.get( i ).appendHqlString( hql, context );
				}
			}
		}
		if ( fromClause != null ) {
			hql.append( " from" );
			fromClause.appendHqlString( hql, context );
		}
		if ( whereClause != null && whereClause.getPredicate() != null ) {
			hql.append( " where " );
			whereClause.getPredicate().appendHqlString( hql, context );
		}
		if ( !groupByClauseExpressions.isEmpty() ) {
			hql.append( " group by " );
			groupByClauseExpressions.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < groupByClauseExpressions.size(); i++ ) {
				hql.append( ", " );
				groupByClauseExpressions.get( i ).appendHqlString( hql, context );
			}
		}
		if ( havingClausePredicate != null ) {
			hql.append( " having " );
			havingClausePredicate.appendHqlString( hql, context );
		}

		super.appendHqlString( hql, context );
	}

	@Internal
	public boolean whereClauseContains(NavigablePath navigablePath, SqmToSqlAstConverter sqlAstConverter) {
		if ( whereClause == null ) {
			return false;
		}
		return isSameOrParent(
				navigablePath,
				sqlAstConverter.resolveMetadata( this, SqmUtil::getWhereClauseNavigablePaths )
		);
	}

	@Internal
	public boolean groupByClauseContains(NavigablePath navigablePath, SqmToSqlAstConverter sqlAstConverter) {
		if ( groupByClauseExpressions.isEmpty() ) {
			return false;
		}
		return isSameOrChildren(
				navigablePath,
				sqlAstConverter.resolveMetadata( this, SqmUtil::getGroupByNavigablePaths )
		);
	}

	@Internal
	public boolean orderByClauseContains(NavigablePath navigablePath, SqmToSqlAstConverter sqlAstConverter) {
		final SqmOrderByClause orderByClause = getOrderByClause();
		if ( orderByClause == null || orderByClause.getSortSpecifications().isEmpty() ) {
			return false;
		}
		return isSameOrChildren(
				navigablePath,
				sqlAstConverter.resolveMetadata( this, SqmUtil::getOrderByNavigablePaths )
		);
	}

	private boolean isSameOrChildren(NavigablePath navigablePath, List<NavigablePath> navigablePaths) {
		for ( NavigablePath path : navigablePaths ) {
			if ( path.isParentOrEqual( navigablePath ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean isSameOrParent(NavigablePath navigablePath, List<NavigablePath> navigablePaths) {
		for ( NavigablePath path : navigablePaths ) {
			if ( navigablePath.isParentOrEqual( path ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SqmQuerySpec<?> that
			&& super.equals( other )
			&& Objects.equals( this.fromClause, that.fromClause )
			&& Objects.equals( this.selectClause, that.selectClause )
			&& Objects.equals( this.whereClause, that.whereClause )
			&& Objects.equals( this.groupByClauseExpressions, that.groupByClauseExpressions )
			&& Objects.equals( this.havingClausePredicate, that.havingClausePredicate );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(),
				fromClause, selectClause, whereClause,
				groupByClauseExpressions, havingClausePredicate );
	}
}
