/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.spi.CompositeTypeImplementor;

import jakarta.annotation.Nullable;

import static org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping.IdentifierValueMapper;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Embeddable describing the virtual-id aspect of a non-aggregated composite id
 */
public class VirtualIdEmbeddable extends AbstractEmbeddableMapping implements IdentifierValueMapper {
	private final NavigableRole navigableRole;
	private final NonAggregatedIdentifierMapping idMapping;
	private final VirtualIdRepresentationStrategy representationStrategy;

	public VirtualIdEmbeddable(
			Component virtualIdSource,
			NonAggregatedIdentifierMapping idMapping,
			EntityPersister identifiedEntityMapping,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {
		super( virtualIdSource.getType().getPropertyNames().length );

		this.idMapping = idMapping;
		navigableRole = castNonNull( idMapping.getNavigableRole() );
		representationStrategy = new VirtualIdRepresentationStrategy(
				this,
				identifiedEntityMapping,
				virtualIdSource,
				creationProcess.getCreationContext()
		);

		final var compositeType = virtualIdSource.getType();
		( (CompositeTypeImplementor) compositeType )
				.injectMappingModelPart( idMapping, creationProcess );

		creationProcess.registerInitializationCallback(
				"VirtualIdEmbeddable(" + navigableRole.getFullPath() + ")#finishInitialization",
				() ->
						finishInitialization(
								virtualIdSource,
								compositeType,
								rootTableExpression,
								rootTableKeyColumnNames,
								creationProcess
						)
		);
	}

	public VirtualIdEmbeddable(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			VirtualIdEmbeddable inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		super( inverseMappingType.attributeMappings.size() );

		this.navigableRole = inverseMappingType.getNavigableRole();
		this.idMapping = (NonAggregatedIdentifierMapping) valueMapping;
		this.representationStrategy = inverseMappingType.representationStrategy;
		this.selectableMappings = selectableMappings;
		creationProcess.registerInitializationCallback(
				"VirtualIdEmbeddable(" + inverseMappingType.getNavigableRole().getFullPath() + ".{inverse})#finishInitialization",
				() -> inverseInitializeCallback(
						declaringTableGroupProducer,
						selectableMappings,
						inverseMappingType,
						creationProcess,
						this
				)
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IdentifierValueMapper

	@Nonnull
	@Override
	public EmbeddableValuedModelPart getEmbeddedPart() {
		return idMapping;
	}

	@Nonnull
	@Override
	public Object getIdentifier(@Nonnull Object entity, @Nullable SharedSessionContractImplementor session) {
		return representationStrategy.getInstantiator().instantiate( () -> getValues( entity ) );
	}

	@Override
	public void setIdentifier(@Nonnull Object entity, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		if ( entity != id ) {
			setValues( entity, getValues( id ) );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableMappingType

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Nonnull
	@Override
	public String getPartName() {
		return idMapping.getPartName();
	}

	@Nonnull
	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return getEmbeddedPart();
	}

	@Nonnull
	@Override
	public VirtualIdRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Nullable
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return idMapping.findContainingEntityMapping();
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(@Nonnull NavigablePath navigablePath, @Nonnull TableGroup tableGroup, @Nullable String resultVariable, @Nonnull DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X, Y> int decompose(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer, @Nullable SharedSessionContractImplementor session) {
		if ( idMapping.getIdClassEmbeddable() != null ) {
			// during decompose, if there is an IdClass for the entity the
			// incoming `domainValue` should be an instance of that IdClass
			return idMapping.getIdClassEmbeddable().decompose( domainValue, offset, x, y, valueConsumer, session );
		}
		else {
			int span = 0;
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final var attributeMapping = attributeMappings.get( i );
				span += attributeMapping.decompose(
						domainValue == null ? null : attributeMapping.getValue( domainValue ),
						offset + span,
						x,
						y,
						valueConsumer,
						session
				);
			}
			return span;
		}
	}

	@Nonnull
	@Override
	public EmbeddableMappingType createInverseMappingType(
			@Nonnull EmbeddedAttributeMapping valueMapping,
			@Nonnull TableGroupProducer declaringTableGroupProducer,
			@Nonnull SelectableMappings selectableMappings,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return new VirtualIdEmbeddable(
				valueMapping,
				declaringTableGroupProducer,
				selectableMappings,
				this,
				creationProcess
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// init

	private boolean finishInitialization(
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {

		final var attributeMappings = new ImmutableAttributeMappingList.Builder( compositeType.getPropertyNames().length );

		return finishInitialization(
				navigableRole,
				bootDescriptor,
				compositeType,
				rootTableExpression,
				rootTableKeyColumnNames,
				this,
				representationStrategy,
				(attributeName, attributeType) -> {
					if ( attributeType instanceof CollectionType ) {
						throw new IllegalAttributeType( "A \"virtual id\" cannot define collection attributes : " + attributeName );
					}
					if ( attributeType instanceof AnyType ) {
						throw new IllegalAttributeType( "A \"virtual id\" cannot define <any/> attributes : " + attributeName );
					}
				},
				(column, jdbcEnvironment) -> MappingModelCreationHelper.getTableIdentifierExpression( column.getValue().getTable(), creationProcess ),
				attributeMappings::add,
				() -> {
					this.attributeMappings = attributeMappings.build();
					// We need the attribute mapping types to finish initialization first before we can build the column mappings
					creationProcess.registerInitializationCallback(
							"VirtualIdEmbeddable(" + navigableRole + ")#initColumnMappings",
							this::initColumnMappings
					);
				},
				creationProcess
		);
	}

	@Override
	public boolean areEqual(@Nullable Object one, @Nullable Object other, @Nullable SharedSessionContractImplementor session) {
		if ( one == other ) {
			return true;
		}
		if ( one == null || other == null ) {
			return false;
		}
		final var idClassEmbeddable = idMapping.getIdClassEmbeddable();
		if ( idClassEmbeddable != null ) {
			return idClassEmbeddable.areEqual( one, other, session );
		}
		else {
			final var attributeMappings = getAttributeMappings();
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final var attribute = attributeMappings.get( i );
				if ( !attribute.areEqual( attribute.getValue( one ),
						attribute.getValue( other ), session ) ) {
					return false;
				}
			}
			return true;
		}
	}

	@Override
	public int compare(@Nullable Object value1, @Nullable Object value2) {
		if ( value1 == value2 ) {
			return 0;
		}
		if ( value1 == null ) {
			return -1;
		}
		if ( value2 == null ) {
			return 1;
		}
		final var idClassEmbeddable = idMapping.getIdClassEmbeddable();
		if ( idClassEmbeddable != null ) {
			final var attributeMappings = idClassEmbeddable.getAttributeMappings();
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final var attribute = attributeMappings.get( i );
				final int comparison =
						attribute.compare( attribute.getValue( value1 ),
								attribute.getValue( value2 ) );
				if ( comparison != 0 ) {
					return comparison;
				}
			}
			return 0;
		}
		else {
			return super.compare( value1, value2 );
		}
	}
}
