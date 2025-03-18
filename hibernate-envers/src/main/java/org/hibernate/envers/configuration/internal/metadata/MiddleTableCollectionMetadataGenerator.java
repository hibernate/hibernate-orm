/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.envers.CollectionAuditTable;
import org.hibernate.envers.boot.model.CompositeIdentifier;
import org.hibernate.envers.boot.model.RootPersistentEntity;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.reader.AuditJoinTableData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.mapper.relation.CommonCollectionMapperData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

/**
 * An implementation of {@link AbstractCollectionMetadataGenerator} that builds collection metadata
 * and association mappings where the association uses a middle table mapping.
 *
 * @author Chris Cranford
 */
public class MiddleTableCollectionMetadataGenerator extends AbstractCollectionMetadataGenerator {

	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			EnversMessageLogger.class,
			MiddleTableCollectionMetadataGenerator.class.getName()
	);

	public MiddleTableCollectionMetadataGenerator(
			EnversMetadataBuildingContext metadataBuildingContext,
			BasicMetadataGenerator basicMetadataGenerator,
			ValueMetadataGenerator valueMetadataGenerator) {
		super( metadataBuildingContext, basicMetadataGenerator, valueMetadataGenerator );
	}

	@Override
	public void addCollection(CollectionMetadataContext context) {
		LOG.debugf(
				"Adding audit mapping for property %s.%s: collection with a join table",
				context.getReferencingEntityName(),
				context.getPropertyName()
		);

		// Generating the name of the middle table
		String auditMiddleTableName;
		String auditMiddleEntityName;
		if ( !StringTools.isEmpty( context.getPropertyAuditingData().getJoinTable().getName() ) ) {
			auditMiddleTableName = context.getPropertyAuditingData().getJoinTable().getName();
			auditMiddleEntityName = context.getPropertyAuditingData().getJoinTable().getName();
		}
		else {
			final Configuration configuration = getMetadataBuildingContext().getConfiguration();
			final String middleTableName = getMiddleTableName( context );
			auditMiddleTableName = configuration.getAuditTableName( null, middleTableName );
			auditMiddleEntityName = configuration.getAuditEntityName( middleTableName );
		}

		LOG.debugf( "Using join table name: %s", auditMiddleTableName );

		// Generating the XML mapping for the middle entity, only if the relation isn't inverse.
		// If the relation is inverse, will be later checked by comparing middleEntityXml with null.
		RootPersistentEntity middleEntity = null;
		if ( !context.getCollection().isInverse() ) {
			final AuditEntityNameRegister auditEntityNameRegistry = getMetadataBuildingContext().getAuditEntityNameRegistry();

			// Generating a unique middle entity name
			auditMiddleEntityName = auditEntityNameRegistry.createUnique( auditMiddleEntityName );

			// Registering the generated name
			auditEntityNameRegistry.register( auditMiddleEntityName );

			middleEntity = createMiddleEntity( context, auditMiddleTableName, auditMiddleEntityName );
		}

		// ******
		// Generating the mapping for the referencing entity (it must be an entity).
		// ******
		// Getting the id-mapping data of the referencing entity (the entity that "owns" this collection).
		final EntityConfiguration referencingEntityConfiguration = context.getReferencingEntityConfiguration();
		final IdMappingData referencingIdMapping = referencingEntityConfiguration.getIdMappingData();

		// Only valid for an inverse relation; null otherwise.
		String mappedBy;

		// The referencing prefix is always for a related entity. So it has always the "_" at the end added.
		String referencingPrefixRelated;
		String referencedPrefix;

		if ( context.getCollection().isInverse() ) {
			// If the relation is inverse, then referencedEntityName is not null.
			mappedBy = CollectionMappedByResolver.resolveMappedBy(
					context.getCollection().getCollectionTable(),
					getReferencedEntityMapping( context ),
					context.getPropertyAuditingData()
			);
			referencingPrefixRelated = mappedBy + "_";
			referencedPrefix = StringTools.getLastComponent( context.getReferencedEntityName() );
		}
		else {

			mappedBy = null;
			referencingPrefixRelated = StringTools.getLastComponent( context.getReferencingEntityName() ) + "_";
			referencedPrefix = context.getReferencedEntityName() == null ? "element" : context.getPropertyName();
		}

		// Storing the id data of the referencing entity: original mapper, prefixed mapper and entity name.
		final MiddleIdData referencingIdData = createMiddleIdData(
				referencingIdMapping,
				referencingPrefixRelated,
				context.getReferencingEntityName()
		);

		// Creating a query generator builder, to which additional id data will be added, in case this collection
		// references some entities (either from the element or index). At the end, this will be used to build
		// a query generator to read the raw data collection from the middle table.
		final String orderBy = getOrderBy( context.getCollection() );
		final QueryGeneratorBuilder queryGeneratorBuilder = new QueryGeneratorBuilder(
				getMetadataBuildingContext().getConfiguration(),
				referencingIdData,
				auditMiddleEntityName,
				isRevisionTypeInId( context ),
				getOrderByCollectionRole( context.getCollection(), orderBy )
		);

		// Adding the XML mapping for the referencing entity, if the relation isn't inverse.
		if ( middleEntity != null ) {
			// Adding related-entity (in this case: the referencing's entity id) id mapping to the xml.
			addAttributesToEntity(
					middleEntity,
					getPrefixedIdAttributes(
							referencingPrefixRelated,
							ColumnNameIterator.from( context.getCollection().getKey().getSelectables().iterator() ),
							referencingIdMapping
					)
			);
		}

		// Generating the element mapping.
		final MiddleComponentData elementComponentData = addValueToMiddleTable(
				context,
				context.getCollection().getElement(),
				middleEntity,
				queryGeneratorBuilder,
				referencedPrefix,
				context.getPropertyAuditingData().getJoinTable(),
				!isLobMapElementType( context )
		);

		// Generating the index mapping, if an index exists.
		final MiddleComponentData indexComponentData = addIndex( context, middleEntity, queryGeneratorBuilder );

		// Building the query generator.
		final RelationQueryGenerator queryGenerator = queryGeneratorBuilder.build( elementComponentData, indexComponentData );

		// Creating common data
		final CommonCollectionMapperData commonCollectionMapperData = createCommonCollectionMapperData(
				context,
				auditMiddleEntityName,
				referencingIdData,
				queryGenerator
		);

		// Checking the type of the collection and adding an appropriate mapper.
		addMapper( context, commonCollectionMapperData, elementComponentData, indexComponentData );

		// Storing information about this relation.
		storeMiddleEntityRelationInformation( context, mappedBy, referencingIdData, referencedPrefix, auditMiddleEntityName );
	}

	private String getMiddleTableName(CollectionMetadataContext context) {
		// We check how Hibernate maps the collection.
		final Collection collection = context.getCollection();
		if ( collection.getElement() instanceof OneToMany && !collection.isInverse() ) {
			final String entityName = context.getReferencingEntityName();
			// This must be a @JoinColumn+@OneToMany mapping. Generating the table name, as Hibernate doesn't use a
			// middle table for mapping this relation.
			return StringTools.getLastComponent( entityName )
					+ "_"
					+ StringTools.getLastComponent( MappingTools.getReferencedEntityName( collection.getElement() )
			);
		}
		// Hibernate uses a middle table for mapping this relation, so we get its name directly.
		CollectionAuditTable collectionAuditTable = context.getPropertyAuditingData().getCollectionAuditTable();
		if ( collectionAuditTable != null ) {
			return collectionAuditTable.name();
		}
		return collection.getCollectionTable().getName();
	}

	private RootPersistentEntity createMiddleEntity(CollectionMetadataContext context, String tableName, String entityName) {
		final AuditTableData auditTableData = new AuditTableData(
				entityName,
				tableName,
				resolveSchema( context ),
				resolveCatalog( context )
		);

		final RootPersistentEntity entity = new RootPersistentEntity( auditTableData, null );

		// When collection element uses a single table discriminator pattern; if any type of WHERE
		// conditions are present in the ORM mapping; they should be ignored by Envers; however,
		// otherwise all other use cases should be applied.
		if ( !context.isOneToManySingleTableSubclass() ) {
			entity.setWhereClause( context.getCollection().getWhere() );
		}

		CompositeIdentifier id = new CompositeIdentifier( getMetadataBuildingContext() );
		entity.setIdentifier( id );

		addRevisionInfoRelationToIdentifier( id );

		final boolean revisionTypeInId = isRevisionTypeInId( context );
		addRevisionTypeToAttributeContainer( revisionTypeInId ? id : entity, revisionTypeInId );
		addAuditStrategyAdditionalColumnsToEntity( entity );

		context.getEntityMappingData().addAdditionalMapping( entity );
		return entity;
	}

	private String resolveSchema(CollectionMetadataContext context) {
		final CollectionAuditTable collectionAuditTable = context.getPropertyAuditingData().getCollectionAuditTable();
		if ( collectionAuditTable != null && !StringTools.isEmpty( collectionAuditTable.schema() ) ) {
			return collectionAuditTable.schema();
		}

		final AuditJoinTableData joinTable = context.getPropertyAuditingData().getJoinTable();
		return getSchemaName( joinTable.getSchema(), context.getCollection().getCollectionTable() );
	}

	private String resolveCatalog(CollectionMetadataContext context) {
		final CollectionAuditTable collectionAuditTable = context.getPropertyAuditingData().getCollectionAuditTable();
		if ( collectionAuditTable != null && !StringTools.isEmpty( collectionAuditTable.catalog() ) ) {
			return collectionAuditTable.catalog();
		}
		final AuditJoinTableData joinTable = context.getPropertyAuditingData().getJoinTable();
		return getCatalogName( joinTable.getCatalog(), context.getCollection().getCollectionTable() );
	}

	private boolean isRevisionTypeInId(CollectionMetadataContext context) {
		return isEmbeddableElementType( context ) || isLobMapElementType( context );
	}

	private PersistentClass getReferencedEntityMapping(CollectionMetadataContext context) {
		return getMetadataBuildingContext().getMetadataCollector().getEntityBinding( context.getReferencedEntityName() );
	}

	private void storeMiddleEntityRelationInformation(
			CollectionMetadataContext context,
			String mappedBy,
			MiddleIdData referencingIdData,
			String referencedPrefix,
			String auditMiddleEntityName) {
		// Only if this is a relation (when there is a referenced entity or a component).
		if ( context.getReferencedEntityName() != null ) {
			final IdMappingData referencedIdMapping = getReferencedIdMappingData(
					context.getReferencingEntityName(),
					context.getReferencedEntityName(),
					context.getPropertyAuditingData(),
					true
			);
			final MiddleIdData referencedIdData = createMiddleIdData(
					referencedIdMapping,
					referencedPrefix + "_",
					context.getReferencedEntityName()
			);
			if ( context.getCollection().isInverse() ) {
				context.getReferencingEntityConfiguration().addToManyMiddleNotOwningRelation(
						context.getPropertyName(),
						mappedBy,
						context.getReferencedEntityName(),
						referencingIdData,
						referencedIdData,
						auditMiddleEntityName
				);
			}
			else {
				context.getReferencingEntityConfiguration().addToManyMiddleRelation(
						context.getPropertyName(),
						context.getReferencedEntityName(),
						referencingIdData,
						referencedIdData,
						auditMiddleEntityName
				);
			}
		}
		else {
			context.getReferencingEntityConfiguration().addToManyComponent(
					context.getPropertyName(),
					auditMiddleEntityName,
					referencingIdData
			);
		}
	}
}
