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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.ExtendedPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.SubclassPropertyMapper;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.internal.tools.Triple;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.OneToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.TimestampType;
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
 */
public final class AuditMetadataGenerator {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			AuditMetadataGenerator.class.getName()
	);
	private final AuditConfiguration.AuditConfigurationContext context;
	private final AuditStrategy auditStrategy;
	private final Element revisionInfoRelationMapping;

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

	// TODO: add support for secondary tables.
	// Map entity name -> (join descriptor -> element describing the "versioned" join)
	//private final Map<String, Map<Join, Element>> entitiesJoins;

	public AuditMetadataGenerator(
			AuditConfiguration.AuditConfigurationContext context,
			AuditStrategy auditStrategy,
			Element revisionInfoRelationMapping,
			AuditEntityNameRegister auditEntityNameRegister) {
		this.context = context;
		this.auditStrategy = auditStrategy;
		this.revisionInfoRelationMapping = revisionInfoRelationMapping;

		this.basicMetadataGenerator = new BasicMetadataGenerator();
		this.componentMetadataGenerator = new ComponentMetadataGenerator( context, this );
		this.idMetadataGenerator = new IdMetadataGenerator( context, this );
		this.toOneRelationMetadataGenerator = new ToOneRelationMetadataGenerator( this );

		this.auditEntityNameRegister = auditEntityNameRegister;

		entitiesConfigurations = new HashMap<String, EntityConfiguration>();
		notAuditedEntitiesConfigurations = new HashMap<String, EntityConfiguration>();

		// TODO: add support for secondary tables.
		//entitiesJoins = new HashMap<String, Map<SecondaryTable, Element>>();
	}

	/**
	 * Clones the revision info relation mapping, so that it can be added to other mappings. Also, the name of
	 * the property and the column are set properly.
	 *
	 * @return A revision info mapping, which can be added to other mappings (has no parent).
	 */
	private Element cloneAndSetupRevisionInfoRelationMapping() {
		final Element revMapping = (Element) revisionInfoRelationMapping.clone();
		revMapping.addAttribute( "name", context.getAuditEntitiesConfiguration().getRevisionFieldName() );
		if ( context.getGlobalConfiguration().isCascadeDeleteRevision() ) {
			revMapping.addAttribute( "on-delete", "cascade" );
	    } 

		MetadataTools.addOrModifyColumn( revMapping, context.getAuditEntitiesConfiguration().getRevisionFieldName() );

		return revMapping;
	}

	void addRevisionInfoRelation(Element anyMapping) {
		anyMapping.add( cloneAndSetupRevisionInfoRelationMapping() );
	}

	void addRevisionType(Element anyMapping, Element anyMappingEnd, boolean key) {
		final Element revTypeProperty = MetadataTools.addProperty(
				anyMapping,
				context.getAuditEntitiesConfiguration().getRevisionTypePropName(),
				context.getAuditEntitiesConfiguration().getRevisionTypePropType(),
				true,
				key
		);
		revTypeProperty.addAttribute( "type", "org.hibernate.envers.internal.entities.RevisionTypeType" );

		// Adding the end revision, if appropriate
		addEndRevision( anyMappingEnd );
	}

	private void addEndRevision(Element anyMapping) {
		// Add the end-revision field, if the appropriate strategy is used.
		if ( auditStrategy instanceof ValidityAuditStrategy ) {
			final Element endRevMapping = (Element) revisionInfoRelationMapping.clone();
			endRevMapping.setName( "many-to-one" );
			endRevMapping.addAttribute( "name", context.getAuditEntitiesConfiguration().getRevisionEndFieldName() );
			MetadataTools.addOrModifyColumn( endRevMapping, context.getAuditEntitiesConfiguration().getRevisionEndFieldName() );

			anyMapping.add( endRevMapping );

			if ( context.getAuditEntitiesConfiguration().isRevisionEndTimestampEnabled() ) {
				// add a column for the timestamp of the end revision
				final String revisionInfoTimestampSqlType = TimestampType.INSTANCE.getName();
				final Element timestampProperty = MetadataTools.addProperty(
						anyMapping,
						context.getAuditEntitiesConfiguration().getRevisionEndTimestampFieldName(),
						revisionInfoTimestampSqlType,
						true,
						true,
						false
				);
				MetadataTools.addColumn(
						timestampProperty,
						context.getAuditEntitiesConfiguration().getRevisionEndTimestampFieldName(),
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
			AttributeBinding attributeBinding,
			CompositeMapperBuilder currentMapper,
			String entityName,
			EntityXmlMappingData xmlMappingData,
			PropertyAuditingData propertyAuditingData,
			boolean processModifiedFlag) {
		final Type type = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();

		final List<Value> values;
		final boolean isInsertable;
		if ( attributeBinding.getAttribute().isSingular() ) {
			final SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
			values = singularAttributeBinding.getValues();
			isInsertable = singularAttributeBinding.isIncludedInInsert();
		}
		else {
			values = null;
			isInsertable = false;
		}

		final boolean isBasic = basicMetadataGenerator.addBasic(
				parent,
				propertyAuditingData,
				attributeBinding.getHibernateTypeDescriptor(),
				values,
				isInsertable,
				currentMapper,
				false
		);

		if ( isBasic ) {
			// The property was mapped by the basic generator.
		}
		else if ( type instanceof ComponentType ) {
			componentMetadataGenerator.addComponent(
					parent,
					propertyAuditingData,
					(EmbeddedAttributeBinding) attributeBinding,
					currentMapper,
					entityName,
					xmlMappingData,
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
		addModifiedFlagIfNeeded( parent, propertyAuditingData, processModifiedFlag );
	}

	private boolean processedInSecondPass(Type type) {
		return type instanceof ComponentType || type instanceof ManyToOneType ||
				type instanceof OneToOneType || type instanceof CollectionType;
	}

	private void addValueInSecondPass(
			Element parent,
			AttributeBinding attributeBinding,
			CompositeMapperBuilder currentMapper,
			String entityName,
			EntityXmlMappingData xmlMappingData,
			PropertyAuditingData propertyAuditingData,
			boolean processModifiedFlag) {
		final Type type = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();

		if ( type instanceof ComponentType ) {
			componentMetadataGenerator.addComponent(
					parent,
					propertyAuditingData,
					(EmbeddedAttributeBinding) attributeBinding,
					currentMapper,
					entityName,
					xmlMappingData,
					false
			);
			// mod flag field has been already generated in first pass
			return;
		}
		else if ( type instanceof ManyToOneType ) {
			toOneRelationMetadataGenerator.addToOne(
					parent,
					propertyAuditingData,
					(ManyToOneAttributeBinding) attributeBinding,
					currentMapper,
					entityName
			);
		}
		else if ( type instanceof OneToOneType ) {
			final OneToOneAttributeBinding oneToOneAttributeBinding = (OneToOneAttributeBinding) attributeBinding;
			if ( oneToOneAttributeBinding.getReferencedAttributeBinding().getAttribute().getName() != null &&
					!oneToOneAttributeBinding.getReferencedEntityBinding().getHierarchyDetails()
							.getEntityIdentifier().getEntityIdentifierBinding()
							.isIdentifierAttributeBinding(  oneToOneAttributeBinding.getReferencedAttributeBinding() ) ) {
				toOneRelationMetadataGenerator.addOneToOneNotOwning(
						propertyAuditingData,
						oneToOneAttributeBinding,
						currentMapper,
						entityName
				);
			}
			else {
				// @OneToOne relation marked with @PrimaryKeyJoinColumn
				toOneRelationMetadataGenerator.addOneToOnePrimaryKeyJoinColumn(
						propertyAuditingData,
						oneToOneAttributeBinding,
						currentMapper,
						entityName
				);
			}
		}
		else if ( type instanceof CollectionType ) {
			final CollectionMetadataGenerator collectionMetadataGenerator = new CollectionMetadataGenerator(
					context,
					this,
					(PluralAttributeBinding) attributeBinding,
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
					context.getGlobalConfiguration().getModifiedFlagSuffix()
			);
		}
	}

	void addValue(
			Element parent,
			AttributeBinding attributeBinding,
			CompositeMapperBuilder currentMapper,
			String entityName,
			EntityXmlMappingData xmlMappingData,
			PropertyAuditingData propertyAuditingData,
			boolean firstPass,
			boolean processModifiedFlag) {
		if ( firstPass ) {
			addValueInFirstPass(
					parent, attributeBinding, currentMapper, entityName,
					xmlMappingData, propertyAuditingData, processModifiedFlag
			);
		}
		else {
			addValueInSecondPass(
					parent, attributeBinding, currentMapper, entityName,
					xmlMappingData, propertyAuditingData, processModifiedFlag
			);
		}
	}

	private void addProperties(
			Element parent,
			AttributeBinding[] properties,
			CompositeMapperBuilder currentMapper,
			ClassAuditingData auditingData,
			String entityName,
			EntityXmlMappingData xmlMappingData,
			boolean firstPass) {
		for ( AttributeBinding property : properties ) {
			final String propertyName = property.getAttribute().getName();
			final PropertyAuditingData propertyAuditingData = auditingData.getPropertyAuditingData( propertyName );
			if ( propertyAuditingData != null ) {
				addValue(
						parent,
						property,
						currentMapper,
						entityName,
						xmlMappingData,
						propertyAuditingData,
						firstPass,
						true
				);
			}
		}
	}

	private boolean checkPropertiesAudited(Iterator<AttributeBinding> properties, ClassAuditingData auditingData) {
		while ( properties.hasNext() ) {
			final AttributeBinding property = properties.next();
			final String propertyName = property.getAttribute().getName();
			final PropertyAuditingData propertyAuditingData = auditingData.getPropertyAuditingData( propertyName );
			if ( propertyAuditingData == null ) {
				return false;
			}
		}

		return true;
	}

	protected String getSchema(String schemaFromAnnotation, TableSpecification table) {
		// Get the schema from the annotation ...
		String schema = schemaFromAnnotation;
		// ... if empty, try using the default ...
		if ( StringTools.isEmpty( schema ) ) {
			schema = context.getGlobalConfiguration().getDefaultSchemaName();

			// ... if still empty, use the same as the normal table.
			if ( StringTools.isEmpty( schema ) &&  table.getSchema().getName().getSchema() != null) {
				schema = table.getSchema().getName().getSchema().getText();
			}
		}

		return schema;
	}

	protected String getCatalog(String catalogFromAnnotation, TableSpecification table) {
		// Get the catalog from the annotation ...
		String catalog = catalogFromAnnotation;
		// ... if empty, try using the default ...
		if ( StringTools.isEmpty( catalog ) ) {
			catalog = context.getGlobalConfiguration().getDefaultCatalogName();

			// ... if still empty, use the same as the normal table.
			if ( StringTools.isEmpty( catalog ) && table.getSchema().getName().getCatalog() != null ) {
				catalog = table.getSchema().getName().getCatalog().getText();
			}
		}

		return catalog;
	}

	// TODO: add support for secondary tables
//	private void createJoins(EntityBinding entityBinding, Element parent, ClassAuditingData auditingData) {
//		final Iterator<SecondaryTable> joins = entityBinding.getSecondaryTables();
//		final Map<SecondaryTable, Element> joinElements = new HashMap<SecondaryTable, Element>();
//		entitiesJoins.put( entityBinding.getEntityName(), joinElements );

//		while ( joins.hasNext() ) {
//			SecondaryTable join = joins.next();

			// Checking if all of the join properties are audited
//			if ( !checkPropertiesAudited( join.getPropertyIterator(), auditingData ) ) {
//				continue;
//			}

			// Determining the table name. If there is no entry in the dictionary, just constructing the table name
			// as if it was an entity (by appending/prepending configured strings).
//			final String originalTableName = join.getSecondaryTableReference().getLogicalName().getText();
//			String auditTableName = auditingData.getSecondaryTableDictionary().get( originalTableName );
//			if ( auditTableName == null ) {
//				auditTableName = context.getAuditEntitiesConfiguration().getAuditEntityName( originalTableName );
//			}

//			final String schema = getSchema( auditingData.getAuditTable().schema(), join.getSecondaryTableReference() );
//			final String catalog = getCatalog( auditingData.getAuditTable().catalog(), join.getSecondaryTableReference() );

//			final Element joinElement = MetadataTools.createJoin( parent, auditTableName, schema, catalog );
//			joinElements.put( join, joinElement );

//			final Element joinKey = joinElement.addElement( "key" );
//			MetadataTools.addColumns( joinKey, join.getForeignKeyReference().getColumns() );
//			MetadataTools.addColumn( joinKey, context.getAuditEntitiesConfiguration().getRevisionFieldName(), null, null, null, null, null, null );
//		}
//	}

//	@SuppressWarnings({"unchecked"})
//	private void createJoins(PersistentClass pc, Element parent, ClassAuditingData auditingData) {
//		final Iterator<Join> joins = pc.getJoinIterator();
//		final Map<Join, Element> joinElements = new HashMap<Join, Element>();
//		entitiesJoins.put( pc.getEntityName(), joinElements );
//
//		while ( joins.hasNext() ) {
//			Join join = joins.next();

			// Checking if all of the join properties are audited
//			if ( !checkPropertiesAudited( join.getPropertyIterator(), auditingData ) ) {
//				continue;
//			}

			// Determining the table name. If there is no entry in the dictionary, just constructing the table name
			// as if it was an entity (by appending/prepending configured strings).
//			final String originalTableName = join.getTable().getName();
//			String auditTableName = auditingData.getSecondaryTableDictionary().get( originalTableName );
//			if ( auditTableName == null ) {
//				auditTableName = verEntCfg.getAuditEntityName( originalTableName );
//			}

//			final String schema = getSchema( auditingData.getAuditTable().schema(), join.getTable() );
//			final String catalog = getCatalog( auditingData.getAuditTable().catalog(), join.getTable() );

//			final Element joinElement = MetadataTools.createJoin( parent, auditTableName, schema, catalog );
//			joinElements.put( join, joinElement );

//			final Element joinKey = joinElement.addElement( "key" );
//			MetadataTools.addColumns( joinKey, join.getKey().getColumnIterator() );
//			MetadataTools.addColumn( joinKey, verEntCfg.getRevisionFieldName(), null, null, null, null, null, null );
//		}
//	}

//	@SuppressWarnings({"unchecked"})
//	private void addJoins(
//			PersistentClass pc,
//			CompositeMapperBuilder currentMapper,
//			ClassAuditingData auditingData,
//			String entityName,
//			EntityXmlMappingData xmlMappingData,
//			boolean firstPass) {
//		final Iterator<Join> joins = pc.getJoinIterator();

//		while ( joins.hasNext() ) {
//			final Join join = joins.next();
//			final Element joinElement = entitiesJoins.get( entityName ).get( join );

//			if ( joinElement != null ) {
//				addProperties(
//						joinElement,
//						join.getPropertyIterator(),
//						currentMapper,
//						auditingData,
//						entityName,
//						xmlMappingData,
//						firstPass
//				);
//			}
//		}
//	}

	@SuppressWarnings({"unchecked"})
	private Triple<Element, ExtendedPropertyMapper, String> generateMappingData(
			EntityBinding entityBinding, EntityXmlMappingData xmlMappingData, AuditTableData auditTableData,
			IdMappingData idMapper) {
		final Element classMapping = MetadataTools.createEntity(
				xmlMappingData.getMainXmlMapping(),
				auditTableData,
				entityBinding.getDiscriminatorMatchValue(),
				entityBinding.isAbstract()
		);
		final ExtendedPropertyMapper propertyMapper = new MultiPropertyMapper();

		// Adding the id mapping
		classMapping.add( (Element) idMapper.getXmlMapping().clone() );

		// Checking if there is a discriminator column
		if ( entityBinding.getHierarchyDetails().getEntityDiscriminator() != null ) {
			final EntityDiscriminator discriminator = entityBinding.getHierarchyDetails().getEntityDiscriminator();
			final Element discriminatorElement = classMapping.addElement( "discriminator" );
			// Database column or SQL formula allowed to distinguish entity types
			MetadataTools.addColumnsOrFormulas(
					discriminatorElement,
					Collections.singletonList( discriminator.getRelationalValue() )
			);
			discriminatorElement.addAttribute(
					"type",
					discriminator.getExplicitHibernateTypeDescriptor().getExplicitTypeName()
			);
		}

		// Adding the "revision type" property
		addRevisionType( classMapping, classMapping, false );

		return Triple.make( classMapping, propertyMapper, null );
	}

	private Triple<Element, ExtendedPropertyMapper, String> generateInheritanceMappingData(
			EntityBinding entityBinding,
			EntityXmlMappingData xmlMappingData,
			AuditTableData auditTableData,
			String inheritanceMappingType) {
		final String parentEntityName = entityBinding.getSuperEntityBinding().getEntityName();
		final String extendsEntityName = context.getAuditEntitiesConfiguration().getAuditEntityName( parentEntityName );
		final Element classMapping = MetadataTools.createSubclassEntity(
				xmlMappingData.getMainXmlMapping(),
				inheritanceMappingType,
				auditTableData,
				extendsEntityName,
				entityBinding.getDiscriminatorMatchValue(),
				entityBinding.isAbstract()
		);

		// The id and revision type is already mapped in the parent

		// Getting the property mapper of the parent - when mapping properties, they need to be included
		final EntityConfiguration parentConfiguration = entitiesConfigurations.get( parentEntityName );
		if ( parentConfiguration == null ) {
			throw new MappingException(
					"Entity '" + entityBinding.getEntityName() + "' is audited, but its superclass: '" +
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
			EntityBinding entityBinding,
			ClassAuditingData auditingData,
			EntityXmlMappingData xmlMappingData,
			boolean isAudited) {
		final String schema = getSchema( auditingData.getAuditTable().schema(), entityBinding.getPrimaryTable() );
		final String catalog = getCatalog( auditingData.getAuditTable().catalog(), entityBinding.getPrimaryTable() );

		if ( !isAudited ) {
			final String entityName = entityBinding.getEntityName();
			final IdMappingData idMapper = idMetadataGenerator.addId( entityBinding, false );

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
					entityBinding.getEntity().getDescriptor().getName().toString(),
					idMapper,
					propertyMapper,
					parentEntityName
			);
			notAuditedEntitiesConfigurations.put( entityName, entityCfg );
			return;
		}

		final String entityName = entityBinding.getEntityName();
		LOG.debugf( "Generating first-pass auditing mapping for entity %s", entityName );

		final String auditEntityName = context.getAuditEntitiesConfiguration().getAuditEntityName( entityName );
		final String auditTableName = context.getAuditEntitiesConfiguration().getAuditTableName(
				entityName,
				entityBinding.getPrimaryTableName()
		);

		// Registering the audit entity name, now that it is known
		auditEntityNameRegister.register( auditEntityName );

		final AuditTableData auditTableData = new AuditTableData( auditEntityName, auditTableName, schema, catalog );

		// Generating a mapping for the id
		final IdMappingData idMapper = idMetadataGenerator.addId( entityBinding, true );

		final InheritanceType inheritanceType = InheritanceType.get( entityBinding );

		// These properties will be read from the mapping data
		final Element classMapping;
		final ExtendedPropertyMapper propertyMapper;
		final String parentEntityName;

		final Triple<Element, ExtendedPropertyMapper, String> mappingData;

		// Reading the mapping data depending on inheritance type (if any)
		switch ( inheritanceType ) {
			case NONE:
				mappingData = generateMappingData( entityBinding, xmlMappingData, auditTableData, idMapper );
				break;

			case SINGLE:
				mappingData = generateInheritanceMappingData( entityBinding, xmlMappingData, auditTableData, "subclass" );
				break;

			case JOINED:
				mappingData = generateInheritanceMappingData( entityBinding, xmlMappingData, auditTableData, "joined-subclass" );

				// Adding the "key" element with all id columns...
				final Element keyMapping = mappingData.getFirst().addElement( "key" );
				MetadataTools.addColumns( keyMapping, entityBinding.getPrimaryTable().getPrimaryKey().getColumns() );

				// ... and the revision number column, read from the revision info relation mapping.
				keyMapping.add( (Element) cloneAndSetupRevisionInfoRelationMapping().element( "column" ).clone() );
				break;

			case TABLE_PER_CLASS:
				mappingData = generateInheritanceMappingData( entityBinding, xmlMappingData, auditTableData, "union-subclass" );
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
				classMapping,
				entityBinding.getNonIdAttributeBindingClosure(), // TODO: this needs to be corrected to only get non-joined attribute bindings.
				//entityBinding.getUnjoinedPropertyIterator(),
				propertyMapper,
				auditingData,
				entityBinding.getEntityName(),
				xmlMappingData,
				true
		);

		// TODO: add support for joins
		if ( !entityBinding.getSecondaryTables().isEmpty() ) {
			throw new NotYetImplementedException( "Secondary tables are not supported by envers yet." );
		}

		// Creating and mapping joins (first pass)
		//createJoins( entityBinding, classMapping, auditingData );
		//addJoins( entityBinding, propertyMapper, auditingData, entityBinding.getEntityName(), xmlMappingData, true );

		// Storing the generated configuration
		final EntityConfiguration entityCfg = new EntityConfiguration(
				auditEntityName,
				entityBinding.getEntity().getDescriptor().getName().toString(),
				idMapper,
				propertyMapper,
				parentEntityName
		);
		entitiesConfigurations.put( entityBinding.getEntityName(), entityCfg );
	}

	@SuppressWarnings({"unchecked"})
	public void generateSecondPass(
			EntityBinding entityBinding,
			ClassAuditingData auditingData,
			EntityXmlMappingData xmlMappingData) {
		final String entityName = entityBinding.getEntityName();
		LOG.debugf( "Generating second-pass auditing mapping for entity %s", entityName );

		final CompositeMapperBuilder propertyMapper = entitiesConfigurations.get( entityName ).getPropertyMapper();

		// Mapping unjoined properties
		final Element parent = xmlMappingData.getClassMapping();

		addProperties(
				parent,
				entityBinding.getNonIdAttributeBindingClosure(), // TODO: this needs to be corrected to only get non-joined attribute bindings.
				//entityBinding.getUnjoinedPropertyIterator(),
				propertyMapper,
				auditingData,
				entityName,
				xmlMappingData,
				false
		);

		// TODO: add suuport for joins
		// Mapping joins (second pass)
		//addJoins( entityBinding, propertyMapper, auditingData, entityName, xmlMappingData, false );
	}

	public Map<String, EntityConfiguration> getEntitiesConfigurations() {
		return entitiesConfigurations;
	}

	// Getters for generators and configuration

	BasicMetadataGenerator getBasicMetadataGenerator() {
		return basicMetadataGenerator;
	}

	AuditStrategy getAuditStrategy() {
		return auditStrategy;
	}

	AuditEntityNameRegister getAuditEntityNameRegister() {
		return auditEntityNameRegister;
	}

	void throwUnsupportedTypeException(Type type, String entityName, String propertyName) {
		final String message = "Type not supported for auditing: " + type.getClass().getName() +
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

			if ( configuration == null || !allowNotAuditedTarget || !RelationTargetAuditMode.NOT_AUDITED.equals(
					relationTargetAuditMode
			) ) {
				throw new MappingException(
						"An audited relation from " + entityName + "."
								+ propertyAuditingData.getName() + " to a not audited entity " + referencedEntityName + "!"
								+ (allowNotAuditedTarget ?
								" Such mapping is possible, but has to be explicitly defined using @Audited(targetAuditMode = NOT_AUDITED)." :
								"")
				);
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
