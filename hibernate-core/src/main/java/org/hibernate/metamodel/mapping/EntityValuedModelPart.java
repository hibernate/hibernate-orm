/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchableContainer;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public interface EntityValuedModelPart extends FetchableContainer {
	EntityMappingType getEntityMappingType();

	@Override
	default ModelPart findSubPart(String name) {
		return getEntityMappingType().findSubPart( name );
	}

	@Override
	default void visitSubParts(Consumer<ModelPart> consumer) {
		getEntityMappingType().visitSubParts( consumer );
	}

	@Override
	default <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// todo (6.0) : this is really only valid for root entity returns, not really many-to-ones, etc..
		return getEntityMappingType().createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	default void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		// todo (6.0) : this is really only valid for root entity returns, not really many-to-ones, etc..
		getEntityMappingType().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	default int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return getEntityMappingType().getJdbcTypeCount( typeConfiguration );
	}

	@Override
	default void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		getEntityMappingType().getJdbcTypeCount( typeConfiguration );
	}

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		return getEntityMappingType().disassemble( value, session );
	}

	@Override
	default void visitDisassembledJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		getEntityMappingType().visitDisassembledJdbcValues( value, clause, valuesConsumer, session );
	}

	@Override
	default void visitJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		getEntityMappingType().visitJdbcValues( value, clause, valuesConsumer, session );
	}
}
