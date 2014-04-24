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
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.EmbeddedIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.MultipleIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.SimpleIdMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.SingleIdMapper;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

/**
 * Generates metadata for primary identifiers (ids) of versions entities.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public final class IdMetadataGenerator {
	private final AuditConfiguration.AuditConfigurationContext context;
	private final AuditMetadataGenerator mainGenerator;

	IdMetadataGenerator(AuditConfiguration.AuditConfigurationContext context, AuditMetadataGenerator auditMetadataGenerator) {
		this.context = context;
		mainGenerator = auditMetadataGenerator;
	}

	@SuppressWarnings({"unchecked"})
	private boolean addIdProperties(
			Element parent,
			EmbeddableBinding embeddableBinding,
			SimpleMapperBuilder mapper,
			boolean key,
			boolean audited) {
		//if ( embeddedAttributeBinding.getAttribute().isSynthetic() ) {
		//	return true;
		//}
		for ( AttributeBinding attributeBinding : embeddableBinding.attributeBindings() ) {
			final Type propertyType = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
			final boolean added;
			if ( propertyType instanceof ManyToOneType ) {
				added = mainGenerator.getBasicMetadataGenerator().addManyToOne(
						parent,
						getIdPersistentPropertyAuditingData( attributeBinding ),
						(ManyToOneAttributeBinding) attributeBinding,
						mapper
				);
			}
			else {
				final SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
				// Last but one parameter: ids are always insertable
				added = mainGenerator.getBasicMetadataGenerator().addBasic(
						parent,
						getIdPersistentPropertyAuditingData( singularAttributeBinding ),
						singularAttributeBinding.getHibernateTypeDescriptor(),
						singularAttributeBinding.getValues(),
						singularAttributeBinding.isIncludedInInsert(),
						mapper,
						key
				);
			}
			if ( !added ) {
				// If the entity is audited, and a non-supported id component is used, throwing an exception.
				// If the entity is not audited, then we simply don't support this entity, even in
				// target relation mode not audited.
				if ( audited ) {
					throw new MappingException( "Type not supported: " + propertyType.getClass().getName() );
				}
				else {
					return false;
				}
			}
		}

		return true;
	}

	@SuppressWarnings({"unchecked"})
	IdMappingData addId(EntityBinding entityBinding, boolean audited) {
		// Xml mapping which will be used for relations
		final Element relIdMapping = new DefaultElement( "properties" );
		// Xml mapping which will be used for the primary key of the versions table
		final Element origIdMapping = new DefaultElement( "composite-id" );

		//final Property idProp = pc.getIdentifierProperty();
		//final Component idMapper = pc.getIdentifierMapper();

		// Checking if the id mapping is supported
		//if ( idMapper == null && idProp == null ) {
		//	return null;
		//}

		final EntityIdentifier idInfo = entityBinding.getHierarchyDetails().getEntityIdentifier();
		SimpleIdMapperBuilder mapper;
		if ( idInfo.definesIdClass() ) {
			// Multiple id
			final EntityIdentifier.NonAggregatedCompositeIdentifierBinding idBinding =
					(EntityIdentifier.NonAggregatedCompositeIdentifierBinding) idInfo.getEntityIdentifierBinding();
			final Class componentClass = context.getClassLoaderService().classForName(
					idBinding.getIdClassMetadata().getIdClassType().getName().toString()
			);

			mapper = new MultipleIdMapper( componentClass );
			if ( !addIdProperties(
					relIdMapping,
					idBinding.getVirtualEmbeddableBinding(),
					mapper,
					false,
					audited
			) ) {
				return null;
			}

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			if ( !addIdProperties(
					origIdMapping,
					idBinding.getVirtualEmbeddableBinding(),
					null,
					true,
					audited
			) ) {
				return null;
			}
		}
		else if ( idInfo.getNature() == EntityIdentifierNature.AGGREGATED_COMPOSITE ) {
			// Embeddable id
			final EntityIdentifier.AggregatedCompositeIdentifierBinding idBinding =
					(EntityIdentifier.AggregatedCompositeIdentifierBinding) idInfo.getEntityIdentifierBinding();

			// TODO: get rid of classloading.
			final Class embeddableClass = context.getClassLoaderService().classForName(
					idBinding.getAttributeBinding().getEmbeddableBinding().getTypeDescriptor().getName().toString()
			);
			mapper = new EmbeddedIdMapper( getIdPropertyData( idBinding.getAttributeBinding() ), embeddableClass );
			if ( !addIdProperties(
					relIdMapping,
					idBinding.getAttributeBinding().getEmbeddableBinding(),
					mapper,
					false,
					audited
			) ) {
				return null;
			}

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			if ( !addIdProperties(
					origIdMapping,
					idBinding.getAttributeBinding().getEmbeddableBinding(),
					null,
					true,
					audited
			) ) {
				return null;
			}
		}
		else {
			final EntityIdentifier.SimpleIdentifierBinding idBinding =
					(EntityIdentifier.SimpleIdentifierBinding) idInfo.getEntityIdentifierBinding();

			// Single id
			mapper = new SingleIdMapper();

			// Last but one parameter: ids are always insertable
			mainGenerator.getBasicMetadataGenerator().addBasic(
					relIdMapping,
					getIdPersistentPropertyAuditingData( idBinding.getAttributeBinding() ),
					idBinding.getAttributeBinding().getHibernateTypeDescriptor(),
					idBinding.getAttributeBinding().getValues(),
					idBinding.getAttributeBinding().isIncludedInInsert(),
					mapper,
					false
			);

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			mainGenerator.getBasicMetadataGenerator().addBasic(
					origIdMapping,
					getIdPersistentPropertyAuditingData( idBinding.getAttributeBinding() ),
					idBinding.getAttributeBinding().getHibernateTypeDescriptor(),
					idBinding.getAttributeBinding().getValues(),
					idBinding.getAttributeBinding().isIncludedInInsert(),
					null,
					true
			);
		}

		origIdMapping.addAttribute( "name", context.getAuditEntitiesConfiguration().getOriginalIdPropName() );

		// Adding a relation to the revision entity (effectively: the "revision number" property)
		mainGenerator.addRevisionInfoRelation( origIdMapping );

		return new IdMappingData( mapper, origIdMapping, relIdMapping );
	}

	private PropertyData getIdPropertyData(SingularAttributeBinding idAttributeBinding) {
		return new PropertyData(
				idAttributeBinding.getAttribute().getName(),
				idAttributeBinding.getAttribute().getName(),
				idAttributeBinding.getPropertyAccessorName(),
				ModificationStore.FULL
		);
	}

	private PropertyAuditingData getIdPersistentPropertyAuditingData(AttributeBinding property) {
		return new PropertyAuditingData(
				property.getAttribute().getName(), property.getPropertyAccessorName(),
				ModificationStore.FULL, RelationTargetAuditMode.AUDITED, null, null, false
		);
	}
}
