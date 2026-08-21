/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Locale;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.RelationTargetNotFoundAction;
import org.hibernate.envers.configuration.internal.metadata.reader.AuditedPropertiesHolder;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.OneToOneNotOwningMapper;
import org.hibernate.envers.internal.entities.mapper.relation.OneToOnePrimaryKeyJoinColumnMapper;
import org.hibernate.envers.internal.entities.mapper.relation.ToOneIdMapper;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

/**
 * Generates metadata for to-one relations (reference-valued properties).
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public final class ToOneRelationMetadataGenerator extends AbstractMetadataGenerator {

	public ToOneRelationMetadataGenerator(EnversMetadataBuildingContext metadataBuildingContext) {
		super( metadataBuildingContext );
	}

	public void addToOne(
			AttributeContainer mapping,
			PropertyAuditingData propertyAuditingData,
			Value value,
			CompositeMapperBuilder mapper,
			String entityName,
			boolean insertable) {
		final String referencedEntityName = ( (ToOne) value ).getReferencedEntityName();

		final IdMappingData idMapping = getReferencedIdMappingData(
				entityName,
				referencedEntityName,
				propertyAuditingData,
				true
		);

		final String lastPropertyPrefix = MappingTools.createToOneRelationPrefix( propertyAuditingData.getName() );

		// Generating the id mapper for the relation
		final IdMapper relMapper = idMapping.getIdMapper().prefixMappedProperties( lastPropertyPrefix );

		// Storing information about this relation
		getAuditedEntityConfiguration( entityName ).addToOneRelation(
				propertyAuditingData.getName(),
				referencedEntityName,
				relMapper,
				insertable,
				shouldIgnoreNotFoundRelation( propertyAuditingData, value ),
				isRelationNotAudited( propertyAuditingData )
		);

		// If the property isn't insertable, checking if this is not a "fake" bidirectional many-to-one relationship,
		// that is, when the one side owns the relation (and is a collection), and the many side is non insertable.
		// When that's the case and the user specified to store this relation without a middle table (using
		// @AuditMappedBy), we have to make the property insertable for the purposes of Envers. In case of changes to
		// the entity that didn't involve the relation, it's value will then be stored properly. In case of changes
		// to the entity that did involve the relation, it's the responsibility of the collection side to store the
		// proper data.
		boolean nonInsertableFake;
		if ( !insertable && propertyAuditingData.isForceInsertable() ) {
			nonInsertableFake = true;
			insertable = true;
		}
		else {
			nonInsertableFake = false;
		}

		// Adding an element to the mapping corresponding to the references entity id's
		idMapping.getRelation()
				.getAttributesPrefixed( lastPropertyPrefix, value.getSelectables().iterator(), false, insertable )
				.forEach( mapping::addAttribute );

		boolean lazy = ( (ToOne) value ).isLazy();

		// Adding mapper for the id
		final PropertyData propertyData = propertyAuditingData.resolvePropertyData();
		mapper.addComposite(
				propertyData,
				new ToOneIdMapper( relMapper, propertyData, referencedEntityName, nonInsertableFake, lazy )
		);
	}

	public void addOneToOneNotOwning(
			PropertyAuditingData propertyAuditingData,
			Value value,
			CompositeMapperBuilder mapper,
			String entityName) {
		final OneToOne propertyValue = (OneToOne) value;
		final String owningReferencePropertyName = propertyValue.getReferencedPropertyName();

		final EntityConfiguration configuration = getAuditedEntityConfiguration( entityName );
		if ( configuration == null ) {
			throw new EnversMappingException( "An audited relation to a non-audited entity " + entityName + "!" );
		}

		final IdMappingData ownedIdMapping = configuration.getIdMappingData();

		if ( ownedIdMapping == null ) {
			throw new EnversMappingException( "An audited relation to a non-audited entity " + entityName + "!" );
		}

		final var referencedEntityName = propertyValue.getReferencedEntityName();
		final var propertyName = propertyAuditingData.getName();
		checkMappedByAudited(
				entityName,
				propertyName,
				referencedEntityName,
				owningReferencePropertyName,
				getMetadataBuildingContext().getClassesAuditingData().getClassAuditingData( referencedEntityName )
		);

		final String lastPropertyPrefix = MappingTools.createToOneRelationPrefix( owningReferencePropertyName );

		// Generating the id mapper for the relation
		final IdMapper ownedIdMapper = ownedIdMapping.getIdMapper().prefixMappedProperties( lastPropertyPrefix );

		// Storing information about this relation
		configuration.addToOneNotOwningRelation(
				propertyName,
				owningReferencePropertyName,
				referencedEntityName,
				ownedIdMapper,
				MappingTools.ignoreNotFound( value ),
				isRelationNotAudited( propertyAuditingData )
		);

		// Adding mapper for the id
		final PropertyData propertyData = propertyAuditingData.resolvePropertyData();
		mapper.addComposite(
				propertyData,
				new OneToOneNotOwningMapper(
						entityName,
						referencedEntityName,
						owningReferencePropertyName,
						propertyData,
						getMetadataBuildingContext().getServiceRegistry()
				)
		);
	}

	private static void checkMappedByAudited(
			String entityName,
			String associationName,
			String referencedEntityName,
			String referencedPropertyName,
			AuditedPropertiesHolder propertiesHolder) {
		final var split = referencedPropertyName.split( "\\.", 2 );
		final var auditingData = propertiesHolder.getPropertyAuditingData( split[0] );
		if ( auditingData == null ) {
			throw new EnversMappingException( String.format(
					Locale.ROOT,
					"Could not resolve mapped by property [%s] for association [%s.%s] in the referenced entity [%s],"
							+ " please ensure that the association is audited on both sides.",
					referencedPropertyName,
					entityName,
					associationName,
					referencedEntityName
			) );
		}
		else if ( split.length > 1 ) {
			// mapped by is a nested component property
			checkMappedByAudited( entityName, associationName, referencedEntityName, split[1], (ComponentAuditingData) auditingData );
		}
	}

	void addOneToOnePrimaryKeyJoinColumn(
			PropertyAuditingData propertyAuditingData,
			Value value,
			CompositeMapperBuilder mapper,
			String entityName,
			boolean insertable) {
		final String referencedEntityName = ( (ToOne) value ).getReferencedEntityName();

		final IdMappingData idMapping = getReferencedIdMappingData(
				entityName,
				referencedEntityName,
				propertyAuditingData,
				true
		);

		final String lastPropertyPrefix = MappingTools.createToOneRelationPrefix( propertyAuditingData.getName() );

		// Generating the id mapper for the relation
		final IdMapper relMapper = idMapping.getIdMapper().prefixMappedProperties( lastPropertyPrefix );

		// Storing information about this relation
		getAuditedEntityConfiguration( entityName ).addToOneRelation(
				propertyAuditingData.getName(),
				referencedEntityName,
				relMapper,
				insertable,
				MappingTools.ignoreNotFound( value ),
				isRelationNotAudited( propertyAuditingData )
		);

		// Adding mapper for the id
		final PropertyData propertyData = propertyAuditingData.resolvePropertyData();
		mapper.addComposite(
				propertyData,
				new OneToOnePrimaryKeyJoinColumnMapper(
						entityName,
						referencedEntityName,
						propertyData,
						getMetadataBuildingContext().getServiceRegistry()
				)
		);
	}

	private boolean shouldIgnoreNotFoundRelation(PropertyAuditingData propertyAuditingData, Value value) {
		final RelationTargetNotFoundAction action = propertyAuditingData.getRelationTargetNotFoundAction();
		if ( getMetadataBuildingContext().getConfiguration().isGlobalLegacyRelationTargetNotFound() ) {
			// When legacy is enabled, the user must explicitly specify IGNORE for it to be ignored.
			return MappingTools.ignoreNotFound( value ) || RelationTargetNotFoundAction.IGNORE.equals( action );
		}
		else {
			// When non-legacy is enabled, the situation is ignored when not ERROR
			return !RelationTargetNotFoundAction.ERROR.equals( action );
		}
	}
}
