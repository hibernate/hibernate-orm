/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
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
	EntityMappingType getEntityMappingType();

	default ModelPart findSubPart(String name) {
		return getEntityMappingType().findSubPart( name, null );
	}

	@Override
	default void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		getEntityMappingType().forEachSubPart( consumer, treatTarget );
	}

	@Override
	default ModelPart findSubPart(String name, EntityMappingType targetType) {
		return getEntityMappingType().findSubPart( name, targetType );
	}

	@Override
	default void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType targetType) {
		getEntityMappingType().visitSubParts( consumer, targetType );
	}

	@Override
	default <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// creating the domain result should only ever be done for a root return.  otherwise `#generateFetch` should
		// have been used.  so delegating to the entity-descriptor should be fine.
		return getEntityMappingType().createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	default void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		// this is really only valid for root entity returns, not really many-to-ones, etc..  but this should
		// really only ever be called as part of creating a root-return.
		getEntityMappingType().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	default void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection,JdbcMapping> selectionConsumer) {
		// this is really only valid for root entity returns, not really many-to-ones, etc..  but this should
		// really only ever be called as part of creating a root-return.
		getEntityMappingType().applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	default int getJdbcTypeCount() {
		return getEntityMappingType().getJdbcTypeCount();
	}

	@Override
	default int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return getEntityMappingType().forEachJdbcType( offset, action );
	}

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		return getEntityMappingType().disassemble( value, session );
	}

	@Override
	default void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session){
		getEntityMappingType().addToCacheKey( cacheKey, value, session );
	}

	@Override
	default <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return getEntityMappingType().forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	default <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> consumer,
			SharedSessionContractImplementor session) {
		return getEntityMappingType().forEachJdbcValue( value, offset, x, y, consumer, session );
	}
}
