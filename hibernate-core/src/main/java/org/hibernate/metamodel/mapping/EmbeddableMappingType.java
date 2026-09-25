package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;

import org.hibernate.spi.IndexedConsumer;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Describes an embeddable - the actual type
 *
 * @implNote Even though this represents the embeddable class, one is created for
 * each embedded usage.  This is done to help
 *
 * @see EmbeddableValuedModelPart
 */
public interface EmbeddableMappingType extends ManagedMappingType, SelectableMappings {
	@Nonnull
	EmbeddableValuedModelPart getEmbeddedValueMapping();

	@Nonnull
	EmbeddableRepresentationStrategy getRepresentationStrategy();

	/**
	 * Returns the {@linkplain EmbeddableDiscriminatorMapping discriminator mapping}
	 * if this discriminator type is polymorphic, {@code null} otherwise.
	 */
	@Nullable
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

	interface ConcreteEmbeddableType {

		@Nonnull
		EmbeddableInstantiator getInstantiator();

		int getSubclassId();

		/**
		 * Returns {@code true} if the provided embeddable class contains the
		 * specified attribute mapping, {@code false} otherwise.
		 * @implNote This method always returns {@code true} for non-polymorphic embeddable types
		 *
		 * @param attributeMapping the attribute to check
		 */
		boolean declaresAttribute(@Nonnull AttributeMapping attributeMapping);

		boolean declaresAttribute(int attributeIndex);

		@Nullable
		Object getDiscriminatorValue();
	}

	@Nullable
	default ConcreteEmbeddableType findSubtypeByDiscriminator(@Nonnull Object discriminatorValue) {
		return null;
	}

	@Nullable
	default ConcreteEmbeddableType findSubtypeBySubclass(@Nonnull String subclassName) {
		return null;
	}

	/**
	 * Returns the concrete embeddable subtypes or an empty collection if {@link #isPolymorphic()} is {@code false}.
	 */
	@Nonnull
	default Collection<ConcreteEmbeddableType> getConcreteEmbeddableTypes() {
		return Collections.emptySet();
	}

	@Nullable
	default SelectableMapping getAggregateMapping() {
		return null;
	}

	default boolean shouldSelectAggregateMapping() {
		return getAggregateMapping() != null;
	}

	@Nonnull
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
		else {
			// Cache this maybe?
			final int aggregateSqlTypeCode =
					aggregateMapping.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
			return castNonNull( findContainingEntityMapping() )
					.getEntityPersister().getFactory().getJdbcServices()
					.getDialect().getAggregateSupport()
					.requiresAggregateCustomWriteExpressionRenderer( aggregateSqlTypeCode );
		}
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
			if ( mappedType instanceof EmbeddableMappingType embeddableMappingType
					&& embeddableMappingType.getAggregateMapping() != null ) {
				count++;
			}
			else {
				count += attributeMapping.getJdbcTypeCount();
			}
		}
		return count;
	}

	@Nullable
	default SelectableMapping getJdbcValueSelectable(int columnIndex) {
		final int numberOfAttributeMappings = getNumberOfAttributeMappings();
		int count = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			if ( attributeMapping instanceof DiscriminatedAssociationAttributeMapping discriminatedAssociationAttributeMapping ) {
				if ( count == columnIndex ) {
					return discriminatedAssociationAttributeMapping.getDiscriminatorMapping();
				}
				count++;
				if ( count == columnIndex ) {
					return discriminatedAssociationAttributeMapping.getKeyPart();
				}
				count++;
			}
			else if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
				if ( toOneAttributeMapping.getSideNature() == ForeignKeyDescriptor.Nature.KEY ) {
					final ValuedModelPart keyPart = toOneAttributeMapping.getForeignKeyDescriptor().getKeyPart();
					if ( keyPart instanceof BasicValuedMapping ) {
						if ( count == columnIndex ) {
							return (SelectableMapping) keyPart;
						}
						count++;
					}
					else if ( keyPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
						final EmbeddableMappingType mappingType = embeddableValuedModelPart.getEmbeddableTypeDescriptor();
						final SelectableMapping selectable = mappingType.getJdbcValueSelectable( columnIndex - count );
						if ( selectable != null ) {
							return selectable;
						}
						count += mappingType.getJdbcValueCount();
					}
					else {
						throw new UnsupportedOperationException( "Unsupported foreign key part: " + keyPart );
					}
				}
			}
			else if ( attributeMapping instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
				final EmbeddableMappingType embeddableMappingType = embeddableValuedModelPart.getMappedType();
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
					if ( attributeMapping instanceof SelectableMapping selectableMapping ) {
						return selectableMapping;
					}
					assert attributeMapping.getJdbcTypeCount() == 0;
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
	default int getSelectableIndex(@Nonnull String selectableName) {
		final int numberOfAttributeMappings = getNumberOfAttributeMappings();
		int offset = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			final MappingType mappedType = attributeMapping.getMappedType();
			if ( mappedType instanceof EmbeddableMappingType embeddableMappingType ) {
				final SelectableMapping aggregateMapping = embeddableMappingType.getAggregateMapping();
				if ( aggregateMapping != null ) {
					if ( aggregateMapping.getSelectableName().equals( selectableName ) ) {
						return offset;
					}
					offset++;
				}
				else {
					final int selectableIndex =
							embeddableMappingType.getSelectableIndex( selectableName );
					if ( selectableIndex != -1 ) {
						return offset + selectableIndex;
					}
					offset += embeddableMappingType.getJdbcTypeCount();
				}
			}
			else if ( attributeMapping instanceof SelectableMapping selectableMapping) {
				if ( selectableMapping.getSelectableName().equals( selectableName ) ) {
					return offset;
				}
				offset++;
			}
			else {
				final var position = new MutableInteger( -1 );
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
		final var discriminatorMapping = getDiscriminatorMapping();
		if ( discriminatorMapping != null
				&& discriminatorMapping.getSelectableName().equals( selectableName ) ) {
			return offset;
		}
		return -1;
	}

	@Nonnull
	@org.hibernate.Internal
	EmbeddableMappingType createInverseMappingType(
			@Nonnull EmbeddedAttributeMapping valueMapping,
			@Nonnull TableGroupProducer declaringTableGroupProducer,
			@Nonnull SelectableMappings selectableMappings,
			@Nonnull MappingModelCreationProcess creationProcess);

	@Override
	default int forEachSelectable(@Nonnull SelectableConsumer consumer) {
		return ManagedMappingType.super.forEachSelectable( consumer );
	}

	@Override
	int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer);

	default void forEachInsertable(int offset, @Nonnull SelectableConsumer consumer) {
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

	default void forEachUpdatable(int offset, @Nonnull SelectableConsumer consumer) {
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
	int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action);

	// Make this abstract again to ensure subclasses implement this method
	@Nonnull
	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	<T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState);

	@Override
	default void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		forEachAttributeMapping(
				attributeMapping -> attributeMapping.applySqlSelections( navigablePath, tableGroup, creationState )
		);
	}

	@Override
	default void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
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

	default int compare(@Nullable Object value1, @Nullable Object value2) {
		if ( value1 == value2 ) {
			return 0;
		}
		else if ( value1 == null ) {
			return -1;
		}
		else if ( value2 == null ) {
			return 1;
		}
		else {
			final var attributeMappings = getAttributeMappings();
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final var attribute = attributeMappings.get( i );
				final int comparison =
						attribute.compare(
								attribute.getValue( value1 ),
								attribute.getValue( value2 )
						);
				if ( comparison != 0 ) {
					return comparison;
				}
			}
			return 0;
		}
	}
}
