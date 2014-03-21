/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.MappingException;
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
import org.hibernate.metamodel.spi.binding.OneToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;

import org.dom4j.Element;

/**
 * Generates metadata for to-one relations (reference-valued properties).
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public final class ToOneRelationMetadataGenerator {
	private final AuditMetadataGenerator mainGenerator;

	ToOneRelationMetadataGenerator(AuditMetadataGenerator auditMetadataGenerator) {
		mainGenerator = auditMetadataGenerator;
	}

	@SuppressWarnings({"unchecked"})
	void addToOne(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			SingularAssociationAttributeBinding attributeBinding,
			CompositeMapperBuilder mapper,
			String entityName) {
		final String referencedEntityName =  attributeBinding.getReferencedEntityName();

		final IdMappingData idMapping = mainGenerator.getReferencedIdMappingData(
				entityName,
				referencedEntityName,
				propertyAuditingData,
				true
		);

		final String lastPropertyPrefix = MappingTools.createToOneRelationPrefix( propertyAuditingData.getName() );

		// Generating the id mapper for the relation
		final IdMapper relMapper = idMapping.getIdMapper().prefixMappedProperties( lastPropertyPrefix );

		// Storing information about this relation
		boolean insertable = attributeBinding.isIncludedInInsert();
		mainGenerator.getEntitiesConfigurations().get( entityName ).addToOneRelation(
				propertyAuditingData.getName(), referencedEntityName, relMapper,
				insertable, MappingTools.ignoreNotFound( attributeBinding )
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
		final Element properties = (Element) idMapping.getXmlRelationMapping().clone();
		properties.addAttribute( "name", propertyAuditingData.getName() );

		MetadataTools.prefixNamesInPropertyElement(
				properties,
				lastPropertyPrefix,
				MetadataTools.getColumnNameIterator( attributeBinding.getValues().iterator() ),
				false,
				insertable
		);

		// Extracting related id properties from properties tag
		for ( Object o : properties.content() ) {
			final Element element = (Element) o;
			element.setParent( null );
			parent.add( element );
		}

		// Adding mapper for the id
		final PropertyData propertyData = propertyAuditingData.getPropertyData();
		mapper.addComposite(
				propertyData,
				new ToOneIdMapper( relMapper, propertyData, referencedEntityName, nonInsertableFake )
		);
	}

	@SuppressWarnings({"unchecked"})
	void addOneToOneNotOwning(
			PropertyAuditingData propertyAuditingData,
			OneToOneAttributeBinding attributeBinding,
			CompositeMapperBuilder mapper,
			String entityName) {
		final String owningReferencePropertyName =
				attributeBinding.getReferencedAttributeBinding().getAttribute().getName();

		final EntityConfiguration configuration = mainGenerator.getEntitiesConfigurations().get( entityName );
		if ( configuration == null ) {
			throw new MappingException( "An audited relation to a non-audited entity " + entityName + "!" );
		}

		final IdMappingData ownedIdMapping = configuration.getIdMappingData();

		if ( ownedIdMapping == null ) {
			throw new MappingException( "An audited relation to a non-audited entity " + entityName + "!" );
		}

		final String lastPropertyPrefix = MappingTools.createToOneRelationPrefix( owningReferencePropertyName );
		final String referencedEntityName = attributeBinding.getReferencedEntityName();

		// Generating the id mapper for the relation
		final IdMapper ownedIdMapper = ownedIdMapping.getIdMapper().prefixMappedProperties( lastPropertyPrefix );

		// Storing information about this relation
		mainGenerator.getEntitiesConfigurations().get( entityName ).addToOneNotOwningRelation(
				propertyAuditingData.getName(), owningReferencePropertyName, referencedEntityName,
				ownedIdMapper, MappingTools.ignoreNotFound( attributeBinding )
		);

		// Adding mapper for the id
		final PropertyData propertyData = propertyAuditingData.getPropertyData();
		mapper.addComposite(
				propertyData,
				new OneToOneNotOwningMapper( entityName, referencedEntityName, owningReferencePropertyName, propertyData )
		);
	}

	@SuppressWarnings({"unchecked"})
	void addOneToOnePrimaryKeyJoinColumn(
			PropertyAuditingData propertyAuditingData,
			OneToOneAttributeBinding attributeBinding,
			CompositeMapperBuilder mapper,
			String entityName) {
		final String referencedEntityName = attributeBinding.getReferencedEntityName();

		final IdMappingData idMapping = mainGenerator.getReferencedIdMappingData(
				entityName,
				referencedEntityName,
				propertyAuditingData,
				true
		);

		final String lastPropertyPrefix = MappingTools.createToOneRelationPrefix( propertyAuditingData.getName() );

		// Generating the id mapper for the relation
		final IdMapper relMapper = idMapping.getIdMapper().prefixMappedProperties( lastPropertyPrefix );

		// Storing information about this relation
		mainGenerator.getEntitiesConfigurations().get( entityName ).addToOneRelation(
				propertyAuditingData.getName(), referencedEntityName, relMapper, attributeBinding.isIncludedInInsert(),
				MappingTools.ignoreNotFound( attributeBinding )
		);

		// Adding mapper for the id
		final PropertyData propertyData = propertyAuditingData.getPropertyData();
		mapper.addComposite(
				propertyData,
				new OneToOnePrimaryKeyJoinColumnMapper( entityName, referencedEntityName, propertyData )
		);
	}
}
