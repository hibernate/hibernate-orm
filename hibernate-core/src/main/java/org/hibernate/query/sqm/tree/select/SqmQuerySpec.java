/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmNode;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
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

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Defines the commonality between a root query and a subquery.
 *
 * @author Steve Ebersole
 */
public class SqmQuerySpec<T> extends SqmQueryPart<T>
		implements SqmNode, SqmFromClauseContainer, SqmWhereClauseContainer, JpaQueryStructure<T> {
	private SqmFromClause fromClause;
	private SqmSelectClause selectClause;
	private @Nullable SqmWhereClause whereClause;

	private boolean hasPositionalGroupItem;
	private List<SqmExpression<?>> groupByClauseExpressions = Collections.emptyList();
	private @Nullable SqmPredicate havingClausePredicate;

	public SqmQuerySpec(NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		// Enforce non-nullness of the fromClause
		this.fromClause = new SqmFromClause();
		this.selectClause = new SqmSelectClause( false, nodeBuilder );
	}

	public SqmQuerySpec(SqmQuerySpec<T> original, SqmCopyContext context) {
		super( original, context );
		this.fromClause = original.fromClause.copy( context );
		this.selectClause = original.selectClause.copy( context );
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
		querySpec.selectClause = selectClause.copy( context );
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
		this.fromClause = fromClause == null ? new SqmFromClause() : fromClause;
	}

	public boolean producesUniqueResults() {
		if ( fromClause.getRoots().size() != 1 ) {
			return false;
		}
		final SqmRoot<?> sqmRoot = fromClause.getRoots().get( 0 );
		final List<SqmSelection<?>> selections = selectClause.getSelections();
		if ( selections.size() != 1 || selections.get( 0 ).getSelectableNode() != sqmRoot ) {
			// If we select anything but the query root, let's be pessimistic about unique results
			return false;
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
	public @Nullable SqmWhereClause getWhereClause() {
		return whereClause;
	}

	public void setWhereClause(@Nullable SqmWhereClause whereClause) {
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

	public @Nullable SqmPredicate getHavingClausePredicate() {
		return havingClausePredicate;
	}

	public void setHavingClausePredicate(@Nullable SqmPredicate havingClausePredicate) {
		this.havingClausePredicate = havingClausePredicate;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public boolean isDistinct() {
		return getSelectClause().isDistinct();
	}

	@Override
	public SqmQuerySpec<T> setDistinct(boolean distinct) {
		getSelectClause().makeDistinct( distinct );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaSelection<T> getSelection() {
		return (JpaSelection<T>) getSelectClause().resolveJpaSelection();
	}

	@Override
	public SqmQuerySpec<T> setSelection(JpaSelection<T> selection) {
		final SqmSelectClause selectClause = getSelectClause();
		// NOTE : this call comes from JPA which inherently supports just a
		// single (possibly "compound") selection.
		// We have this special case where we return the SqmSelectClause itself if it doesn't have exactly 1 item
		if ( selection instanceof SqmSelectClause sqmSelectClause ) {
			if ( selection != selectClause ) {
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
	public @Nullable SqmPredicate getRestriction() {
		final SqmWhereClause whereClause = getWhereClause();
		return whereClause == null ? null : whereClause.getPredicate();
	}

	@Override
	public SqmQuerySpec<T> setRestriction(@Nullable JpaPredicate restriction) {
		if ( restriction == null ) {
			setWhereClause( null );
		}
		else {
		SqmWhereClause whereClause = getWhereClause();
		if ( whereClause == null ) {
			setWhereClause( whereClause = new SqmWhereClause( nodeBuilder() ) );
		}
		whereClause.setPredicate( (SqmPredicate) restriction );
		}
		return this;
	}

	@Override
	public SqmQuerySpec<T> setRestriction(@Nullable Expression<Boolean> restriction) {
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
	public SqmQuerySpec<T> setRestriction(Predicate @Nullable... restrictions) {
		if ( restrictions == null ) {
			throw new IllegalArgumentException( "The predicate array cannot be null" );
		}
		else if ( restrictions.length == 0 ) {
			setWhereClause( null );
		}
		else {
			final SqmWhereClause whereClause = resetWhereClause();
			for ( Predicate restriction : restrictions ) {
				whereClause.applyPredicate( (SqmPredicate) restriction );
			}
		}
		return this;
	}

	@Override
	public SqmQuerySpec<T> setRestriction(List<Predicate> restrictions) {
		if ( restrictions == null ) {
			throw new IllegalArgumentException( "The predicate list cannot be null" );
		}
		else if ( restrictions.isEmpty() ) {
			setWhereClause( null );
		}
		else {
			final SqmWhereClause whereClause = resetWhereClause();
			for ( Predicate restriction : restrictions ) {
				whereClause.applyPredicate( (SqmPredicate) restriction );
			}
		}
		return this;
	}

	private SqmWhereClause resetWhereClause() {
		final SqmWhereClause whereClause = getWhereClause();
		if ( whereClause == null ) {
			final SqmWhereClause newWhereClause = new SqmWhereClause( nodeBuilder() );
			setWhereClause( newWhereClause );
			return newWhereClause;
		}
		else {
			whereClause.setPredicate( null );
			return whereClause;
		}
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
	public @Nullable SqmPredicate getGroupRestriction() {
		return havingClausePredicate;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(@Nullable JpaPredicate restriction) {
		havingClausePredicate = (SqmPredicate) restriction;
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(@Nullable Expression<Boolean> restriction) {
		havingClausePredicate = restriction == null ? null : nodeBuilder().wrap( restriction );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(Predicate @Nullable... restrictions) {
		havingClausePredicate = restrictions == null ? null : nodeBuilder().wrap( restrictions );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setGroupRestriction(List<Predicate> restrictions) {
		havingClausePredicate = nodeBuilder().wrap( restrictions );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		super.setSortSpecifications( sortSpecifications );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setOffset(@Nullable JpaExpression<? extends Number> offset) {
		setOffsetExpression( (SqmExpression<? extends Number>) offset );
		return this;
	}

	@Override
	public SqmQuerySpec<T> setFetch(@Nullable JpaExpression<? extends Number> fetch) {
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
		if ( selectClause.getSelections().isEmpty() ) {
			if ( CollectionHelper.isEmpty( roots ) ) {
				throw new SemanticException( "No query roots were specified" );
			}
			else {
				selectedFromSet = Collections.singleton( roots.get( 0 ) );
			}
		}
		else {
			selectedFromSet = Collections.newSetFromMap( new IdentityHashMap<>( selectClause.getSelections().size() ) );
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
				collectSelectedFromSet( selectedFromSet, (SqmFrom<?, ?>) castNonNull( path.getLhs() ) );
			}
		}
		else if ( selectableNode instanceof SqmEmbeddedValuedSimplePath<?> path ) {
			final SqmDomainType<?> sqmType = path.getSqmType();
			if ( sqmType != null ) {
				assertEmbeddableCollections( path.getNavigablePath(), (EmbeddableDomainType<?>) sqmType );
			}
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
		final List<SqmSelection<?>> selections = selectClause.getSelections();
		if ( !selections.isEmpty() ) {
			hql.append( "select " );
			if ( selectClause.isDistinct() ) {
				hql.append( "distinct " );
			}
			selections.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < selections.size(); i++ ) {
				hql.append( ", " );
				selections.get( i ).appendHqlString( hql, context );
			}
		}
		hql.append( " from" );
		fromClause.appendHqlString( hql, context );
		final SqmPredicate wherePredicate = whereClause == null ? null : whereClause.getPredicate();
		if ( wherePredicate != null ) {
			hql.append( " where " );
			wherePredicate.appendHqlString( hql, context );
		}
		if ( !groupByClauseExpressions.isEmpty() ) {
			hql.append( " group by " );
			groupByClauseExpressions.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < groupByClauseExpressions.size(); i++ ) {
				hql.append( ", " );
				groupByClauseExpressions.get( i ).appendHqlString( hql, context );
			}
		}
		final SqmPredicate havingClausePredicate = this.havingClausePredicate;
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
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmQuerySpec<?> that
			&& super.equals( object )
			&& fromClause.equals( that.fromClause )
			&& selectClause.equals( that.selectClause )
			&& Objects.equals( whereClause, that.whereClause )
			&& Objects.equals( groupByClauseExpressions, that.groupByClauseExpressions )
			&& Objects.equals( havingClausePredicate, that.havingClausePredicate );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + fromClause.hashCode();
		result = 31 * result + selectClause.hashCode();
		result = 31 * result + Objects.hashCode( whereClause );
		result = 31 * result + Objects.hashCode( groupByClauseExpressions );
		result = 31 * result + Objects.hashCode( havingClausePredicate );
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmQuerySpec<?> that
			&& super.isCompatible( object )
			&& fromClause.isCompatible( that.fromClause )
			&& selectClause.isCompatible( that.selectClause )
			&& SqmCacheable.areCompatible( whereClause, that.whereClause )
			&& SqmCacheable.areCompatible( groupByClauseExpressions, that.groupByClauseExpressions )
			&& SqmCacheable.areCompatible( havingClausePredicate, that.havingClausePredicate );
	}

	@Override
	public int cacheHashCode() {
		int result = super.cacheHashCode();
		result = 31 * result + fromClause.cacheHashCode();
		result = 31 * result + selectClause.cacheHashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( whereClause );
		result = 31 * result + SqmCacheable.cacheHashCode( groupByClauseExpressions );
		result = 31 * result + SqmCacheable.cacheHashCode( havingClausePredicate );
		return result;
	}
}
