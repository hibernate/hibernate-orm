/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Locale;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Value;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;

/**
 * @author Chris Cranford
 */
public class ValueMetadataGenerator extends AbstractMetadataGenerator {

	private final BasicMetadataGenerator basicMetadataGenerator;
	private final ComponentMetadataGenerator componentMetadataGenerator;
	private final ToOneRelationMetadataGenerator toOneRelationMetadataGenerator;
	private final JoinColumnCollectionMetadataGenerator joinedColumnCollectionGenerator;
	private final MiddleTableCollectionMetadataGenerator middleTableCollectionGenerator;

	public ValueMetadataGenerator(
			EnversMetadataBuildingContext metadataBuildingContext,
			BasicMetadataGenerator basicMetadataGenerator) {
		super( metadataBuildingContext );
		this.basicMetadataGenerator = basicMetadataGenerator;

		// case special value-based generators
		this.componentMetadataGenerator = new ComponentMetadataGenerator( metadataBuildingContext, this );
		this.toOneRelationMetadataGenerator = new ToOneRelationMetadataGenerator( metadataBuildingContext );
		this.joinedColumnCollectionGenerator = new JoinColumnCollectionMetadataGenerator( metadataBuildingContext, basicMetadataGenerator, this );
		this.middleTableCollectionGenerator = new MiddleTableCollectionMetadataGenerator( metadataBuildingContext, basicMetadataGenerator, this );
	}

	public void addValue(
			AttributeContainer attributeContainer,
			Value value,
			PropertyAccessStrategy propertyAccessStrategy,
			CompositeMapperBuilder currentMapper,
			String entityName,
			EntityMappingData mappingData,
			PropertyAuditingData propertyAuditingData,
			boolean insertable,
			boolean firstPass,
			boolean processModifiedFlags) {
		if ( firstPass ) {
			addValueInFirstPass(
					attributeContainer,
					value,
					propertyAccessStrategy,
					currentMapper,
					entityName,
					mappingData,
					propertyAuditingData,
					insertable,
					processModifiedFlags
			);
		}
		else {
			addValueInSecondPass(
					attributeContainer,
					value,
					propertyAccessStrategy,
					currentMapper,
					entityName,
					mappingData,
					propertyAuditingData,
					insertable,
					processModifiedFlags
			);
		}
	}

	private void addValueInFirstPass(
			AttributeContainer attributeContainer,
			Value value,
			PropertyAccessStrategy propertyAccessStrategy,
			CompositeMapperBuilder currentMapper,
			String entityName,
			EntityMappingData mappingData,
			PropertyAuditingData propertyAuditingData,
			boolean insertable,
			boolean processModifiedFlag) {
		final Type type = value.getType();
		propertyAuditingData.setPropertyAccessStrategy( propertyAccessStrategy );

		if ( type instanceof BasicType ) {
			basicMetadataGenerator.addBasic(
					attributeContainer,
					propertyAuditingData,
					value,
					currentMapper,
					insertable,
					false
			);
		}
		else if ( type instanceof ComponentType) {
			componentMetadataGenerator.addComponent(
					attributeContainer,
					propertyAuditingData,
					value,
					currentMapper,
					entityName,
					mappingData,
					true
			);
		}
		else {
			if ( !processedInSecondPass( type ) ) {
				// If we got here in the first pass, it means the basic mapper didn't map it, and none of the
				// above branches either.
				throwUnsupportedTypeException( type, entityName, propertyAuditingData.getName() );
			}
			return;
		}

		if ( isModifiedFlagsAllowed( processModifiedFlag, propertyAuditingData ) ) {
			addModifiedFlags( attributeContainer, propertyAuditingData, value );
		}
	}

	private void addValueInSecondPass(
			AttributeContainer attributeContainer,
			Value value,
			PropertyAccessStrategy propertyAccessStrategy,
			CompositeMapperBuilder currentMapper,
			String entityName,
			EntityMappingData mappingData,
			PropertyAuditingData propertyAuditingData,
			boolean insertable,
			boolean processModifiedFlag) {
		final Type type = value.getType();

		if ( type instanceof ComponentType ) {
			componentMetadataGenerator.addComponent(
					attributeContainer,
					propertyAuditingData,
					value,
					currentMapper,
					entityName,
					mappingData,
					false
			);
			// mod flag field has been already generated in first pass
			return;
		}
		else if ( type instanceof ManyToOneType ) {
			toOneRelationMetadataGenerator.addToOne(
					attributeContainer,
					propertyAuditingData,
					value,
					currentMapper,
					entityName,
					insertable
			);
		}
		else if ( type instanceof OneToOneType ) {
			final OneToOne oneToOne = (OneToOne) value;
			if ( oneToOne.getReferencedPropertyName() != null ) {
				toOneRelationMetadataGenerator.addOneToOneNotOwning(
						propertyAuditingData,
						value,
						currentMapper,
						entityName
				);
			}
			else {
				// @OneToOne relation marked with @PrimaryKeyJoinColumn
				toOneRelationMetadataGenerator.addOneToOnePrimaryKeyJoinColumn(
						propertyAuditingData,
						value,
						currentMapper,
						entityName,
						insertable
				);
			}
		}
		else if ( type instanceof CollectionType ) {

			final EntityConfiguration referencingEntityConfiguration = getAuditedEntityConfiguration( entityName );
			if ( referencingEntityConfiguration == null ) {
				throw new EnversMappingException(
						String.format(
								Locale.ENGLISH,
								"Unable to read auditing configuration for %s!",
								entityName
						)
				);
			}

			final Collection collection = (Collection) value;
			String referencedEntityName = MappingTools.getReferencedEntityName( collection.getElement() );

			final CollectionMetadataContext context = new CollectionMetadataContext() {
				@Override
				public EntityMappingData getEntityMappingData() {
					return mappingData;
				}

				@Override
				public Collection getCollection() {
					return collection;
				}

				@Override
				public CompositeMapperBuilder getMapperBuilder() {
					return currentMapper;
				}

				@Override
				public String getReferencedEntityName() {
					return referencedEntityName;
				}

				@Override
				public String getReferencingEntityName() {
					return entityName;
				}

				@Override
				public EntityConfiguration getReferencingEntityConfiguration() {
					return referencingEntityConfiguration;
				}

				@Override
				public PropertyAuditingData getPropertyAuditingData() {
					return propertyAuditingData;
				}
			};

			if ( context.isMiddleTableCollection() ) {
				middleTableCollectionGenerator.addCollection( context );
			}
			else {
				joinedColumnCollectionGenerator.addCollection(context );
			}
		}
		else {
			return;
		}

		if ( isModifiedFlagsAllowed( processModifiedFlag, propertyAuditingData ) ) {
			addModifiedFlags( attributeContainer, propertyAuditingData, value );
		}
	}

	private boolean processedInSecondPass(Type type) {
		return type instanceof ComponentType || type instanceof ManyToOneType ||
				type instanceof OneToOneType || type instanceof CollectionType;
	}

	private boolean isModifiedFlagsAllowed(boolean processModifiedFlags, PropertyAuditingData propertyAuditingData) {
		return processModifiedFlags && propertyAuditingData.isUsingModifiedFlag();
	}

	private void addModifiedFlags(AttributeContainer attributeContainer, PropertyAuditingData propertyAuditingData, Value value) {
		getMetadataBuildingContext().getConfiguration()
				.getModifiedColumnNamingStrategy()
				.addModifiedColumns(
						getMetadataBuildingContext().getConfiguration(),
						value,
						attributeContainer,
						propertyAuditingData
		);
	}

}
