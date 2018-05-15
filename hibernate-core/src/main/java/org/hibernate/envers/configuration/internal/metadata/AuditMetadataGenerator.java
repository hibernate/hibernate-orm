/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.RevisionTypeType;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.ExtendedPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.SubclassPropertyMapper;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.internal.tools.Triple;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.strategy.spi.MappingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.GeneratedValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import org.dom4j.Element;

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
public final class AuditMetadataGenerator {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			AuditMetadataGenerator.class.getName()
	);

	private final InFlightMetadataCollector metadata;
	private final ServiceRegistry serviceRegistry;
	private final AuditMetadataBuildingOptions options;
	private final Element revisionInfoRelationMapping;

	private final ClassLoaderService classLoaderService;

	/*
	 * Generators for different kinds of property values/types.
	 */
	private final BasicMetadataGenerator basicMetadataGenerator;
	private final ComponentMetadataGenerator componentMetadataGenerator;
	private final IdMetadataGenerator idMetadataGenerator;
	private final ToOneRelationMetadataGenerator toOneRelationMetadataGenerator;

	/*
	 * Here information about already generated mappings will be accumulated.
	 */
	private final Map<String, EntityConfiguration> entitiesConfigurations;
	private final Map<String, EntityConfiguration> notAuditedEntitiesConfigurations;

	private final AuditEntityNameRegister auditEntityNameRegister;

	// Map entity name -> (join descriptor -> element describing the "versioned" join)
	private final Map<String, Map<Join, Element>> entitiesJoins;

	public AuditMetadataGenerator(
			InFlightMetadataCollector metadata,
			ServiceRegistry serviceRegistry,
			AuditMetadataBuildingOptions options,
			Element revisionInfoRelationMapping,
			AuditEntityNameRegister auditEntityNameRegister) {
		this.metadata = metadata;
		this.serviceRegistry = serviceRegistry;
		this.options = options;
		this.revisionInfoRelationMapping = revisionInfoRelationMapping;

		this.basicMetadataGenerator = new BasicMetadataGenerator();
		this.componentMetadataGenerator = new ComponentMetadataGenerator( this );
		this.idMetadataGenerator = new IdMetadataGenerator( this );
		this.toOneRelationMetadataGenerator = new ToOneRelationMetadataGenerator( this );

		this.auditEntityNameRegister = auditEntityNameRegister;

		entitiesConfigurations = new HashMap<>();
		notAuditedEntitiesConfigurations = new HashMap<>();
		entitiesJoins = new HashMap<>();

		classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	public InFlightMetadataCollector getMetadata() {
		return metadata;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	/**
	 * Clones the revision info relation mapping, so that it can be added to other mappings. Also, the name of
	 * the property and the column are set properly.
	 *
	 * @return A revision info mapping, which can be added to other mappings (has no parent).
	 */
	private Element cloneAndSetupRevisionInfoRelationMapping() {
		final Element revMapping = (Element) revisionInfoRelationMapping.clone();
		revMapping.addAttribute( "name", options.getRevisionFieldName() );
		if ( options.isCascadeDeleteRevisionEnabled() ) {
			revMapping.addAttribute( "on-delete", "cascade" );
		}

		MetadataTools.addOrModifyColumn( revMapping, options.getRevisionFieldName() );

		return revMapping;
	}

	void addRevisionInfoRelation(Element anyMapping) {
		anyMapping.add( cloneAndSetupRevisionInfoRelationMapping() );
	}

	void addRevisionType(Element anyMapping, Element anyMappingEnd) {
		addRevisionType( anyMapping, anyMappingEnd, false );
	}

	void addRevisionType(Element anyMapping, Element anyMappingEnd, boolean isKey) {
		final Element revTypeProperty = MetadataTools.addProperty(
				anyMapping,
				options.getRevisionTypePropName(),
				options.getRevisionTypePropType(),
				true,
				isKey
		);
		revTypeProperty.addAttribute( "type", RevisionTypeType.INSTANCE.getJavaTypeDescriptor().getTypeName() );
	}

	void addAdditionalColumns(Element anyMapping) {
		final AuditStrategy strategy = options.getAuditStrategy();
		strategy.addAdditionalColumns(
				new MappingContext(
						anyMapping,
						revisionInfoRelationMapping,
						options,
						metadata.getDatabase().getDialect()
				)
		);
	}

	private void addEndRevisionTimestamp(Element mapping) {
		if ( options.getAuditStrategy() instanceof ValidityAuditStrategy ) {
			if ( options.isRevisionEndTimestampEnabled() ) {
				final Type timestampType = metadata.getTypeConfiguration().getBasicTypeRegistry().getBasicType( Timestamp.class );
				final Element property = MetadataTools.addProperty(
						mapping,
						options.getRevisionEndTimestampFieldName(),
						timestampType.getJavaTypeDescriptor().getTypeName(),
						true,
						true,
						false
				);
				MetadataTools.addColumn(
						property,
						options.getRevisionEndTimestampFieldName(),
						null,
						null,
						null,
						null,
						null,
						null
				);
			}
		}
	}

	private void addValueInFirstPass(
			Element parent,
			ValueMapping value,
			CompositeMapperBuilder currentMapper,
			String entityName,
			EntityXmlMappingData xmlMappingData,
			PropertyAuditingData propertyAuditingData,
			boolean insertable,
			boolean processModifiedFlag) {
		final boolean isBasic = basicMetadataGenerator.addBasic(
				parent,
				propertyAuditingData,
				value,
				currentMapper,
				insertable,
				false
		);

		if ( isBasic ) {
			// The property was mapped by the basic generator.
		}
		else if ( value instanceof Component ) {
			componentMetadataGenerator.addComponent(
					parent, propertyAuditingData, value, currentMapper,
					entityName, xmlMappingData, true
			);
		}
		else {
			if ( !processedInSecondPass( value ) ) {
				// If we got here in the first pass, it means the basic mapper didn't map it, and none of the
				// above branches either.
				throwUnsupportedTypeException( value, entityName, propertyAuditingData.getName() );
			}
			return;
		}
		addModifiedFlagIfNeeded( parent, propertyAuditingData, processModifiedFlag );
	}

	private boolean processedInSecondPass(ValueMapping value) {
		return value instanceof Component ||
				value instanceof ManyToOne ||
				value instanceof OneToOne ||
				value instanceof Collection;
	}

	private void addValueInSecondPass(
			Element parent,
			ValueMapping value,
			CompositeMapperBuilder currentMapper,
			String entityName,
			EntityXmlMappingData xmlMappingData,
			PropertyAuditingData propertyAuditingData,
			boolean insertable,
			boolean processModifiedFlag) {
		if ( value instanceof Component ) {
			componentMetadataGenerator.addComponent(
					parent,
					propertyAuditingData,
					value,
					currentMapper,
					entityName,
					xmlMappingData,
					false
			);
			// mod flag field has been already generated in first pass
			return;
		}
		else if ( value instanceof ManyToOne ) {
			toOneRelationMetadataGenerator.addToOne(
					parent,
					propertyAuditingData,
					value,
					currentMapper,
					entityName,
					insertable
			);
		}
		else if ( value instanceof OneToOne ) {
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
		else if ( value instanceof Collection ) {
			final CollectionMetadataGenerator collectionMetadataGenerator = new CollectionMetadataGenerator(
					this,
					(Collection) value,
					currentMapper,
					entityName,
					xmlMappingData,
					propertyAuditingData
			);
			collectionMetadataGenerator.addCollection();
		}
		else {
			return;
		}
		addModifiedFlagIfNeeded( parent, propertyAuditingData, processModifiedFlag );
	}

	private void addModifiedFlagIfNeeded(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			boolean processModifiedFlag) {
		if ( processModifiedFlag && propertyAuditingData.isUsingModifiedFlag() ) {
			MetadataTools.addModifiedFlagProperty(
					parent,
					propertyAuditingData.getName(),
					options.getModifiedFlagSuffix(),
					propertyAuditingData.getModifiedFlagName()
			);
		}
	}

	void addValue(
			Element parent, ValueMapping value, CompositeMapperBuilder currentMapper, String entityName,
			EntityXmlMappingData xmlMappingData, PropertyAuditingData propertyAuditingData,
			boolean insertable, boolean firstPass, boolean processModifiedFlag) {
		if ( firstPass ) {
			addValueInFirstPass(
					parent, value, currentMapper, entityName,
					xmlMappingData, propertyAuditingData, insertable, processModifiedFlag
			);
		}
		else {
			addValueInSecondPass(
					parent, value, currentMapper, entityName,
					xmlMappingData, propertyAuditingData, insertable, processModifiedFlag
			);
		}
	}

	private void addProperties(
			Element parent,
			Iterator<Property> properties,
			CompositeMapperBuilder currentMapper,
			ClassAuditingData auditingData,
			String entityName,
			EntityXmlMappingData xmlMappingData,
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
				addValue(
						parent,
						property.getValue(),
						currentMapper,
						entityName,
						xmlMappingData,
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
			final ValueGeneration generation = property.getValueGenerationStrategy();
			if ( generation instanceof GeneratedValueGeneration ) {
				final GeneratedValueGeneration valueGeneration = (GeneratedValueGeneration) generation;
				if ( GenerationTiming.INSERT == valueGeneration.getGenerationTiming()
					|| GenerationTiming.ALWAYS == valueGeneration.getGenerationTiming() ) {
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

	protected String getSchema(String schemaFromAnnotation, MappedTable table) {
		// Get the schema from the annotation ...
		String schema = schemaFromAnnotation;
		// ... if empty, try using the default ...
		if ( StringTools.isEmpty( schema ) ) {
			schema = options.getDefaultSchemaName();

			// ... if still empty, use the same as the normal table.
			if ( StringTools.isEmpty( schema ) ) {
				schema = table.getSchema();
			}
		}

		return schema;
	}

	protected String getCatalog(String catalogFromAnnotation, MappedTable table) {
		// Get the catalog from the annotation ...
		String catalog = catalogFromAnnotation;
		// ... if empty, try using the default ...
		if ( StringTools.isEmpty( catalog ) ) {
			catalog = options.getDefaultCatalogName();

			// ... if still empty, use the same as the normal table.
			if ( StringTools.isEmpty( catalog ) ) {
				catalog = table.getCatalog();
			}
		}

		return catalog;
	}

	@SuppressWarnings({"unchecked"})
	private void createJoins(PersistentClass pc, Element parent, ClassAuditingData auditingData) {
		final Iterator<Join> joins = pc.getJoinIterator();
		final Map<Join, Element> joinElements = new HashMap<>();
		entitiesJoins.put( pc.getEntityName(), joinElements );

		while ( joins.hasNext() ) {
			Join join = joins.next();

			// Checking if all of the join properties are audited
			if ( !checkPropertiesAudited( join.getPropertyIterator(), auditingData ) ) {
				continue;
			}

			// Determining the table name. If there is no entry in the dictionary, just constructing the table name
			// as if it was an entity (by appending/prepending configured strings).
			final String originalTableName = join.getTable().getName();
			String auditTableName = auditingData.getSecondaryTableDictionary().get( originalTableName );
			if ( auditTableName == null ) {
				auditTableName = options.getAuditEntityName( originalTableName );
			}

			final String schema = getSchema( auditingData.getAuditTable().schema(), join.getTable() );
			final String catalog = getCatalog( auditingData.getAuditTable().catalog(), join.getTable() );

			final Element joinElement = MetadataTools.createJoin( parent, auditTableName, schema, catalog );
			joinElements.put( join, joinElement );

			// HHH-8305 - Fix case when join is considered optional.
			if ( join.isOptional() ) {
				joinElement.addAttribute( "optional", "true" );
			}

			// HHH-8305 - Fix case when join is the inverse side of a mapping.
			if ( join.isInverse() ) {
				joinElement.addAttribute( "inverse", "true" );
			}

			final Element joinKey = joinElement.addElement( "key" );
			MetadataTools.addColumns( joinKey, join.getKey().getMappedColumns() );
			MetadataTools.addColumn( joinKey, options.getRevisionFieldName(), null, null, null, null, null, null );
		}
	}

	@SuppressWarnings({"unchecked"})
	private void addJoins(
			PersistentClass pc,
			CompositeMapperBuilder currentMapper,
			ClassAuditingData auditingData,
			String entityName,
			EntityXmlMappingData xmlMappingData,
			boolean firstPass) {
		pc.getJoins().forEach( join -> {
			final Element joinElement = entitiesJoins.get( entityName ).get( join );

			if ( joinElement != null ) {
				addProperties(
						joinElement,
						join.getPropertyIterator(),
						currentMapper,
						auditingData,
						entityName,
						xmlMappingData,
						firstPass
				);
			}
		} );
	}

	@SuppressWarnings({"unchecked"})
	private Triple<Element, ExtendedPropertyMapper, String> generateMappingData(
			PersistentClass pc, EntityXmlMappingData xmlMappingData, AuditTableData auditTableData,
			IdMappingData idMapper) {
		final Element classMapping = MetadataTools.createEntity(
				xmlMappingData.getMainXmlMapping(),
				auditTableData,
				pc.getDiscriminatorValue(),
				pc.isAbstract()
		);
		final ExtendedPropertyMapper propertyMapper = new MultiPropertyMapper();

		// Checking if there is a discriminator column
		final ValueMapping discriminatorMapping = pc.getEntityMappingHierarchy().getDiscriminatorMapping();
		if ( discriminatorMapping != null ) {
			final Element discriminatorElement = classMapping.addElement( "discriminator" );
			// Database column or SQL formula allowed to distinguish entity types
			MetadataTools.addColumnsOrFormulas( discriminatorElement, discriminatorMapping.getMappedColumns() );
			discriminatorElement.addAttribute( "type", discriminatorMapping.getJavaTypeMapping().getTypeName() );
		}

		// Adding the id mapping
		classMapping.add( (Element) idMapper.getXmlMapping().clone() );

		// Adding the "revision type" property
		addRevisionType( classMapping, classMapping );

		addAdditionalColumns( classMapping );

		return Triple.make( classMapping, propertyMapper, null );
	}

	private Triple<Element, ExtendedPropertyMapper, String> generateInheritanceMappingData(
			PersistentClass pc, EntityXmlMappingData xmlMappingData, AuditTableData auditTableData,
			String inheritanceMappingType) {
		final String extendsEntityName = options.getAuditEntityName( pc.getSuperclass().getEntityName() );
		final Element classMapping = MetadataTools.createSubclassEntity(
				xmlMappingData.getMainXmlMapping(),
				inheritanceMappingType,
				auditTableData,
				extendsEntityName,
				pc.getDiscriminatorValue(),
				pc.isAbstract()
		);

		// The id and revision type is already mapped in the parent

		// Getting the property mapper of the parent - when mapping properties, they need to be included
		final String parentEntityName = pc.getSuperclass().getEntityName();

		final EntityConfiguration parentConfiguration = entitiesConfigurations.get( parentEntityName );
		if ( parentConfiguration == null ) {
			throw new MappingException(
					"Entity '" + pc.getEntityName() + "' is audited, but its superclass: '" +
							parentEntityName + "' is not."
			);
		}

		final ExtendedPropertyMapper parentPropertyMapper = parentConfiguration.getPropertyMapper();
		final ExtendedPropertyMapper propertyMapper = new SubclassPropertyMapper(
				new MultiPropertyMapper(),
				parentPropertyMapper
		);

		return Triple.make( classMapping, propertyMapper, parentEntityName );
	}

	@SuppressWarnings({"unchecked"})
	public void generateFirstPass(
			PersistentClass pc,
			ClassAuditingData auditingData,
			EntityXmlMappingData xmlMappingData,
			boolean isAudited) {
		final String schema = getSchema( auditingData.getAuditTable().schema(), pc.getTable() );
		final String catalog = getCatalog( auditingData.getAuditTable().catalog(), pc.getTable() );

		if ( !isAudited ) {
			final String entityName = pc.getEntityName();
			final IdMappingData idMapper = idMetadataGenerator.addId( pc, false );

			if ( idMapper == null ) {
				// Unsupported id mapping, e.g. key-many-to-one. If the entity is used in auditing, an exception
				// will be thrown later on.
				LOG.debugf(
						"Unable to create auditing id mapping for entity %s, " +
								"because of an unsupported Hibernate id mapping (e.g. key-many-to-one)",
						entityName
				);
				return;
			}

			final ExtendedPropertyMapper propertyMapper = null;
			final String parentEntityName = null;
			final EntityConfiguration entityCfg = new EntityConfiguration(
					entityName,
					pc.getClassName(),
					idMapper,
					propertyMapper,
					parentEntityName
			);
			notAuditedEntitiesConfigurations.put( entityName, entityCfg );
			return;
		}

		final String entityName = pc.getEntityName();
		LOG.debugf( "Generating first-pass auditing mapping for entity %s", entityName );

		final String auditEntityName = options.getAuditEntityName( entityName );
		final String auditTableName = options.getAuditTableName( entityName, pc.getTable().getName() );

		// Registering the audit entity name, now that it is known
		auditEntityNameRegister.register( auditEntityName );

		final AuditTableData auditTableData = new AuditTableData( auditEntityName, auditTableName, schema, catalog );

		// Generating a mapping for the id
		final IdMappingData idMapper = idMetadataGenerator.addId( pc, true );

		final InheritanceType inheritanceType = InheritanceType.get( pc );

		// These properties will be read from the mapping data
		final Element classMapping;
		final ExtendedPropertyMapper propertyMapper;
		final String parentEntityName;

		final Triple<Element, ExtendedPropertyMapper, String> mappingData;

		// Reading the mapping data depending on inheritance type (if any)
		switch ( inheritanceType ) {
			case NONE:
				mappingData = generateMappingData( pc, xmlMappingData, auditTableData, idMapper );
				break;

			case SINGLE:
				mappingData = generateInheritanceMappingData( pc, xmlMappingData, auditTableData, "subclass" );
				break;

			case JOINED:
				mappingData = generateInheritanceMappingData( pc, xmlMappingData, auditTableData, "joined-subclass" );

				// Adding the "key" element with all id columns...
				final Element keyMapping = mappingData.getFirst().addElement( "key" );
				MetadataTools.addColumns( keyMapping, pc.getTable().getPrimaryKey().getColumns() );

				// ... and the revision number column, read from the revision info relation mapping.
				keyMapping.add( (Element) cloneAndSetupRevisionInfoRelationMapping().element( "column" ).clone() );

				if ( !options.isRevisionEndTimestampLegacyBehaviorEnabled() ) {
					// HHH-9062 - Add revision end timestamp to mapping, if applicable.
					addEndRevisionTimestamp( mappingData.getFirst() );
				}

				break;

			case TABLE_PER_CLASS:
				mappingData = generateInheritanceMappingData( pc, xmlMappingData, auditTableData, "union-subclass" );
				break;

			default:
				throw new AssertionError( "Impossible enum value." );
		}

		classMapping = mappingData.getFirst();
		propertyMapper = mappingData.getSecond();
		parentEntityName = mappingData.getThird();

		xmlMappingData.setClassMapping( classMapping );

		// Mapping unjoined properties
		addProperties(
				classMapping, pc.getUnjoinedPropertyIterator(), propertyMapper,
				auditingData, pc.getEntityName(), xmlMappingData,
				true
		);

		// Creating and mapping joins (first pass)
		createJoins( pc, classMapping, auditingData );
		addJoins( pc, propertyMapper, auditingData, pc.getEntityName(), xmlMappingData, true );

		// HHH-7940 - New synthetic property support for @IndexColumn/@OrderColumn dynamic properties
		addSynthetics( classMapping, auditingData, propertyMapper, xmlMappingData, pc.getEntityName(), true );

		// Storing the generated configuration
		final EntityConfiguration entityCfg = new EntityConfiguration(
				auditEntityName,
				pc.getClassName(),
				idMapper,
				propertyMapper,
				parentEntityName
		);
		entitiesConfigurations.put( pc.getEntityName(), entityCfg );
	}

	private void addSynthetics(
			Element classMapping,
			ClassAuditingData auditingData,
			CompositeMapperBuilder currentMapper,
			EntityXmlMappingData xmlMappingData,
			String entityName,
			boolean firstPass) {
		for ( PropertyAuditingData propertyAuditingData : auditingData.getSyntheticProperties() ) {
			addValue(
					classMapping,
					propertyAuditingData.getValue(),
					currentMapper,
					entityName,
					xmlMappingData,
					propertyAuditingData,
					true,
					firstPass,
					false
			);
		}
	}

	@SuppressWarnings({"unchecked"})
	public void generateSecondPass(
			PersistentClass pc,
			ClassAuditingData auditingData,
			EntityXmlMappingData xmlMappingData) {
		final String entityName = pc.getEntityName();
		LOG.debugf( "Generating second-pass auditing mapping for entity %s", entityName );

		final CompositeMapperBuilder propertyMapper = entitiesConfigurations.get( entityName ).getPropertyMapper();

		// Mapping unjoined properties
		final Element parent = xmlMappingData.getClassMapping();

		addProperties(
				parent,
				pc.getUnjoinedPropertyIterator(),
				propertyMapper,
				auditingData,
				entityName,
				xmlMappingData,
				false
		);

		// Mapping joins (second pass)
		addJoins( pc, propertyMapper, auditingData, entityName, xmlMappingData, false );
	}

	public Map<String, EntityConfiguration> getEntitiesConfigurations() {
		return entitiesConfigurations;
	}

	// Getters for generators and configuration

	BasicMetadataGenerator getBasicMetadataGenerator() {
		return basicMetadataGenerator;
	}

	AuditMetadataBuildingOptions getOptions() {
		return options;
	}

	AuditEntityNameRegister getAuditEntityNameRegister() {
		return auditEntityNameRegister;
	}

	void throwUnsupportedTypeException(ValueMapping value, String entityName, String propertyName) {
		final String message = "Type not supported for auditing: " + value.getJavaTypeMapping().getTypeName() +
				", on entity " + entityName + ", property '" + propertyName + "'.";

		throw new MappingException( message );
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
	 * @throws MappingException If a relation from an audited to a non-audited entity is detected, which is not
	 * mapped using {@link RelationTargetAuditMode#NOT_AUDITED}.
	 */
	IdMappingData getReferencedIdMappingData(
			String entityName, String referencedEntityName,
			PropertyAuditingData propertyAuditingData,
			boolean allowNotAuditedTarget) {
		EntityConfiguration configuration = getEntitiesConfigurations().get( referencedEntityName );
		if ( configuration == null ) {
			final RelationTargetAuditMode relationTargetAuditMode = propertyAuditingData.getRelationTargetAuditMode();
			configuration = getNotAuditedEntitiesConfigurations().get( referencedEntityName );

			final boolean isAudited = !RelationTargetAuditMode.NOT_AUDITED.equals( relationTargetAuditMode );
			if ( configuration == null || !allowNotAuditedTarget || isAudited ) {
				if ( !allowNotAuditedTarget ) {
					throw new MappingException(
							String.format(
									Locale.ROOT,
									"An audited relation from %s.%s to a not audited entity %s!",
									entityName,
									propertyAuditingData.getName(),
									referencedEntityName
							)
					);
				}
				else {
					throw new MappingException(
							String.format(
									Locale.ROOT,
									"An audited relation from %s.%s to a not audited entity %s! " +
											"Such mapping is possible but has to be explicitly defined using " +
											"@Audited(targetAuditMode = NOT_AUDITED).",
									entityName,
									propertyAuditingData.getName(),
									referencedEntityName
							)
					);
				}
			}
		}

		return configuration.getIdMappingData();
	}

	/**
	 * Get the notAuditedEntitiesConfigurations property.
	 *
	 * @return the notAuditedEntitiesConfigurations property value
	 */
	public Map<String, EntityConfiguration> getNotAuditedEntitiesConfigurations() {
		return notAuditedEntitiesConfigurations;
	}
}
