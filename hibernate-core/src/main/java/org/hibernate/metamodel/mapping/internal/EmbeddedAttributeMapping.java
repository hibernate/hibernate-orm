/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PropertyBasedMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.AggregateEmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.AggregateEmbeddableResultImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;

import jakarta.annotation.Nullable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.metamodel.mapping.internal.ParentPropertyAccessHelper.parentPropertyAccess;


/**
 * @author Steve Ebersole
 */
public class EmbeddedAttributeMapping
		extends AbstractSingularAttributeMapping
		implements EmbeddableValuedFetchable, Fetchable {
	private final NavigableRole navigableRole;

	private final String tableExpression;
	private final EmbeddableMappingType embeddableMappingType;
	@Nullable private final PropertyAccess parentInjectionAttributePropertyAccess;
	private final boolean selectable;

	public EmbeddedAttributeMapping(
			PropertyAccessorService propertyAccessorService,
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			String tableExpression,
			AttributeMetadata attributeMetadata,
			String parentInjectionAttributeName,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			@Nullable ManagedMappingType declaringType,
			@Nullable PropertyAccess propertyAccess) {
		this(
			name,
			navigableRole,
			stateArrayPosition,
			fetchableIndex,
			tableExpression,
			attributeMetadata,
			parentPropertyAccess( propertyAccessorService, parentInjectionAttributeName, embeddableMappingType ),
			mappedFetchTiming,
			mappedFetchStyle,
			embeddableMappingType,
			declaringType,
			propertyAccess
		);
	}

	public EmbeddedAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			String tableExpression,
			AttributeMetadata attributeMetadata,
			@Nullable PropertyAccess parentInjectionAttributePropertyAccess,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			@Nullable ManagedMappingType declaringType,
			@Nullable PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				fetchableIndex,
				attributeMetadata,
				mappedFetchTiming,
				mappedFetchStyle,
				declaringType,
				propertyAccess
		);
		this.navigableRole = navigableRole;

		this.parentInjectionAttributePropertyAccess = parentInjectionAttributePropertyAccess;
		this.tableExpression = tableExpression;

		this.embeddableMappingType = embeddableMappingType;

		selectable =
				!NavigablePath.IDENTIFIER_MAPPER_PROPERTY.equals( getAttributeName() )
					&& attributeMetadata.isSelectable();
	}

	// Constructor is only used for creating the inverse attribute mapping
	EmbeddedAttributeMapping(
			@Nullable ManagedMappingType keyDeclaringType,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			EmbeddableValuedModelPart inverseModelPart,
			EmbeddableMappingType embeddableTypeDescriptor,
			MappingModelCreationProcess creationProcess) {
		super(
				inverseModelPart.getFetchableName(),
				inverseModelPart.asAttributeMapping() != null
						? inverseModelPart.asAttributeMapping().getStateArrayPosition()
						: -1,
				inverseModelPart.getFetchableKey(),
				inverseModelPart.asAttributeMapping() != null
						? inverseModelPart.asAttributeMapping().getAttributeMetadata()
						: null,
				inverseModelPart.getMappedFetchOptions(),
				keyDeclaringType,
				inverseModelPart instanceof PropertyBasedMapping propertyBasedMapping
						? propertyBasedMapping.getPropertyAccess()
						: null
		);

		navigableRole =
				castNonNull( inverseModelPart.getNavigableRole() ).getParent()
						.append( inverseModelPart.getFetchableName() );

		tableExpression = selectableMappings.getSelectable( 0 ).getContainingTableExpression();
		embeddableMappingType = embeddableTypeDescriptor.createInverseMappingType(
				this,
				declaringTableGroupProducer,
				selectableMappings,
				creationProcess
		);
		parentInjectionAttributePropertyAccess = null;

		if ( NavigablePath.IDENTIFIER_MAPPER_PROPERTY.equals( getAttributeName() ) ) {
			selectable = false;
		}
		else {
			final var attributeMapping = inverseModelPart.asAttributeMapping();
			selectable = attributeMapping == null || attributeMapping.isSelectable();
		}
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getMappedType() {
		return getEmbeddableTypeDescriptor();
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return embeddableMappingType;
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Nullable
	@Override
	public PropertyAccess getParentInjectionAttributePropertyAccess() {
		return parentInjectionAttributePropertyAccess;
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		if ( embeddableMappingType.shouldSelectAggregateMapping() ) {
			return new AggregateEmbeddableResultImpl<>(
					navigablePath,
					this,
					resultVariable,
					creationState
			);
		}
		return new EmbeddableResultImpl<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		embeddableMappingType.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		embeddableMappingType.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( embeddableMappingType.shouldSelectAggregateMapping() ) {
			return new AggregateEmbeddableFetchImpl(
					fetchablePath,
					this,
					fetchParent,
					fetchTiming,
					selected,
					creationState
			);
		}
		return new EmbeddableFetchImpl(
				fetchablePath,
				this,
				fetchParent,
				fetchTiming,
				selected,
				creationState
		);
	}

	@Nonnull
	@Override
	public SqlTuple toSqlExpression(
			@Nonnull TableGroup tableGroup,
			@Nonnull Clause clause,
			@Nonnull SqmToSqlAstConverter walker,
			@Nonnull SqlAstCreationState sqlAstCreationState) {
		if ( embeddableMappingType.getAggregateMapping() != null ) {
			final var selection = embeddableMappingType.getAggregateMapping();
			final var navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
			final var tableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
			return new SqlTuple(
					Collections.singletonList(
							sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
									tableReference,
									selection
							)
					),
					this
			);
		}
		final List<ColumnReference> columnReferences = arrayList( embeddableMappingType.getJdbcTypeCount() );
		final var navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
		final var defaultTableReference = tableGroup.resolveTableReference( navigablePath, this, getContainingTableExpression() );
		getEmbeddableTypeDescriptor().forEachSelectable(
				(columnIndex, selection) -> {
					final var tableReference =
							getContainingTableExpression().equals( selection.getContainingTableExpression() )
									? defaultTableReference
									: tableGroup.resolveTableReference( navigablePath, this, selection.getContainingTableExpression() );
					final var columnReference =
							sqlAstCreationState.getSqlExpressionResolver()
									.resolveSqlExpression( tableReference, selection );

					columnReferences.add( columnReference.getColumnReference() );
				}
		);

		return new SqlTuple( columnReferences, this );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final var joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
		final var tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				null,
				creationState
		);

		return new TableGroupJoin( navigablePath, joinType, tableGroup );
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return new StandardVirtualTableGroup( navigablePath, this, lhs, fetched );
	}

	@Override
	public String getSqlAliasStem() {
		return castNonNull( getAttributeName() );
	}

	@Override
	public String toString() {
		return "EmbeddedAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	@Nonnull
	@Override
	public EmbeddedAttributeMapping asEmbeddedAttributeMapping() {
		return this;
	}

	@Override
	public boolean isEmbeddedAttributeMapping() {
		return true;
	}

	@Override
	public boolean isSelectable() {
		return selectable;
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return tableGroupProducer( castNonNull( getDeclaringType() ) )
				.containsTableReference( tableExpression );
	}

	private static TableGroupProducer tableGroupProducer(ManagedMappingType declaringType) {
		if ( declaringType instanceof TableGroupProducer tableGroupProducer ) {
			return tableGroupProducer;
		}
		else if ( declaringType instanceof EmbeddableMappingType embeddableMappingType ) {
			return embeddableMappingType.getEmbeddedValueMapping();
		}
		else {
			throw new AssertionFailure( "Unexpected declaring type" );
		}
	}

	@Override
	public int compare(@Nullable Object value1, @Nullable Object value2) {
		return embeddableMappingType.compare( value1, value2 );
	}
}
