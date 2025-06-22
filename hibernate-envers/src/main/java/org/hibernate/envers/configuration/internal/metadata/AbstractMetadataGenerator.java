/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Locale;
import java.util.Map;

import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.model.BasicAttribute;
import org.hibernate.envers.boot.model.Identifier;
import org.hibernate.envers.boot.model.JoinedSubclassPersistentEntity;
import org.hibernate.envers.boot.model.PersistentEntity;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.RevisionTypeType;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.strategy.spi.MappingContext;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;

/**
 * Abstract base class for all metadata generator implementations.
 *
 * @author Chris Cranford
 */
public abstract class AbstractMetadataGenerator {

	private final EnversMetadataBuildingContext metadataBuildingContext;
	private final AuditEntityConfigurationRegistry entityConfigurationRegistry;

	public AbstractMetadataGenerator(EnversMetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.entityConfigurationRegistry = metadataBuildingContext.getAuditEntityConfigurationRegistry();
	}

	protected EnversMetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	public Map<String, EntityConfiguration> getAuditedEntityConfigurations() {
		return entityConfigurationRegistry.getAuditedEntityConfigurations();
	}

	public Map<String, EntityConfiguration> getNotAuditedEntityConfigurations() {
		return entityConfigurationRegistry.getNotAuditedEntityConfigurations();
	}

	protected boolean hasAuditedEntityConfiguration(String entityName) {
		return entityConfigurationRegistry.hasAuditedEntityConfiguration( entityName );
	}

	protected boolean hasNotAuditedEntityConfiguration(String entityName) {
		return entityConfigurationRegistry.hasNotAuditedEntityConfiguration( entityName );
	}

	protected EntityConfiguration getAuditedEntityConfiguration(String entityName) {
		return entityConfigurationRegistry.getAuditedEntityConfiguration( entityName );
	}

	protected EntityConfiguration getNotAuditedEntityConfiguration(String entityName) {
		return entityConfigurationRegistry.getNotAuditedEntityConfiguration( entityName );
	}

	protected void addAuditedEntityConfiguration(String entityName, EntityConfiguration entityConfiguration) {
		entityConfigurationRegistry.addAuditedEntityConfiguration( entityName, entityConfiguration );
	}

	protected void addNotAuditedEntityConfiguration(String entityName, EntityConfiguration entityConfiguration) {
		entityConfigurationRegistry.addNotAuditedEntityConfiguration( entityName, entityConfiguration );
	}

	protected String getSchemaName(String schemaFromAnnotation, Table table) {
		String schemaName = schemaFromAnnotation;
		if ( StringTools.isEmpty( schemaName ) ) {
			schemaName = metadataBuildingContext.getConfiguration().getDefaultSchemaName();
			if ( StringTools.isEmpty( schemaName ) ) {
				schemaName = table.getSchema();
			}
		}
		return schemaName;
	}

	protected String getCatalogName(String catalogFromAnnotation, Table table) {
		String catalogName = catalogFromAnnotation;
		if ( StringTools.isEmpty( catalogName ) ) {
			catalogName = metadataBuildingContext.getConfiguration().getDefaultCatalogName();
			if ( StringTools.isEmpty( catalogName ) ) {
				catalogName = table.getCatalog();
			}
		}
		return catalogName;
	}

	protected void addRevisionInfoRelationToIdentifier(Identifier identifier) {
		final Configuration configuration = metadataBuildingContext.getConfiguration();
		identifier.addAttribute( configuration.getRevisionInfo().getRevisionInfoRelationMapping() );
	}

	protected void addAuditStrategyAdditionalColumnsToEntity(PersistentEntity entity) {
		metadataBuildingContext.getConfiguration().getAuditStrategy().addAdditionalColumns(
				new MappingContext(
						entity,
						metadataBuildingContext.getConfiguration(),
						metadataBuildingContext.getConfiguration().getRevisionTypePropertyType(),
						metadataBuildingContext.getConfiguration().getRevisionInfo().getRevisionInfoClass().getName(),
						false
				)
		);
	}

	protected void addAuditStrategyRevisionEndTimestampOnly(PersistentEntity entity) {
		if ( ( entity instanceof JoinedSubclassPersistentEntity ) ) {
			// Only joined subclass entities are allowed to add revision timestamp to associated tables
			metadataBuildingContext.getConfiguration().getAuditStrategy().addAdditionalColumns(
					new MappingContext(
							entity,
							metadataBuildingContext.getConfiguration(),
							metadataBuildingContext.getConfiguration().getRevisionTypePropertyType(),
							metadataBuildingContext.getConfiguration().getRevisionInfo().getRevisionInfoClass().getName(),
							true
					)
			);
		}
	}

	protected void addRevisionTypeToAttributeContainer(AttributeContainer container, boolean key) {
		container.addAttribute(
				new BasicAttribute(
						metadataBuildingContext.getConfiguration().getRevisionTypePropertyName(),
						metadataBuildingContext.getConfiguration().getRevisionTypePropertyType(),
						true,
						key,
						RevisionTypeType.class.getName()
				)
		);
	}

	/**
	 * Reads the id mapping data of a referenced entity.
	 *
	 * @param entityName Name of the entity which is the source of the relation.
	 * @param referencedEntityName Name of the entity which is the target of the relation.
	 * @param propertyAuditingData Auditing data of the property that is the source of the relation.
	 * @param allowNotAuditedTarget Are not-audited target entities allowed.
	 *
	 * @return The id mapping data of the related entity.
	 *
	 * @throws EnversMappingException If a relation from an audited to a non-audited entity is detected, which is not
	 * mapped using {@link RelationTargetAuditMode#NOT_AUDITED}.
	 */
	protected IdMappingData getReferencedIdMappingData(
			String entityName,
			String referencedEntityName,
			PropertyAuditingData propertyAuditingData,
			boolean allowNotAuditedTarget) {
		EntityConfiguration configuration = getAuditedEntityConfiguration( referencedEntityName );
		if ( configuration == null ) {
			configuration = getNotAuditedEntityConfiguration( referencedEntityName );
			if ( configuration == null || !allowNotAuditedTarget || !isRelationNotAudited( propertyAuditingData ) ) {
				throw new EnversMappingException(
						String.format(
								"An audited relation from %s.%s to a not audited entity %s! %s",
								entityName,
								propertyAuditingData.getName(),
								referencedEntityName,
								allowNotAuditedTarget
										? "Such a mapping is possible but requires using @Audited(targetAuditMode = NOT_AUDITED)."
										: ""
						)
				);
			}
		}

		return configuration.getIdMappingData();
	}

	protected void throwUnsupportedTypeException(Type type, String entityName, String propertyName) {
		throw new EnversMappingException(
				String.format(
						Locale.ENGLISH,
						"Type not supported for auditing: %s, on entity %s, property '%s'.",
						type.getClass().getName(),
						entityName,
						propertyName
				)
		);
	}

	private boolean isRelationNotAudited(PropertyAuditingData propertyAuditingData) {
		return RelationTargetAuditMode.NOT_AUDITED.equals( propertyAuditingData.getRelationTargetAuditMode() );
	}
}
