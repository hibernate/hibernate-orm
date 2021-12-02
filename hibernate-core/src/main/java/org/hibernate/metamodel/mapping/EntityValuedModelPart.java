/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * @author Steve Ebersole
 */
public interface EntityValuedModelPart extends FetchableContainer {
	EntityMappingType getEntityMappingType();

	default ModelPart findSubPart(String name) {
		return getEntityMappingType().findSubPart( name, null );
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
	default int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return getEntityMappingType().forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	default int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer consumer,
			SharedSessionContractImplementor session) {
		return getEntityMappingType().forEachJdbcValue( value, clause, offset, consumer, session );
	}
}
