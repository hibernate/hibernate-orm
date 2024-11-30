/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.derived;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
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
import org.hibernate.query.sqm.SqmExpressible;
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
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.metamodel.Attribute;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNullElse;

/**
 * @author Christian Beikov
 */
@Incubating
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
			Set<Attribute<?, ?>> attributes,
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
			Set<Attribute<?, ?>> attributes,
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
					selectionIndex,
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

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return modelPartMap.get( name );
	}

	@Override
	public void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		for ( int i = 0; i < modelParts.length; i++ ) {
			consumer.accept( i, modelParts[i] );
		}
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		for ( int i = 0; i < modelParts.length; i++ ) {
			consumer.accept( modelParts[i] );
		}
	}

	@Override
	public JavaType<?> getJavaType() {
		return domainType.getExpressibleJavaType();
	}

	@Override
	public String getPartName() {
		return componentName;
	}

	@Override
	public int getJdbcTypeCount() {
		return existingModelPartContainer.getJdbcTypeCount();
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return this;
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return this;
	}

	@Override
	public EmbeddableRepresentationStrategy getRepresentationStrategy() {
		return existingModelPartContainer.getEmbeddableTypeDescriptor()
				.getRepresentationStrategy();
	}

	@Override
	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return modelParts.length;
	}

	@Override
	public AttributeMapping getAttributeMapping(int position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AttributeMappingsList getAttributeMappings() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forEachAttributeMapping(Consumer<? super AttributeMapping> action) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] getValues(Object instance) {
		return existingModelPartContainer.getEmbeddableTypeDescriptor().getValues( instance );
	}

	@Override
	public Object getValue(Object instance, int position) {
		return existingModelPartContainer.getEmbeddableTypeDescriptor().getValue( instance, position );
	}

	@Override
	public void setValues(Object instance, Object[] resolvedValues) {
		existingModelPartContainer.getEmbeddableTypeDescriptor().setValues( instance, resolvedValues );
	}

	@Override
	public void setValue(Object instance, int position, Object value) {
		existingModelPartContainer.getEmbeddableTypeDescriptor().setValue( instance, position, value );
	}

	@Override
	public int getSelectableIndex(String selectableName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		final List<SelectableMapping> results = new ArrayList<>();
		forEachSelectable( (index, selection) -> results.add( selection ) );
		return results.get( columnIndex );
	}

	@Override
	public Fetchable getFetchable(int position) {
		return (Fetchable) modelParts[position];
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return getSelectable( index ).getJdbcMapping();
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		int span = 0;
		for ( ModelPart mapping : modelParts ) {
			span += mapping.forEachSelectable( offset + span, consumer );
		}
		return span;
	}

	@Override
	public void forEachInsertable(int offset, SelectableConsumer consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forEachUpdatable(int offset, SelectableConsumer consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getContainingTableExpression() {
		return "";
	}

	@Override
	public SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
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

	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return null;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
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
		for ( ModelPart mapping : modelParts ) {
			mapping.applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		for ( ModelPart mapping : modelParts ) {
			mapping.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
		}
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
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

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
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
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
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
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
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
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> consumer,
			SharedSessionContractImplementor session) {
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
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = 0;
		for ( ModelPart attributeMapping : modelParts ) {
			span += attributeMapping.forEachJdbcType( span + offset, action );
		}
		return span;
	}
}
