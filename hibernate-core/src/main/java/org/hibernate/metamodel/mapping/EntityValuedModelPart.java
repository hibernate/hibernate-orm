package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * Entity-valued model part<ul>
 *     <li>{@link jakarta.persistence.ManyToOne}</li>
 *     <li>{@link jakarta.persistence.OneToOne}</li>
 *     <li>entity-valued collection element</li>
 *     <li>entity-valued Map key</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface EntityValuedModelPart extends FetchableContainer {
	/**
	 * The descriptor of the entity that is the type for this part
	 */
	@Nonnull
	EntityMappingType getEntityMappingType();

	@Nullable
	default ModelPart findSubPart(@Nonnull String name) {
		return getEntityMappingType().findSubPart( name, null );
	}

	@Override
	default void forEachSubPart(@Nonnull IndexedConsumer<ModelPart> consumer, @Nullable EntityMappingType treatTarget) {
		getEntityMappingType().forEachSubPart( consumer, treatTarget );
	}

	@Nullable
	@Override
	default ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType targetType) {
		return getEntityMappingType().findSubPart( name, targetType );
	}

	@Override
	default void visitSubParts(@Nonnull Consumer<ModelPart> consumer, @Nullable EntityMappingType targetType) {
		getEntityMappingType().visitSubParts( consumer, targetType );
	}

	@Nonnull
	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	default <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		// creating the domain result should only ever be done for a root return.  otherwise `#generateFetch` should
		// have been used.  so delegating to the entity-descriptor should be fine.
		return getEntityMappingType().createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	default void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		// this is really only valid for root entity returns, not really many-to-ones, etc..  but this should
		// really only ever be called as part of creating a root-return.
		getEntityMappingType().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	default void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection,JdbcMapping> selectionConsumer) {
		// this is really only valid for root entity returns, not really many-to-ones, etc..  but this should
		// really only ever be called as part of creating a root-return.
		getEntityMappingType().applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	default int getJdbcTypeCount() {
		return getEntityMappingType().getJdbcTypeCount();
	}

	@Override
	default int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		return getEntityMappingType().forEachJdbcType( offset, action );
	}

	@Nullable
	@Override
	default Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return getEntityMappingType().disassemble( value, session );
	}

	@Override
	default void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session){
		getEntityMappingType().addToCacheKey( cacheKey, value, session );
	}

	@Override
	default <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return getEntityMappingType().forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	default <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> consumer,
			@Nullable SharedSessionContractImplementor session) {
		return getEntityMappingType().forEachJdbcValue( value, offset, x, y, consumer, session );
	}
}
