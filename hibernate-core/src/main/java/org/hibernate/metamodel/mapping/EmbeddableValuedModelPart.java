/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * An embedded (embeddable-valued) model part.
 *
 * @see jakarta.persistence.Embedded
 * @see jakarta.persistence.EmbeddedId
 * @see jakarta.persistence.Embeddable
 *
 * @author Steve Ebersole
 */
public interface EmbeddableValuedModelPart extends ModelPart, Fetchable, FetchableContainer, TableGroupJoinProducer {
	EmbeddableMappingType getEmbeddableTypeDescriptor();

	@Override
	default int getJdbcTypeCount() {
		return getEmbeddableTypeDescriptor().getJdbcTypeCount();
	}

	@Override
	default List<JdbcMapping> getJdbcMappings() {
		return getEmbeddableTypeDescriptor().getJdbcMappings();
	}

	@Override
	default int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return getEmbeddableTypeDescriptor().forEachJdbcType( offset, action );
	}

	@Override
	default int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().forEachJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		return getEmbeddableTypeDescriptor().forEachSelectable( offset, consumer );
	}

	@Override
	default int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().forEachDisassembledJdbcValue(
				value,
				clause,
				offset,
				valuesConsumer,
				session
		);
	}

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().disassemble( value, session );
	}

	/**
	 * The main table expression (table name or subselect) that usually contains
	 * most of the columns to which this embedded is mapped.
	 *
	 * @apiNote Hibernate has historically required a composite to be mapped to the same table.
	 */
	String getContainingTableExpression();

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
