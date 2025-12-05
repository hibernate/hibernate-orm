/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Consumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping.IdentifierValueMapper;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.CompositeTypeImplementor;

/**
 * EmbeddableMappingType implementation describing an {@link jakarta.persistence.IdClass}
 */
public class IdClassEmbeddable extends AbstractEmbeddableMapping implements IdentifierValueMapper {
	private final NavigableRole navigableRole;
	private final NonAggregatedIdentifierMapping idMapping;
	private final VirtualIdEmbeddable virtualIdEmbeddable;
	private final JavaType<?> javaType;
	private final IdClassRepresentationStrategy representationStrategy;
//	private final IdClassEmbedded embedded;
	private final EmbeddableValuedModelPart embedded;

	public IdClassEmbeddable(
			Component idClassSource,
			RootClass bootEntityDescriptor,
			NonAggregatedIdentifierMapping idMapping,
			EntityMappingType identifiedEntityMapping,
			String idTable,
			String[] idColumns,
			VirtualIdEmbeddable virtualIdEmbeddable,
			MappingModelCreationProcess creationProcess) {
		super( new MutableAttributeMappingList( idClassSource.getPropertySpan() ) );

		this.idMapping = idMapping;
		this.virtualIdEmbeddable = virtualIdEmbeddable;

		navigableRole = idMapping.getNavigableRole().append( NavigablePath.IDENTIFIER_MAPPER_PROPERTY );

		javaType =
				creationProcess.getCreationContext().getTypeConfiguration().getJavaTypeRegistry()
						.resolveManagedTypeDescriptor( idClassSource.getComponentClass() );

		representationStrategy = new IdClassRepresentationStrategy(
				this,
				idClassSource.sortProperties() == null,
				idClassSource::getPropertyNames
		);

		final var propertyAccess =
				PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
						null,
						EntityIdentifierMapping.ID_ROLE_NAME,
						true
				);
		final var attributeMetadata =
				MappingModelCreationHelper.getAttributeMetadata( propertyAccess );

		embedded = new EmbeddedAttributeMapping(
				NavigablePath.IDENTIFIER_MAPPER_PROPERTY,
				identifiedEntityMapping.getNavigableRole()
						.append( EntityIdentifierMapping.ID_ROLE_NAME )
						.append( NavigablePath.IDENTIFIER_MAPPER_PROPERTY ),
				-1,
				-1,
				idTable,
				attributeMetadata,
				(PropertyAccess) null,
				FetchTiming.IMMEDIATE,
				FetchStyle.JOIN,
				this,
				identifiedEntityMapping,
				propertyAccess
		);

		final var idClassType = idClassSource.getType();
		( (CompositeTypeImplementor) idClassType ).injectMappingModelPart( embedded, creationProcess );

		creationProcess.registerInitializationCallback(
				"IdClassEmbeddable(" + navigableRole.getFullPath() + ")#finishInitialization",
				() ->
						finishInitialization(
								idClassSource,
								idClassType,
								idTable,
								idColumns,
								creationProcess
						)
		);

	}

	public IdClassEmbeddable(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			IdClassEmbeddable inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		super( new MutableAttributeMappingList( inverseMappingType.attributeMappings.size() ) );

		this.navigableRole = inverseMappingType.getNavigableRole();
		this.idMapping = (NonAggregatedIdentifierMapping) valueMapping;
		this.virtualIdEmbeddable = (VirtualIdEmbeddable) valueMapping.getEmbeddableTypeDescriptor();
		this.javaType = inverseMappingType.javaType;
		this.representationStrategy = new IdClassRepresentationStrategy( this, false, () -> {
			final String[] attributeNames = new String[inverseMappingType.getNumberOfAttributeMappings()];
			for ( int i = 0; i < attributeNames.length; i++ ) {
				attributeNames[i] = inverseMappingType.getAttributeMapping( i ).getAttributeName();
			}
			return attributeNames;
		} );
		this.embedded = valueMapping;
		this.selectableMappings = selectableMappings;
		creationProcess.registerInitializationCallback(
				"IdClassEmbeddable(" + inverseMappingType.getNavigableRole().getFullPath() + ".{inverse})#finishInitialization",
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
		return embedded;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		final Object id = representationStrategy.getInstantiator().instantiate( null );

		final var propertyValues = new Object[virtualIdEmbeddable.getNumberOfAttributeMappings()];

		for ( int i = 0; i < propertyValues.length; i++ ) {
			final var attributeMapping = virtualIdEmbeddable.getAttributeMapping( i );
			final Object object = attributeMapping.getValue( entity );
			if ( object == null ) {
				final var idClassAttributeMapping = getAttributeMapping( i );
				propertyValues[i] =
						idClassAttributeMapping.getPropertyAccess().getGetter().getReturnTypeClass().isPrimitive()
								? idClassAttributeMapping.getExpressibleJavaType().getDefaultValue()
								: null;
			}
			//JPA 2 @MapsId + @IdClass points to the pk of the entity
			else if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping
					&& !( getAttributeMapping( i ) instanceof ToOneAttributeMapping ) ) {
				final var targetPart =
						toOneAttributeMapping.getForeignKeyDescriptor()
								.getPart( toOneAttributeMapping.getSideNature().inverse() );
				if ( targetPart.isEntityIdentifierMapping() ) {
					propertyValues[i] = ( (EntityIdentifierMapping) targetPart ).getIdentifier( object );
				}
				else {
					propertyValues[i] = object;
					assert false;
				}
			}
			else {
				propertyValues[i] = object;
			}
		}

		setValues( id, propertyValues );

		return id;
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		final var entityDescriptor =
				factory.getMappingMetamodel()
						.getEntityDescriptor( entity.getClass() );
		final var propertyValues = new Object[attributeMappings.size()];
		virtualIdEmbeddable.forEachAttribute(
				(position, virtualIdAttribute) -> {
					final var idClassAttribute = attributeMappings.get( position );
					Object object = idClassAttribute.getValue( id );
					if ( virtualIdAttribute instanceof ToOneAttributeMapping toOneAttributeMapping
						&& !( idClassAttribute instanceof ToOneAttributeMapping ) ) {
						final var entityPersister =
								toOneAttributeMapping.getEntityMappingType()
										.getEntityPersister();
						final var entityKey = session.generateEntityKey( object, entityPersister );
						final var persistenceContext = session.getPersistenceContext();
						final var holder = persistenceContext.getEntityHolder( entityKey );
						// use the managed object i.e. proxy or initialized entity
						object = holder == null ? null : holder.getManagedObject();
						if ( object == null ) {
							// get the association out of the entity itself
							object = entityDescriptor.getPropertyValue( entity,
									toOneAttributeMapping.getAttributeName() );
						}
					}
					propertyValues[position] = object;
				}
		);

		virtualIdEmbeddable.setValues( entity, propertyValues );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableMappingType

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getPartName() {
		return NavigablePath.IDENTIFIER_MAPPER_PROPERTY;
	}

	@Override
	public EmbeddableRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return javaType;
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return embedded;
	}

	@Override
	public void forEachAttributeMapping(Consumer<? super AttributeMapping> action) {
		forEachAttribute( (index, attribute) -> action.accept( attribute ) );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return idMapping.findContainingEntityMapping();
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}


	@Override
	public <T> DomainResult<T> createDomainResult(NavigablePath navigablePath, TableGroup tableGroup, String resultVariable, DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		return new IdClassEmbeddable(
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
						throw new IllegalAttributeType( "An IdClass cannot define collection attributes : " + attributeName );
					}
					if ( attributeType instanceof AnyType ) {
						throw new IllegalAttributeType( "An IdClass cannot define <any/> attributes : " + attributeName );
					}
				},
				(column, jdbcEnvironment) -> MappingModelCreationHelper.getTableIdentifierExpression( column.getValue().getTable(), creationProcess ),
				this::addAttribute,
				() -> {
					// We need the attribute mapping types to finish initialization first before we can build the column mappings
					creationProcess.registerInitializationCallback(
							"IdClassEmbeddable(" + getNavigableRole() + ")#initColumnMappings",
							this::initColumnMappings
					);
				},
				creationProcess
		);
	}

}
