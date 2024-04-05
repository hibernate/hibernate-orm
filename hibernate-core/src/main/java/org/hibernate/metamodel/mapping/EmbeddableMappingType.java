/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.BiConsumer;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * Describes an embeddable - the actual type
 *
 * @implNote Even though this represents the embeddable class, one is created for
 * each embedded usage.  This is done to help
 *
 * @see EmbeddableValuedModelPart
 */
public interface EmbeddableMappingType extends ManagedMappingType, SelectableMappings {
	EmbeddableValuedModelPart getEmbeddedValueMapping();

	EmbeddableRepresentationStrategy getRepresentationStrategy();

	boolean isCreateEmptyCompositesEnabled();

	/**
	 * Returns the {@linkplain EmbeddableDiscriminatorMapping discriminator mapping}
	 * if this discriminator type is polymorphic, {@code null} otherwise.
	 */
	default EmbeddableDiscriminatorMapping getDiscriminatorMapping() {
		return null;
	}

	/**
	 * Returns {@code true} if this embeddable mapping type defines a
	 * discriminator-based inheritance hierarchy, {@code false} otherwise.
	 */
	default boolean isPolymorphic() {
		return getDiscriminatorMapping() != null;
	}

	/**
	 * Returns {@code true} if the provided embeddable class contains the
	 * specified attribute mapping, {@code false} otherwise.
	 * @implNote This method always returns {@code true} for non-polymorphic embeddable types
	 *
	 * @param embeddableClassName the embeddable subclass in which the attribute must be declared
	 * @param attributeMapping the attribute to check
	 */
	default boolean declaresAttribute(String embeddableClassName, AttributeMapping attributeMapping) {
		return true;
	}

	default SelectableMapping getAggregateMapping() {
		return null;
	}

	default boolean shouldSelectAggregateMapping() {
		return getAggregateMapping() != null;
	}

	@Override
	default EmbeddableMappingType getPartMappingType() {
		return this;
	}

	default boolean shouldMutateAggregateMapping() {
		// For insert and update we always want to mutate the whole aggregate
		return getAggregateMapping() != null;
	}

	default boolean shouldBindAggregateMapping() {
		return getAggregateMapping() != null;
	}

	@Override
	default boolean anyRequiresAggregateColumnWriter() {
		return requiresAggregateColumnWriter() || ManagedMappingType.super.anyRequiresAggregateColumnWriter();
	}

	default boolean requiresAggregateColumnWriter() {
		final SelectableMapping aggregateMapping = getAggregateMapping();
		if ( aggregateMapping == null ) {
			return false;
		}
		// Cache this maybe?
		final int aggregateSqlTypeCode = aggregateMapping.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		return findContainingEntityMapping().getEntityPersister().getFactory()
				.getJdbcServices()
				.getDialect()
				.getAggregateSupport()
				.requiresAggregateCustomWriteExpressionRenderer( aggregateSqlTypeCode );
	}

	/**
	 * Different from {@link #getJdbcTypeCount()} as this will treat an aggregate as a single element.
	 */
	default int getJdbcValueCount() {
		final int numberOfAttributeMappings = getNumberOfAttributeMappings();
		int count = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			final MappingType mappedType = attributeMapping.getMappedType();
			if ( mappedType instanceof EmbeddableMappingType
					&& ( (EmbeddableMappingType) mappedType ).getAggregateMapping() != null ) {
				count++;
			}
			else {
				count += attributeMapping.getJdbcTypeCount();
			}
		}
		return count;
	}

	default SelectableMapping getJdbcValueSelectable(int columnIndex) {
		final int numberOfAttributeMappings = getNumberOfAttributeMappings();
		int count = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			final MappingType mappedType = attributeMapping.getMappedType();
			if ( mappedType instanceof EmbeddableMappingType ) {
				final EmbeddableMappingType embeddableMappingType = (EmbeddableMappingType) mappedType;
				final SelectableMapping aggregateMapping = embeddableMappingType.getAggregateMapping();
				if ( aggregateMapping == null ) {
					final SelectableMapping subSelectable = embeddableMappingType.getJdbcValueSelectable( columnIndex - count );
					if ( subSelectable != null ) {
						return subSelectable;
					}
					count += embeddableMappingType.getJdbcValueCount();
				}
				else {
					if ( count == columnIndex ) {
						return aggregateMapping;
					}
					count++;
				}
			}
			else {
				if ( count == columnIndex ) {
					return (SelectableMapping) attributeMapping;
				}
				count += attributeMapping.getJdbcTypeCount();
			}
		}
		if ( isPolymorphic() && columnIndex == count ) {
			return getDiscriminatorMapping();
		}
		return null;
	}

	@Override
	default int getSelectableIndex(String selectableName) {
		final int numberOfAttributeMappings = getNumberOfAttributeMappings();
		int offset = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			final MappingType mappedType = attributeMapping.getMappedType();
			if ( mappedType instanceof EmbeddableMappingType ) {
				final EmbeddableMappingType embeddableMappingType = (EmbeddableMappingType) mappedType;
				final SelectableMapping aggregateMapping = embeddableMappingType.getAggregateMapping();
				if ( aggregateMapping != null ) {
					if ( aggregateMapping.getSelectableName().equals( selectableName ) ) {
						return offset;
					}
					offset++;
				}
				else {
					final int selectableIndex = embeddableMappingType.getSelectableIndex( selectableName );
					if ( selectableIndex != -1 ) {
						return offset + selectableIndex;
					}
					offset += embeddableMappingType.getJdbcTypeCount();
				}
			}
			else if ( attributeMapping instanceof SelectableMapping ) {
				if ( ( (SelectableMapping) attributeMapping ).getSelectableName().equals( selectableName ) ) {
					return offset;
				}
				offset++;
			}
			else {
				final MutableInteger position = new MutableInteger( -1 );
				final int jdbcTypeCount = attributeMapping.forEachSelectable(
						(selectionIndex, selectableMapping) -> {
							if ( selectableMapping.getSelectableName().equals( selectableName ) ) {
								position.set( selectionIndex );
							}
						}
				);
				if ( position.get() != -1 ) {
					return offset + position.get();
				}
				offset += jdbcTypeCount;
			}
		}
		if ( isPolymorphic() && getDiscriminatorMapping().getSelectableName().equals( selectableName ) ) {
			return offset;
		}
		return -1;
	}

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

	default void forEachInsertable(int offset, SelectableConsumer consumer) {
		forEachSelectable(
				offset,
				(selectionIndex, selectableMapping) -> {
					if ( ! selectableMapping.isInsertable() || selectableMapping.isFormula() ) {
						return;
					}

					consumer.accept( selectionIndex, selectableMapping );
				}
		);
	}

	default void forEachUpdatable(int offset, SelectableConsumer consumer) {
		forEachSelectable(
				offset,
				(selectionIndex, selectableMapping) -> {
					if ( ! selectableMapping.isUpdateable() || selectableMapping.isFormula() ) {
						return;
					}

					consumer.accept( selectionIndex, selectableMapping );
				}
		);
	}

	@Override
	int getJdbcTypeCount();

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
		forEachAttributeMapping(
				attributeMapping -> attributeMapping.applySqlSelections( navigablePath, tableGroup, creationState )
		);
	}

	@Override
	default void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		forEachAttributeMapping(
				attributeMapping ->
						attributeMapping.applySqlSelections(
								navigablePath,
								tableGroup,
								creationState,
								selectionConsumer
						)
		);
	}

	default int compare(Object value1, Object value2) {
		final AttributeMappingsList attributeMappings = getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			AttributeMapping attributeMapping = attributeMappings.get( i );
			final Getter getter = attributeMapping.getPropertyAccess().getGetter();
			final int comparison = attributeMapping.compare( getter.get( value1 ), getter.get( value2 ) );
			if ( comparison != 0 ) {
				return comparison;
			}
		}
		return 0;
	}
}
