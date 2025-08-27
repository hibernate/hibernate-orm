/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PropertyBasedMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
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

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNullElse;

/**
 * @author Steve Ebersole
 */
public class EmbeddedAttributeMapping
		extends AbstractSingularAttributeMapping
		implements EmbeddableValuedFetchable, Fetchable {
	private final NavigableRole navigableRole;

	private final String tableExpression;
	private final EmbeddableMappingType embeddableMappingType;
	private final PropertyAccess parentInjectionAttributePropertyAccess;
	private final boolean selectable;

	public EmbeddedAttributeMapping(
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
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		this(
			name,
			navigableRole,
			stateArrayPosition,
			fetchableIndex,
			tableExpression,
			attributeMetadata,
			getPropertyAccess( parentInjectionAttributeName, embeddableMappingType ),
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
			PropertyAccess parentInjectionAttributePropertyAccess,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
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

		if ( getAttributeName().equals( NavigablePath.IDENTIFIER_MAPPER_PROPERTY ) ) {
			selectable = false;
		}
		else {
			selectable = attributeMetadata.isSelectable();
		}
	}

	// Constructor is only used for creating the inverse attribute mapping
	EmbeddedAttributeMapping(
			ManagedMappingType keyDeclaringType,
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

		this.navigableRole = inverseModelPart.getNavigableRole().getParent().append( inverseModelPart.getFetchableName() );

		this.tableExpression = selectableMappings.getSelectable( 0 ).getContainingTableExpression();
		this.embeddableMappingType = embeddableTypeDescriptor.createInverseMappingType(
				this,
				declaringTableGroupProducer,
				selectableMappings,
				creationProcess
		);
		this.parentInjectionAttributePropertyAccess = null;

		if ( getAttributeName().equals( NavigablePath.IDENTIFIER_MAPPER_PROPERTY ) ) {
			selectable = false;
		}
		else {
			AttributeMapping attributeMapping = inverseModelPart.asAttributeMapping();
			if ( attributeMapping != null ) {
				selectable = attributeMapping.isSelectable();
			}
			else {
				selectable = true;
			}
		}
	}

	@Override
	public EmbeddableMappingType getMappedType() {
		return getEmbeddableTypeDescriptor();
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return embeddableMappingType;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public PropertyAccess getParentInjectionAttributePropertyAccess() {
		return parentInjectionAttributePropertyAccess;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
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
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		embeddableMappingType.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		embeddableMappingType.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

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

	@Override
	public SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		if ( embeddableMappingType.getAggregateMapping() != null ) {
			final SelectableMapping selection = embeddableMappingType.getAggregateMapping();
			final NavigablePath navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
			final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
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
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( embeddableMappingType.getJdbcTypeCount() );
		final NavigablePath navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
		final TableReference defaultTableReference = tableGroup.resolveTableReference( navigablePath, this, getContainingTableExpression() );
		getEmbeddableTypeDescriptor().forEachSelectable(
				(columnIndex, selection) -> {
					final TableReference tableReference = getContainingTableExpression().equals( selection.getContainingTableExpression() )
							? defaultTableReference
							: tableGroup.resolveTableReference( navigablePath, this, selection.getContainingTableExpression() );
					final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
							tableReference,
							selection
					);

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
		final SqlAstJoinType joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
		final TableGroup tableGroup = createRootTableGroupJoin(
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
		return getAttributeName();
	}

	@Override
	public String toString() {
		return "EmbeddedAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	private static PropertyAccess getPropertyAccess(
			String parentInjectionAttributeName,
			EmbeddableMappingType embeddableMappingType) {
		final PropertyAccess parentInjectionAttributePropertyAccess;
		if ( parentInjectionAttributeName != null ) {
			parentInjectionAttributePropertyAccess = PropertyAccessStrategyBasicImpl.INSTANCE.buildPropertyAccess(
					embeddableMappingType.getMappedJavaType().getJavaTypeClass(),
					parentInjectionAttributeName,
					true );
		}
		else {
			parentInjectionAttributePropertyAccess = null;
		}
		return parentInjectionAttributePropertyAccess;
	}

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
		final ManagedMappingType declaringType = getDeclaringType();
		final TableGroupProducer producer;
		if ( declaringType instanceof TableGroupProducer tableGroupProducer ) {
			producer = tableGroupProducer;
		}
		else if ( declaringType instanceof EmbeddableMappingType embeddableMappingType ) {
			producer = embeddableMappingType.getEmbeddedValueMapping();
		}
		else {
			throw new AssertionFailure( "Unexpected declaring type" );
		}
		return producer.containsTableReference( tableExpression );
	}

	@Override
	public int compare(Object value1, Object value2) {
		return embeddableMappingType.compare( value1, value2 );
	}
}
