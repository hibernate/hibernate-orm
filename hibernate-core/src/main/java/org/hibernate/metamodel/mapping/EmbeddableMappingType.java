/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.EmbeddableRepresentationStrategy;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * Describes an embeddable - the actual type
 *
 * @see EmbeddableValuedModelPart
 */
public interface EmbeddableMappingType extends ManagedMappingType, SelectableMappings {
	EmbeddableValuedModelPart getEmbeddedValueMapping();

	EmbeddableRepresentationStrategy getRepresentationStrategy();

	boolean isCreateEmptyCompositesEnabled();

	EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess);

	@Override
	default int forEachSelectable(SelectableConsumer consumer) {
		return ManagedMappingType.super.forEachSelectable( consumer );
	}

	@Override
	int forEachSelectable(int offset, SelectableConsumer consumer);

	@Override
	int getJdbcTypeCount();

	@Override
	List<JdbcMapping> getJdbcMappings();

	@Override
	int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action);

	// Make this abstract again to ensure subclasses implement this method
	@Override
	<T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState);

	@Override
	default void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		visitAttributeMappings(
				attributeMapping -> attributeMapping.applySqlSelections( navigablePath, tableGroup, creationState )
		);
	}

	@Override
	default void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		visitAttributeMappings(
				attributeMapping ->
						attributeMapping.applySqlSelections(
								navigablePath,
								tableGroup,
								creationState,
								selectionConsumer
						)
		);
	}
}
