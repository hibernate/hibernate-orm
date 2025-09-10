/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

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
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.spi.CompositeTypeImplementor;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping.IdentifierValueMapper;

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
		super( new MutableAttributeMappingList( virtualIdSource.getType().getPropertyNames().length ) );

		this.idMapping = idMapping;
		navigableRole = idMapping.getNavigableRole();
		representationStrategy = new VirtualIdRepresentationStrategy(
				this,
				identifiedEntityMapping,
				virtualIdSource,
				creationProcess.getCreationContext()
		);

		final var compositeType = virtualIdSource.getType();
		( (CompositeTypeImplementor) compositeType ).injectMappingModelPart( idMapping, creationProcess );

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
		super( new MutableAttributeMappingList( inverseMappingType.attributeMappings.size() ) );

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
						this,
						this.attributeMappings
				)
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IdentifierValueMapper

	@Override
	public EmbeddableValuedModelPart getEmbeddedPart() {
		return idMapping;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return representationStrategy.getInstantiator().instantiate( () -> getValues( entity ) );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		if ( entity != id ) {
			setValues( entity, getValues( id ) );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableMappingType

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getPartName() {
		return idMapping.getPartName();
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return getEmbeddedPart();
	}

	@Override
	public VirtualIdRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return idMapping.findContainingEntityMapping();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(NavigablePath navigablePath, TableGroup tableGroup, String resultVariable, DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer, SharedSessionContractImplementor session) {
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
						attributeMapping.getValue( domainValue ),
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

	@Override
	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
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

		// Reset the attribute mappings that were added in previous attempts
		this.attributeMappings.clear();

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
				this::addAttribute,
				() -> {
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
	public boolean areEqual(@Nullable Object one, @Nullable Object other, SharedSessionContractImplementor session) {
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
	public int compare(Object value1, Object value2) {
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
