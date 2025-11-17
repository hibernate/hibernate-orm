/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Describes the mapping of an embeddable (composite).
 *
 * @see jakarta.persistence.Embedded
 * @see jakarta.persistence.EmbeddedId
 * @see jakarta.persistence.Embeddable
 */
public interface EmbeddableValuedModelPart extends ValuedModelPart, Fetchable, FetchableContainer, TableGroupJoinProducer {
	EmbeddableMappingType getEmbeddableTypeDescriptor();

	@Override
	default EmbeddableMappingType getMappedType() {
		return getEmbeddableTypeDescriptor();
	}

	@Override
	default JavaType<?> getJavaType() {
		return getEmbeddableTypeDescriptor().getJavaType();
	}

	@Override
	default ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return getEmbeddableTypeDescriptor().findSubPart( name, treatTargetType );
	}

	@Override
	default void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		getEmbeddableTypeDescriptor().forEachSubPart( consumer, treatTarget );
	}

	@Override
	default void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		getEmbeddableTypeDescriptor().visitSubParts( consumer, treatTargetType );
	}

	@Override
	default int getJdbcTypeCount() {
		return getEmbeddableTypeDescriptor().getJdbcTypeCount();
	}

	@Override
	default JdbcMapping getJdbcMapping(int index) {
		return getEmbeddableTypeDescriptor().getJdbcMapping( index );
	}

	@Override
	default int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return getEmbeddableTypeDescriptor().forEachJdbcType( offset, action );
	}

	@Override
	default <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().forEachJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	default <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	default  <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().decompose( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	default int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfFetchables();
	}

	@Override
	default Fetchable getFetchable(int position) {
		return getEmbeddableTypeDescriptor().getFetchable( position );
	}

	@Override
	default int getSelectableIndex(String selectableName) {
		return getEmbeddableTypeDescriptor().getSelectableIndex( selectableName );
	}

	@Override
	default SelectableMapping getSelectable(int columnIndex) {
		return getEmbeddableTypeDescriptor().getSelectable( columnIndex );
	}

	@Override
	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		return getEmbeddableTypeDescriptor().forEachSelectable( offset, consumer );
	}

	@Override
	default void forEachInsertable(SelectableConsumer consumer) {
		getEmbeddableTypeDescriptor().forEachInsertable( 0, consumer );
	}

	@Override
	default void forEachUpdatable(SelectableConsumer consumer) {
		getEmbeddableTypeDescriptor().forEachUpdatable( 0, consumer );
	}

	@Override
	default boolean hasPartitionedSelectionMapping() {
		return getEmbeddableTypeDescriptor().hasPartitionedSelectionMapping();
	}

	@Override
	default <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().forEachDisassembledJdbcValue(
				value,
				offset,
				x,
				y,
				valuesConsumer,
				session
		);
	}

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().disassemble( value, session );
	}

	@Override
	default void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		getEmbeddableTypeDescriptor().addToCacheKey( cacheKey, value, session );
	}

	/**
	 * @see org.hibernate.annotations.Parent
	 */
	default PropertyAccess getParentInjectionAttributePropertyAccess() {
		return null;
	}

	SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState);
}
