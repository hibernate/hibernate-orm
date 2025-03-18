/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.model.Column;
import org.hibernate.envers.boot.model.Join;
import org.hibernate.envers.boot.model.JoinAwarePersistentEntity;
import org.hibernate.envers.boot.model.PersistentEntity;
import org.hibernate.envers.boot.model.RootPersistentEntity;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.ExtendedPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.SubclassPropertyMapper;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.generator.internal.GeneratedGeneration;

import org.jboss.logging.Logger;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 * @author Tomasz Bech
 * @author Stephanie Pau at Markit Group Plc
 * @author Hern&aacute;n Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public final class AuditMetadataGenerator extends AbstractMetadataGenerator {

	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			EnversMessageLogger.class,
			AuditMetadataGenerator.class.getName()
	);

	private final EnversMetadataBuildingContext metadataBuildingContext;
	private final Configuration configuration;
	private final PersistentEntityInstantiator entityInstantiator;

	private final IdMetadataGenerator idMetadataGenerator;
	private final ValueMetadataGenerator valueMetadataGenerator;

	// Map entity name -> (join descriptor -> element describing the "versioned" join)
	private final Map<String, Map<org.hibernate.mapping.Join, Join>> entityJoins;

	public AuditMetadataGenerator(EnversMetadataBuildingContext metadataBuildingContext) {
		super( metadataBuildingContext );
		this.metadataBuildingContext = metadataBuildingContext;
		this.configuration = metadataBuildingContext.getConfiguration();

		// Create generators
		BasicMetadataGenerator basicMetadataGenerator = new BasicMetadataGenerator();
		this.idMetadataGenerator = new IdMetadataGenerator( metadataBuildingContext, basicMetadataGenerator );
		this.valueMetadataGenerator = new ValueMetadataGenerator( metadataBuildingContext, basicMetadataGenerator );

		this.entityInstantiator = new PersistentEntityInstantiator( configuration );

		entityJoins = new HashMap<>();
	}

	private void addProperties(
			AttributeContainer attributeContainer,
			Iterator<Property> properties,
			CompositeMapperBuilder currentMapper,
			ClassAuditingData auditingData,
			String entityName,
			EntityMappingData mappingData,
			boolean firstPass) {
		while ( properties.hasNext() ) {
			final Property property = properties.next();
			final String propertyName = property.getName();
			final PropertyAuditingData propertyAuditingData = auditingData.getPropertyAuditingData( propertyName );
			if ( propertyAuditingData != null ) {
				// HHH-10246
				// Verifies if a mapping exists using a @JoinColumn against a @NaturalId field
				// and if so, this eliminates the mapping property as it isn't needed.
				if ( property instanceof SyntheticProperty ) {
					if ( property.getValue().isAlternateUniqueKey() ) {
						continue;
					}
				}
				valueMetadataGenerator.addValue(
						attributeContainer,
						property.getValue(),
						property.getPropertyAccessStrategy(),
						currentMapper,
						entityName,
						mappingData,
						propertyAuditingData,
						isPropertyInsertable( property ),
						firstPass,
						true
				);
			}
		}
	}

	private boolean isPropertyInsertable(Property property) {
		if ( !property.isInsertable() ) {
			// TODO: this is now broken by changes to generators
			final GeneratorCreator generation = property.getValueGeneratorCreator();
			if ( generation instanceof GeneratedGeneration) {
				final GeneratedGeneration valueGeneration = (GeneratedGeneration) generation;
				if ( valueGeneration.generatesOnInsert() ) {
					return true;
				}
			}
		}
		return property.isInsertable();
	}

	private boolean checkPropertiesAudited(Iterator<Property> properties, ClassAuditingData auditingData) {
		while ( properties.hasNext() ) {
			final Property property = properties.next();
			final String propertyName = property.getName();
			final PropertyAuditingData propertyAuditingData = auditingData.getPropertyAuditingData( propertyName );
			if ( propertyAuditingData == null ) {
				return false;
			}
		}
		return true;
	}

	private void createJoins(PersistentClass persistentClass, JoinAwarePersistentEntity entity, ClassAuditingData auditingData) {
		final Iterator<org.hibernate.mapping.Join> joins = persistentClass.getJoins().iterator();
		final Map<org.hibernate.mapping.Join, Join> joinElements = new HashMap<>();
		entityJoins.put( persistentClass.getEntityName(), joinElements );

		while ( joins.hasNext() ) {
			org.hibernate.mapping.Join join = joins.next();

			// Checking if all of the join properties are audited
			if ( !checkPropertiesAudited( join.getProperties().iterator(), auditingData ) ) {
				continue;
			}

			// Determining the table name. If there is no entry in the dictionary, just constructing the table name
			// as if it was an entity (by appending/prepending configured strings).
			final String originalTableName = join.getTable().getName();
			String auditTableName = auditingData.getSecondaryTableDictionary().get( originalTableName );
			if ( auditTableName == null ) {
				auditTableName = configuration.getAuditEntityName( originalTableName );
			}

			final AuditTable auditTable = auditingData.getAuditTable();
			final Join joinElement = new Join(
					getCatalogName( auditTable.catalog(), join.getTable() ),
					getSchemaName( auditTable.schema(), join.getTable() ),
					auditTableName
			);
			joinElements.put( join, joinElement );

			// HHH-8305 - Fix case when join is considered optional.
			if ( join.isOptional() ) {
				joinElement.setOptional( true );
			}

			// HHH-8305 - Fix case when join is the inverse side of a mapping.
			if ( join.isInverse() ) {
				joinElement.setInverse( true );
			}

			joinElement.addKeyColumnsFromValue( join.getKey() );
			joinElement.addKeyColumn( new Column( configuration.getRevisionFieldName() ) );

			entity.addJoin( joinElement );
		}
	}

	private void addJoins(
			PersistentClass persistentClass,
			CompositeMapperBuilder currentMapper,
			ClassAuditingData auditingData,
			String entityName,
			EntityMappingData mappingData,
			boolean firstPass) {
		final Iterator<org.hibernate.mapping.Join> joins = persistentClass.getJoins().iterator();

		while ( joins.hasNext() ) {
			final org.hibernate.mapping.Join join = joins.next();
			final Join entityJoin = entityJoins.get( entityName ).get( join );

			if ( entityJoin != null ) {
				addProperties(
						entityJoin,
						join.getProperties().iterator(),
						currentMapper,
						auditingData,
						entityName,
						mappingData,
						firstPass
				);
			}
		}
	}

	private MappingDescriptor generateMappingData(
			PersistentClass persistentClass,
			AuditTableData auditTableData,
			IdMappingData idMapper) {

		final PersistentEntity entity;
		final ExtendedPropertyMapper propertyMapper;
		final String parentEntityName;

		final InheritanceType inheritanceType = InheritanceType.get( persistentClass );
		if ( InheritanceType.NONE == inheritanceType ) {
			entity = entityInstantiator.instantiate( persistentClass, auditTableData );
			propertyMapper = new MultiPropertyMapper();
			parentEntityName = null;

			// Add the id mapping
			( (RootPersistentEntity) entity ).setIdentifier( idMapper.getIdentifier() );

			// Adding the "revision type" property
			addRevisionTypeToAttributeContainer( entity, false );
			addAuditStrategyAdditionalColumnsToEntity( entity );
		}
		else {
			parentEntityName = persistentClass.getSuperclass().getEntityName();
			final EntityConfiguration parentConfiguration = getAuditedEntityConfiguration( parentEntityName );
			if ( parentConfiguration == null ) {
				throw new EnversMappingException(
						String.format(
								Locale.ENGLISH,
								"Entity '%s' is audited, but its superclass '%s' is not.",
								persistentClass.getEntityName(),
								parentEntityName
						)
				);
			}

			entity = entityInstantiator.instantiate( persistentClass, auditTableData );
			propertyMapper = new SubclassPropertyMapper(
					new MultiPropertyMapper(),
					parentConfiguration.getPropertyMapper()
			);
		}

		return new MappingDescriptor(entity, propertyMapper, parentEntityName);
	}

	public void generateFirstPass(ClassAuditingData auditingData, EntityMappingData mappingData, boolean isAudited) {
		final PersistentClass persistentClass = auditingData.getPersistentClass();

		if ( !isAudited ) {
			final String entityName = persistentClass.getEntityName();
			final IdMappingData idMapper = idMetadataGenerator.addIdAndGetMappingData( persistentClass, false );

			if ( idMapper == null ) {
				// Unsupported id mapping, e.g. key-many-to-one. If the entity is used in auditing, an exception
				// will be thrown later on.
				LOG.debugf(
						"Unable to create auditing id mapping for entity %s, because of an unsupported Hibernate id mapping (e.g. key-many-to-one)",
						entityName
				);
				return;
			}

			final ExtendedPropertyMapper propertyMapper = null;
			final String parentEntityName = null;
			final EntityConfiguration entityCfg = new EntityConfiguration(
					entityName,
					persistentClass.getClassName(),
					idMapper,
					propertyMapper,
					parentEntityName
			);
			addNotAuditedEntityConfiguration( entityName, entityCfg );
			return;
		}

		final String entityName = persistentClass.getEntityName();
		LOG.debugf( "Generating first-pass auditing mapping for entity %s", entityName );

		final String auditEntityName = configuration.getAuditEntityName( entityName );
		final String auditTableName = configuration.getAuditTableName( entityName, persistentClass.getTable().getName() );

		// Registering the audit entity name, now that it is known
		metadataBuildingContext.getAuditEntityNameRegistry().register( auditEntityName );

		final AuditTableData auditTableData = new AuditTableData(
				auditEntityName,
				auditTableName,
				getSchemaName( auditingData.getAuditTable().schema(), persistentClass.getTable() ),
				getCatalogName( auditingData.getAuditTable().catalog(), persistentClass.getTable() )
		);

		// Generating a mapping for the id
		final IdMappingData idMapper = idMetadataGenerator.addIdAndGetMappingData( persistentClass, true );

		// Reading the mapping data based on inheritance model
		final MappingDescriptor mappingDescriptor = generateMappingData( persistentClass, auditTableData, idMapper );

		final PersistentEntity entity = mappingDescriptor.getEntity();
		final ExtendedPropertyMapper propertyMapper = mappingDescriptor.getMapper();
		final String parentEntityName = mappingDescriptor.getParentEntityName();

		// Mapping unjoined properties
		LOG.infof( "Adding properties for entity: %s", persistentClass.getEntityName() );
		addProperties(
				entity,
				persistentClass.getUnjoinedProperties().iterator(),
				propertyMapper,
				auditingData,
				persistentClass.getEntityName(),
				mappingData,
				true
		);

		// Creating and mapping joins (first pass); if applicable
		if ( entity.isJoinAware() ) {
			final JoinAwarePersistentEntity joinAwareEntity = (JoinAwarePersistentEntity) entity;
			createJoins( persistentClass, joinAwareEntity, auditingData );
			addJoins( persistentClass, propertyMapper, auditingData, persistentClass.getEntityName(), mappingData, true );
		}

		// HHH-7940 - New synthetic property support for @IndexColumn/@OrderColumn dynamic properties
		addSynthetics( entity, auditingData, propertyMapper, mappingData, persistentClass.getEntityName() );

		if ( !configuration.isRevisionEndTimestampUseLegacyPlacement() ) {
			addAuditStrategyRevisionEndTimestampOnly( entity );
		}

		mappingData.addMapping( entity );

		// Storing the generated configuration
		final EntityConfiguration entityCfg = new EntityConfiguration(
				auditEntityName,
				persistentClass.getClassName(),
				idMapper,
				propertyMapper,
				parentEntityName
		);

		addAuditedEntityConfiguration( persistentClass.getEntityName(), entityCfg );
	}

	private void addSynthetics(
			PersistentEntity entity,
			ClassAuditingData auditingData,
			CompositeMapperBuilder currentMapper,
			EntityMappingData mappingData,
			String entityName) {
		for ( PropertyAuditingData propertyAuditingData : auditingData.getSyntheticProperties() ) {
			valueMetadataGenerator.addValue(
					entity,
					propertyAuditingData.getValue(),
					null,
					currentMapper,
					entityName,
					mappingData,
					propertyAuditingData,
					true,
					true,
					false
			);
		}
	}

	public void generateSecondPass(ClassAuditingData auditingData, EntityMappingData mappingData) {
		final PersistentClass persistentClass = auditingData.getPersistentClass();
		final String entityName = persistentClass.getEntityName();
		LOG.debugf( "Generating second-pass auditing mapping for entity %s", entityName );

		final CompositeMapperBuilder propertyMapper = getAuditedEntityConfiguration( entityName ).getPropertyMapper();

		// HHH-11748 - Generate a second pass for identifiers
		// This is useful for situations where @Id point to @ManyToOne and @OneToOne associations.
		idMetadataGenerator.generateSecondPass( entityName, persistentClass );

		// Mapping unjoined properties
		addProperties(
				mappingData.getEntityDefinition(),
				persistentClass.getUnjoinedProperties().iterator(),
				propertyMapper,
				auditingData,
				entityName,
				mappingData,
				false
		);

		// Mapping joins (second pass)
		addJoins( persistentClass, propertyMapper, auditingData, entityName, mappingData, false );
	}

	private static class MappingDescriptor {
		private final PersistentEntity entity;
		private final ExtendedPropertyMapper mapper;
		private final String parentEntityName;

		public MappingDescriptor(PersistentEntity entity, ExtendedPropertyMapper mapper, String parentEntityName) {
			this.entity = entity;
			this.mapper = mapper;
			this.parentEntityName = parentEntityName;
		}

		public PersistentEntity getEntity() {
			return entity;
		}

		public ExtendedPropertyMapper getMapper() {
			return mapper;
		}

		public String getParentEntityName() {
			return parentEntityName;
		}
	}
}
