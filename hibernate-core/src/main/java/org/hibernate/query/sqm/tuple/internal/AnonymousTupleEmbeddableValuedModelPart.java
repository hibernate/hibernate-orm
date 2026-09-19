/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.query.sqm.spi.SqmExpressible;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.metamodel.Attribute;
import jakarta.annotation.Nullable;

import static java.util.Objects.requireNonNullElse;

/**
 * @author Christian Beikov
 */
public class AnonymousTupleEmbeddableValuedModelPart implements EmbeddableValuedModelPart, EmbeddableMappingType {

	private static final FetchOptions FETCH_OPTIONS = FetchOptions.valueOf( FetchTiming.IMMEDIATE, FetchStyle.JOIN );

	private final Map<String, ModelPart> modelPartMap;
	private final ModelPart[] modelParts;
	private final DomainType<?> domainType;
	private final String componentName;
	private final EmbeddableValuedModelPart existingModelPartContainer;
	private final int fetchableIndex;

	public AnonymousTupleEmbeddableValuedModelPart(
			SqmExpressible<?> sqmExpressible,
			SqlTypedMapping[] sqlTypedMappings,
			int selectionIndex,
			String selectionExpression,
			Set<String> compatibleTableExpressions,
			Set<? extends Attribute<?, ?>> attributes,
			DomainType<?> domainType,
			String componentName,
			EmbeddableValuedModelPart existingModelPartContainer,
			int fetchableIndex) {
		this.modelPartMap = createModelParts(
				sqmExpressible,
				sqlTypedMappings,
				selectionIndex,
				selectionExpression,
				compatibleTableExpressions,
				attributes,
				existingModelPartContainer
		);
		this.modelParts = modelPartMap.values().toArray( new ModelPart[0] );
		this.domainType = domainType;
		this.componentName = componentName;
		this.existingModelPartContainer = existingModelPartContainer;
		this.fetchableIndex = fetchableIndex;
	}

	private Map<String, ModelPart> createModelParts(
			SqmExpressible<?> sqmExpressible,
			SqlTypedMapping[] sqlTypedMappings,
			int selectionIndex,
			String selectionExpression,
			Set<String> compatibleTableExpressions,
			Set<? extends Attribute<?, ?>> attributes,
			EmbeddableValuedModelPart modelPartContainer) {
		final Map<String, ModelPart> modelParts = CollectionHelper.linkedMapOfSize( attributes.size() );
		int index = 0;
		for ( Attribute<?, ?> attribute : attributes ) {
			if ( !( attribute instanceof SingularPersistentAttribute<?, ?> ) ) {
				throw new IllegalArgumentException( "Only embeddables without collections are supported!" );
			}
			final DomainType<?> attributeType = ( (SingularPersistentAttribute<?, ?>) attribute ).getType();
			final ModelPart modelPart = AnonymousTupleTableGroupProducer.createModelPart(
					this,
					sqmExpressible,
					attributeType,
					sqlTypedMappings,
					selectionIndex + index,
					selectionExpression + "_" + attribute.getName(),
					attribute.getName(),
					modelPartContainer.findSubPart( attribute.getName(), null ),
					compatibleTableExpressions,
					index++
			);
			modelParts.put( modelPart.getPartName(), modelPart );
		}
		return modelParts;
	}

	@Nullable
	@Override
	public ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
		return modelPartMap.get( name );
	}

	@Override
	public void forEachSubPart(@Nonnull IndexedConsumer<ModelPart> consumer, @Nullable EntityMappingType treatTarget) {
		for ( int i = 0; i < modelParts.length; i++ ) {
			consumer.accept( i, modelParts[i] );
		}
	}

	@Override
	public void visitSubParts(@Nonnull Consumer<ModelPart> consumer, @Nullable EntityMappingType treatTargetType) {
		for ( int i = 0; i < modelParts.length; i++ ) {
			consumer.accept( modelParts[i] );
		}
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return domainType.getExpressibleJavaType();
	}

	@Nonnull
	@Override
	public String getPartName() {
		return componentName;
	}

	@Override
	public int getJdbcTypeCount() {
		return existingModelPartContainer.getJdbcTypeCount();
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return this;
	}

	@Nonnull
	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return this;
	}

	@Nonnull
	@Override
	public EmbeddableRepresentationStrategy getRepresentationStrategy() {
		return existingModelPartContainer.getEmbeddableTypeDescriptor()
				.getRepresentationStrategy();
	}

	@Nonnull
	@Override
	public EmbeddableMappingType createInverseMappingType(
			@Nonnull EmbeddedAttributeMapping valueMapping,
			@Nonnull TableGroupProducer declaringTableGroupProducer,
			@Nonnull SelectableMappings selectableMappings,
			@Nonnull MappingModelCreationProcess creationProcess) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return modelParts.length;
	}

	@Nonnull
	@Override
	public AttributeMapping getAttributeMapping(int position) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public AttributeMappingsList getAttributeMappings() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forEachAttributeMapping(@Nonnull Consumer<? super AttributeMapping> action) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X, Y> int decompose(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public Object[] getValues(@Nonnull Object instance) {
		return existingModelPartContainer.getEmbeddableTypeDescriptor().getValues( instance );
	}

	@Nullable
	@Override
	public Object getValue(@Nonnull Object instance, int position) {
		return existingModelPartContainer.getEmbeddableTypeDescriptor().getValue( instance, position );
	}

	@Override
	public void setValues(@Nonnull Object instance, @Nonnull Object[] resolvedValues) {
		existingModelPartContainer.getEmbeddableTypeDescriptor().setValues( instance, resolvedValues );
	}

	@Override
	public void setValue(@Nonnull Object instance, int position, @Nullable Object value) {
		existingModelPartContainer.getEmbeddableTypeDescriptor().setValue( instance, position, value );
	}

	@Override
	public int getSelectableIndex(@Nonnull String selectableName) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		final List<SelectableMapping> results = new ArrayList<>();
		forEachSelectable( (index, selection) -> results.add( selection ) );
		return results.get( columnIndex );
	}

	@Nonnull
	@Override
	public Fetchable getFetchable(int position) {
		return (Fetchable) modelParts[position];
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return getSelectable( index ).getJdbcMapping();
	}

	@Override
	public int forEachSelectable(@Nonnull SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		int span = 0;
		for ( ModelPart mapping : modelParts ) {
			span += mapping.forEachSelectable( offset + span, consumer );
		}
		return span;
	}

	@Override
	public void forEachInsertable(int offset, @Nonnull SelectableConsumer consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forEachUpdatable(int offset, @Nonnull SelectableConsumer consumer) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return "";
	}

	@Nonnull
	@Override
	public SqlTuple toSqlExpression(
			@Nonnull TableGroup tableGroup,
			@Nonnull Clause clause,
			@Nonnull SqmToSqlAstConverter walker,
			@Nonnull SqlAstCreationState sqlAstCreationState) {
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( getJdbcTypeCount() );
		final NavigablePath navigablePath = tableGroup.getNavigablePath().append( componentName );
		final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, this, getContainingTableExpression() );
		for ( ModelPart modelPart : modelParts ) {
			modelPart.forEachSelectable(
					(columnIndex, selection) -> {
						final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver()
								.resolveSqlExpression( tableReference, selection );

						columnReferences.add( columnReference.getColumnReference() );
					}
			);
		}

		return new SqlTuple( columnReferences, this );
	}

	@Nonnull
	@Override
	public JavaType<?> getMappedJavaType() {
		return existingModelPartContainer.getJavaType();
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.INNER;
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return predicate == null;
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
		return getPartName();
	}

	@Override
	public String getFetchableName() {
		return getPartName();
	}

	@Override
	public int getFetchableKey() {
		return fetchableIndex;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return FETCH_OPTIONS;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException( "AnonymousTupleEmbeddableValuedModelPart is not fetchable" );
	}

	@Override
	public int getNumberOfFetchables() {
		return modelParts.length;
	}

	@Nullable
	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Nullable
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return null;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
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
		for ( ModelPart mapping : modelParts ) {
			mapping.applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		for ( ModelPart mapping : modelParts ) {
			mapping.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
		}
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		int span = 0;
		if ( domainValue == null ) {
			for ( ModelPart mapping : modelParts ) {
				span += mapping.breakDownJdbcValues( null, offset + span, x, y, valueConsumer, session );
			}
		}
		else {
			final Object[] values = (Object[]) domainValue;
			assert values.length == modelParts.length;
			for ( int i = 0; i < modelParts.length; i++ ) {
				final Object attributeValue = values[i];
				span += modelParts[i].breakDownJdbcValues( attributeValue, offset + span, x, y, valueConsumer, session );
			}
		}
		return span;
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		final Object[] values = (Object[]) value;
		final Object[] result = new Object[ modelParts.length ];
		for ( int i = 0; i < modelParts.length; i++ ) {
			Object o = values[i];
			result[i] = modelParts[i].disassemble( o, session );
		}

		return result;
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		if ( value == null ) {
			for ( ModelPart mapping : modelParts ) {
				mapping.addToCacheKey( cacheKey, null, session );
			}
		}
		else {
			final Object[] values = (Object[]) value;
			int i = 0;
			for ( ModelPart mapping : modelParts ) {
				mapping.addToCacheKey( cacheKey, values[i], session );
				i++;
			}
		}
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		int span = 0;
		if ( value == null ) {
			for ( ModelPart mapping : modelParts ) {
				span += mapping.forEachDisassembledJdbcValue( null, span + offset, x, y, valuesConsumer, session );
			}
		}
		else {
			final Object[] values = (Object[]) value;
			for ( int i = 0; i < modelParts.length; i++ ) {
				span += modelParts[i].forEachDisassembledJdbcValue( values[i], span + offset, x, y, valuesConsumer, session );
			}
		}
		return span;
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> consumer,
			@Nullable SharedSessionContractImplementor session) {
		int span = 0;
		if ( value == null ) {
			for ( ModelPart mapping : modelParts ) {
				span += mapping.forEachJdbcValue( null, span + offset, x, y, consumer, session );
			}
		}
		else {
			final Object[] values = (Object[]) value;
			for ( int i = 0; i < modelParts.length; i++ ) {
				final Object o = values[i];
				span += modelParts[i].forEachJdbcValue( o, span + offset, x, y, consumer, session );
			}
		}
		return span;
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		int span = 0;
		for ( ModelPart attributeMapping : modelParts ) {
			span += attributeMapping.forEachJdbcType( span + offset, action );
		}
		return span;
	}
}
