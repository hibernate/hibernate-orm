/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.SinglePropertyMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.CommonCollectionMapperData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.entities.mapper.relation.ToOneIdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleRelatedComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleStraightComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.query.OneAuditEntityQueryGenerator;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

/**
 * An implementation of {@link AbstractCollectionMetadataGenerator} that builds collection metadata
 * and association mappings where the association uses a join column mapping.
 *
 * @author Chris Cranford
 */
public class JoinColumnCollectionMetadataGenerator extends AbstractCollectionMetadataGenerator {

	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			EnversMessageLogger.class,
			JoinColumnCollectionMetadataGenerator.class.getName()
	);

	public JoinColumnCollectionMetadataGenerator(
			EnversMetadataBuildingContext metadataBuildingContext,
			BasicMetadataGenerator basicMetadataGenerator,
			ValueMetadataGenerator valueMetadataGenerator) {
		super( metadataBuildingContext, basicMetadataGenerator, valueMetadataGenerator );
	}

	@Override
	public void addCollection(CollectionMetadataContext context) {
		LOG.debugf(
				"Adding audit mapping for property %s.%s: one-to-many collection, using a join column on the referenced entity",
				context.getReferencingEntityName(),
				context.getPropertyName()
		);

		final Collection collection = context.getCollection();
		final PropertyAuditingData propertyAuditingData = context.getPropertyAuditingData();
		final String mappedBy = CollectionMappedByResolver.resolveMappedBy( collection, propertyAuditingData );

		final IdMappingData referencedIdMapping = getReferencedIdMappingData(
				context.getReferencingEntityName(),
				context.getReferencedEntityName(),
				propertyAuditingData,
				false
		);

		final EntityConfiguration referencingEntityConfiguration = context.getReferencingEntityConfiguration();
		final IdMappingData referencingIdMapping = referencingEntityConfiguration.getIdMappingData();

		// Generating the id mappers data for the referencing side of the relation.
		final MiddleIdData referencingIdData = createMiddleIdData(
				referencingIdMapping,
				mappedBy + "_",
				context.getReferencingEntityName()
		);

		// And for the referenced side. The prefixed mapper won't be used (as this collection isn't persisted
		// in a join table, so the prefix value is arbitrary).
		final MiddleIdData referencedIdData = createMiddleIdData(
				referencedIdMapping,
				null,
				context.getReferencedEntityName()
		);

		// Generating the element mapping.
		final MiddleRelatedComponentMapper elementComponentMapper = new MiddleRelatedComponentMapper( referencedIdData );
		final MiddleComponentData elementComponentData = new MiddleComponentData( elementComponentMapper );

		// Generating the index mapping, if an index exists. It can only exists in case a jakarta.persistence.MapKey
		// annotation is present on the entity. So the middleEntityXml will be not be used. The queryGeneratorBuilder
		// will only be checked for nullnes.
		MiddleComponentData indexComponentData = addIndex( context, null, null );

		// Generating the query generator - it should read directly from the related entity.
		final RelationQueryGenerator queryGenerator = new OneAuditEntityQueryGenerator(
				getMetadataBuildingContext().getConfiguration(),
				referencingIdData,
				context.getReferencedEntityName(),
				referencedIdData,
				context.getCollection().getElement() instanceof ComponentType,
				mappedBy,
				CollectionMappedByResolver.isMappedByKey( collection, mappedBy ),
				getOrderByCollectionRole( collection, collection.getOrderBy() )
		);

		// Creating common mapper data.
		final CommonCollectionMapperData commonCollectionMapperData = createCommonCollectionMapperData(
				context,
				context.getReferencedEntityName(),
				referencingIdData,
				queryGenerator
		);

		PropertyMapper fakeBidirectionalRelationMapper;
		PropertyMapper fakeBidirectionalRelationIndexMapper;
		if ( context.isFakeOneToManyBidirectional() || hasCollectionIndex( context ) ) {
			// In case of a fake many-to-one bidirectional relation, we have to generate a mapper which maps
			// the mapped-by property name to the id of the related entity (which is the owner of the collection).
			final String auditMappedBy = getAddOneToManyAttachedAuditMappedBy( context );

			fakeBidirectionalRelationMapper = getBidirectionalRelationMapper(
					context.getReferencingEntityName(),
					referencingIdMapping,
					auditMappedBy
			);

			// Checking if there's an index defined. If so, adding a mapper for it.
			final String positionMappedBy = getAttachedPositionMappedBy( context );
			if ( positionMappedBy != null ) {
				fakeBidirectionalRelationIndexMapper = getBidirectionalRelationIndexMapper( context, positionMappedBy );
				indexComponentData = getBidirectionalIndexData( indexComponentData, positionMappedBy );
			}
			else {
				fakeBidirectionalRelationIndexMapper = null;
			}
		}
		else {
			fakeBidirectionalRelationMapper = null;
			fakeBidirectionalRelationIndexMapper = null;
		}

		// Checking the type of the collection and adding an appropriate mapper.
		addMapper( context, commonCollectionMapperData, elementComponentData, indexComponentData );

		// Storing information about this relation.
		referencingEntityConfiguration.addToManyNotOwningRelation(
				context.getPropertyName(),
				mappedBy,
				context.getReferencedEntityName(),
				referencingIdData.getPrefixedMapper(),
				fakeBidirectionalRelationMapper,
				fakeBidirectionalRelationIndexMapper,
				hasCollectionIndex( context )
		);
	}

	private boolean hasCollectionIndex(CollectionMetadataContext context) {
		return context.getCollection().isIndexed() && ( (IndexedCollection) context.getCollection() ).getIndex() != null;
	}

	private String getAddOneToManyAttachedAuditMappedBy(CollectionMetadataContext context) {
		if ( context.isFakeOneToManyBidirectional() ) {
			return context.getPropertyAuditingData().getAuditMappedBy();
		}
		return context.getCollection().getMappedByProperty();
	}

	private PropertyMapper getBidirectionalRelationMapper(String entityName, IdMappingData idData, String auditMappedBy) {
		// Creating a prefixed relation mapper.
		final IdMapper relMapper = idData.getIdMapper().prefixMappedProperties(
				MappingTools.createToOneRelationPrefix( auditMappedBy )
		);

		return new ToOneIdMapper(
				relMapper,
				// The mapper will only be used to map from entity to map, so no need to provide other details
				// when constructing the PropertyData.
				new PropertyData( auditMappedBy, null, null ),
				entityName,
				false,
				false
		);
	}

	private PropertyMapper getBidirectionalRelationIndexMapper(CollectionMetadataContext context, String positionMappedBy) {
		if ( positionMappedBy != null ) {
			final Type indexType = getCollectionIndexType( context );
			return new SinglePropertyMapper( PropertyData.forProperty( positionMappedBy, indexType ) );
		}
		return null;
	}

	private Type getCollectionIndexType(CollectionMetadataContext context) {
		if ( context.getCollection().isIndexed() ) {
			return ( (IndexedCollection) context.getCollection() ).getIndex().getType();
		}
		// todo - do we need to reverse lookup the type anyway?
		return null;
	}

	private String getAttachedPositionMappedBy(CollectionMetadataContext context) {
		if ( context.isFakeOneToManyBidirectional() ) {
			return context.getPropertyAuditingData().getPositionMappedBy();
		}
		else if ( hasCollectionIndex( context ) ) {
			return ( (IndexedCollection) context.getCollection() ).getIndex().getSelectables().get( 0 ).getText();
		}
		return null;
	}

	private MiddleComponentData getBidirectionalIndexData(MiddleComponentData original, String positionMappedBy) {
		if ( positionMappedBy != null ) {
			// overwriting the index component data to properly read the index.
			return new MiddleComponentData( new MiddleStraightComponentMapper( positionMappedBy ) );
		}
		return original;
	}
}
