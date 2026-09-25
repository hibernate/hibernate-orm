package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoinProducer;
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
	@Nonnull
	EmbeddableMappingType getEmbeddableTypeDescriptor();

	@Nonnull
	@Override
	default EmbeddableMappingType getMappedType() {
		return getEmbeddableTypeDescriptor();
	}

	@Nonnull
	@Override
	default JavaType<?> getJavaType() {
		return getEmbeddableTypeDescriptor().getJavaType();
	}

	@Nullable
	@Override
	default ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
		return getEmbeddableTypeDescriptor().findSubPart( name, treatTargetType );
	}

	@Override
	default void forEachSubPart(@Nonnull IndexedConsumer<ModelPart> consumer, @Nullable EntityMappingType treatTarget) {
		getEmbeddableTypeDescriptor().forEachSubPart( consumer, treatTarget );
	}

	@Override
	default void visitSubParts(@Nonnull Consumer<ModelPart> consumer, @Nullable EntityMappingType treatTargetType) {
		getEmbeddableTypeDescriptor().visitSubParts( consumer, treatTargetType );
	}

	@Override
	default int getJdbcTypeCount() {
		return getEmbeddableTypeDescriptor().getJdbcTypeCount();
	}

	@Nonnull
	@Override
	default JdbcMapping getJdbcMapping(int index) {
		return getEmbeddableTypeDescriptor().getJdbcMapping( index );
	}

	@Override
	default int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		return getEmbeddableTypeDescriptor().forEachJdbcType( offset, action );
	}

	@Override
	default <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().forEachJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	default <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	default  <X, Y> int decompose(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().decompose( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	default int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfFetchables();
	}

	@Nonnull
	@Override
	default Fetchable getFetchable(int position) {
		return getEmbeddableTypeDescriptor().getFetchable( position );
	}

	@Override
	default int getSelectableIndex(@Nonnull String selectableName) {
		return getEmbeddableTypeDescriptor().getSelectableIndex( selectableName );
	}

	@Nonnull
	@Override
	default SelectableMapping getSelectable(int columnIndex) {
		return getEmbeddableTypeDescriptor().getSelectable( columnIndex );
	}

	@Override
	default int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		return getEmbeddableTypeDescriptor().forEachSelectable( offset, consumer );
	}

	@Override
	default void forEachInsertable(@Nonnull SelectableConsumer consumer) {
		getEmbeddableTypeDescriptor().forEachInsertable( 0, consumer );
	}

	@Override
	default void forEachUpdatable(@Nonnull SelectableConsumer consumer) {
		getEmbeddableTypeDescriptor().forEachUpdatable( 0, consumer );
	}

	@Override
	default boolean hasPartitionedSelectionMapping() {
		return getEmbeddableTypeDescriptor().hasPartitionedSelectionMapping();
	}

	@Override
	default <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().forEachDisassembledJdbcValue(
				value,
				offset,
				x,
				y,
				valuesConsumer,
				session
		);
	}

	@Nullable
	@Override
	default Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().disassemble( value, session );
	}

	@Override
	default void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		getEmbeddableTypeDescriptor().addToCacheKey( cacheKey, value, session );
	}

	/**
	 * @see org.hibernate.annotations.Parent
	 */
	@Nullable
	default PropertyAccess getParentInjectionAttributePropertyAccess() {
		return null;
	}

	@Nonnull
	SqlTuple toSqlExpression(
			@Nonnull TableGroup tableGroup,
			@Nonnull Clause clause,
			@Nonnull SqmToSqlAstConverter walker,
			@Nonnull SqlAstCreationState sqlAstCreationState);
}
