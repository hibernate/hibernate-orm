/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.hbm;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.hibernate.EntityMode;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.Value;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.EntityElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.JoinElementSource;
import org.hibernate.metamodel.binding.BagBinding;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.AbstractAttributeContainer;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.SingularAttributeSource;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLAnyElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLBagElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLCacheElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLColumnElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLComponentElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLDynamicComponentElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLIdbagElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLJoinElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLJoinedSubclassElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLListElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLManyToOneElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLMapElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLOneToOneElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLParamElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLPropertiesElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLPropertyElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSetElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSqlDeleteElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSqlInsertElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSqlUpdateElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSubclassElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSynchronizeElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLTuplizerElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLUnionSubclassElement;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * @author Steve Ebersole
 */
public class BindingCreator {
	private final MetadataImplementor metadata;
	private final List<String> processedEntityNames;

	private InheritanceType currentInheritanceType;
	private HbmBindingContext currentBindingContext;

	public BindingCreator(MetadataImplementor metadata, List<String> processedEntityNames) {
		this.metadata = metadata;
		this.processedEntityNames = processedEntityNames;
	}

	// todo : currently this does not allow inheritance across hbm/annotations.  Do we need to?

	public void processEntityHierarchy(EntityHierarchy entityHierarchy) {
		currentInheritanceType = entityHierarchy.getHierarchyInheritanceType();
		EntityBinding rootEntityBinding = createEntityBinding( entityHierarchy.getEntitySourceInformation(), null );
		if ( currentInheritanceType != InheritanceType.NO_INHERITANCE ) {
			processHierarchySubEntities( entityHierarchy, rootEntityBinding );
		}
	}

	private void processHierarchySubEntities(SubEntityContainer subEntityContainer, EntityBinding superEntityBinding) {
		for ( EntityHierarchySubEntity subEntity : subEntityContainer.subEntityDescriptors() ) {
			EntityBinding entityBinding = createEntityBinding( subEntity.getEntitySourceInformation(), superEntityBinding );
			processHierarchySubEntities( subEntity, entityBinding );
		}
	}

	private EntityBinding createEntityBinding(EntitySourceInformation entitySourceInfo, EntityBinding superEntityBinding) {
		if ( processedEntityNames.contains( entitySourceInfo.getMappedEntityName() ) ) {
			return metadata.getEntityBinding( entitySourceInfo.getMappedEntityName() );
		}

		currentBindingContext = entitySourceInfo.getSourceMappingDocument().getMappingLocalBindingContext();
		try {
			final EntityBinding entityBinding = doCreateEntityBinding( entitySourceInfo, superEntityBinding );

			metadata.addEntity( entityBinding );
			processedEntityNames.add( entityBinding.getEntity().getName() );
			return entityBinding;
		}
		finally {
			currentBindingContext = null;
		}
	}

	private EntityBinding doCreateEntityBinding(EntitySourceInformation entitySourceInfo, EntityBinding superEntityBinding) {
		final EntityBinding entityBinding = createBasicEntityBinding( entitySourceInfo, superEntityBinding );

		bindAttributes( entitySourceInfo, entityBinding );
		bindSecondaryTables( entitySourceInfo, entityBinding );
		bindTableUniqueConstraints( entityBinding );

		return entityBinding;
	}

	private EntityBinding createBasicEntityBinding(
			EntitySourceInformation entitySourceInfo,
			EntityBinding superEntityBinding) {
		if ( superEntityBinding == null ) {
			return makeRootEntityBinding( entitySourceInfo );
		}
		else {
			if ( currentInheritanceType == InheritanceType.SINGLE_TABLE ) {
				return makeDiscriminatedSubclassBinding( entitySourceInfo, superEntityBinding );
			}
			else if ( currentInheritanceType == InheritanceType.JOINED ) {
				return makeJoinedSubclassBinding( entitySourceInfo, superEntityBinding );
			}
			else if ( currentInheritanceType == InheritanceType.TABLE_PER_CLASS ) {
				return makeUnionedSubclassBinding( entitySourceInfo, superEntityBinding );
			}
			else {
				// extreme internal error!
				throw new RuntimeException( "Internal condition failure" );
			}
		}
	}

	private EntityBinding makeRootEntityBinding(EntitySourceInformation entitySourceInfo) {
		final EntityBinding entityBinding = new EntityBinding();
		// todo : this is actually not correct
		// 		the problem is that we need to know whether we have mapped subclasses which happens later
		//		one option would be to simply reset the InheritanceType at that time.
		entityBinding.setInheritanceType( currentInheritanceType );
		entityBinding.setRoot( true );

		final XMLHibernateMapping.XMLClass xmlClass = (XMLHibernateMapping.XMLClass) entitySourceInfo.getEntityElement();
		final String entityName = entitySourceInfo.getMappedEntityName();
		final String verbatimClassName = xmlClass.getName();

		final EntityMode entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;
		entityBinding.setEntityMode( entityMode );

		final String className;
		if ( entityMode == EntityMode.POJO ) {
			className = entitySourceInfo.getSourceMappingDocument()
					.getMappingLocalBindingContext()
					.qualifyClassName( verbatimClassName );
		}
		else {
			className = null;
		}

		Entity entity = new Entity(
				entityName,
				className,
				entitySourceInfo.getSourceMappingDocument().getMappingLocalBindingContext().makeClassReference( className ),
				null
		);
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, entitySourceInfo );
		bindIdentifier( entityBinding, entitySourceInfo );

		entityBinding.setMutable( xmlClass.isMutable() );
		entityBinding.setExplicitPolymorphism( "explicit".equals( xmlClass.getPolymorphism() ) );
		entityBinding.setWhereFilter( xmlClass.getWhere() );
		entityBinding.setRowId( xmlClass.getRowid() );
		entityBinding.setOptimisticLockStyle( interpretOptimisticLockStyle( entitySourceInfo ) );
		entityBinding.setCaching( interpretCaching( entitySourceInfo ) );

		return entityBinding;
	}

	private OptimisticLockStyle interpretOptimisticLockStyle(EntitySourceInformation entitySourceInfo) {
		final String optimisticLockModeString = Helper.getStringValue(
				( (XMLHibernateMapping.XMLClass) entitySourceInfo.getEntityElement() ).getOptimisticLock(),
				"version"
		);
		try {
			return OptimisticLockStyle.valueOf( optimisticLockModeString.toUpperCase() );
		}
		catch (Exception e) {
			throw new MappingException(
					"Unknown optimistic-lock value : " + optimisticLockModeString,
					entitySourceInfo.getSourceMappingDocument().getOrigin()
			);
		}
	}

	private static Caching interpretCaching(EntitySourceInformation entitySourceInfo) {
		final XMLCacheElement cache = ( (XMLHibernateMapping.XMLClass) entitySourceInfo.getEntityElement() ).getCache();
		if ( cache == null ) {
			return null;
		}
		final String region = cache.getRegion() != null ? cache.getRegion() : entitySourceInfo.getMappedEntityName();
		final AccessType accessType = Enum.valueOf( AccessType.class, cache.getUsage() );
		final boolean cacheLazyProps = !"non-lazy".equals( cache.getInclude() );
		return new Caching( region, accessType, cacheLazyProps );
	}

	private EntityBinding makeDiscriminatedSubclassBinding(
			EntitySourceInformation entitySourceInfo,
			EntityBinding superEntityBinding) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.SINGLE_TABLE );
		bindSuperType( entityBinding, superEntityBinding );

		final String verbatimClassName = entitySourceInfo.getEntityElement().getName();

		final EntityMode entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;
		entityBinding.setEntityMode( entityMode );

		final String className;
		if ( entityMode == EntityMode.POJO ) {
			className = entitySourceInfo.getSourceMappingDocument().getMappingLocalBindingContext().qualifyClassName( verbatimClassName );
		}
		else {
			className = null;
		}

		final Entity entity = new Entity(
				entitySourceInfo.getMappedEntityName(),
				className,
				entitySourceInfo.getSourceMappingDocument().getMappingLocalBindingContext().makeClassReference( className ),
				null
		);
		entityBinding.setEntity( entity );


		performBasicEntityBind( entityBinding, entitySourceInfo );

		return entityBinding;
	}

	private EntityBinding makeJoinedSubclassBinding(
			EntitySourceInformation entitySourceInfo,
			EntityBinding superEntityBinding) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.JOINED );
		bindSuperType( entityBinding, superEntityBinding );

		final XMLJoinedSubclassElement joinedEntityElement = (XMLJoinedSubclassElement) entitySourceInfo.getEntityElement();
		final HbmBindingContext bindingContext = entitySourceInfo.getSourceMappingDocument().getMappingLocalBindingContext();

		final String entityName = bindingContext.determineEntityName( joinedEntityElement );
		final String verbatimClassName = joinedEntityElement.getName();

		final EntityMode entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;
		entityBinding.setEntityMode( entityMode );

		final String className;
		if ( entityMode == EntityMode.POJO ) {
			className = bindingContext.qualifyClassName( verbatimClassName );
		}
		else {
			className = null;
		}

		final Entity entity = new Entity( entityName, className, bindingContext.makeClassReference( className ), null );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, entitySourceInfo );

		return entityBinding;
	}

	private EntityBinding makeUnionedSubclassBinding(
			EntitySourceInformation entitySourceInfo,
			EntityBinding superEntityBinding) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.TABLE_PER_CLASS );
		bindSuperType( entityBinding, superEntityBinding );

		final XMLUnionSubclassElement unionEntityElement = (XMLUnionSubclassElement) entitySourceInfo.getEntityElement();
		final HbmBindingContext bindingContext = entitySourceInfo.getSourceMappingDocument().getMappingLocalBindingContext();

		final String entityName = bindingContext.determineEntityName( unionEntityElement );
		final String verbatimClassName = unionEntityElement.getName();

		final EntityMode entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;
		entityBinding.setEntityMode( entityMode );

		final String className;
		if ( entityMode == EntityMode.POJO ) {
			className = bindingContext.qualifyClassName( verbatimClassName );
		}
		else {
			className = null;
		}

		final Entity entity = new Entity( entityName, className, bindingContext.makeClassReference( className ), null );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, entitySourceInfo );

		return entityBinding;
	}

	private void bindSuperType(EntityBinding entityBinding, EntityBinding superEntityBinding) {
//		entityBinding.setSuperEntityBinding( superEntityBinding );
//		// not sure what to do with the domain model super type...
	}

	@SuppressWarnings( {"unchecked"})
	private void performBasicEntityBind(EntityBinding entityBinding, EntitySourceInformation entitySourceInfo) {
		bindPrimaryTable( entitySourceInfo, entityBinding );

		entityBinding.setJpaEntityName( null );

		final EntityElement entityElement = entitySourceInfo.getEntityElement();
		final HbmBindingContext bindingContext = entitySourceInfo.getSourceMappingDocument().getMappingLocalBindingContext();

		final String proxy = entityElement.getProxy();
		final boolean isLazy = entityElement.isLazy() == null
				? true
				: entityElement.isLazy();
		if ( entityBinding.getEntityMode() == EntityMode.POJO ) {
			if ( proxy != null ) {
				entityBinding.setProxyInterfaceType(
						bindingContext.makeClassReference(
								bindingContext.qualifyClassName( proxy )
						)
				);
				entityBinding.setLazy( true );
			}
			else if ( isLazy ) {
				entityBinding.setProxyInterfaceType( entityBinding.getEntity().getClassReferenceUnresolved() );
				entityBinding.setLazy( true );
			}
		}
		else {
			entityBinding.setProxyInterfaceType( new Value( Map.class ) );
			entityBinding.setLazy( isLazy );
		}

		final String customTuplizerClassName = extractCustomTuplizerClassName(
				entityElement,
				entityBinding.getEntityMode()
		);
		if ( customTuplizerClassName != null ) {
			entityBinding.setCustomEntityTuplizerClass( bindingContext.<EntityTuplizer>locateClassByName( customTuplizerClassName ) );
		}

		if ( entityElement.getPersister() != null ) {
			entityBinding.setCustomEntityPersisterClass( bindingContext.<EntityPersister>locateClassByName( entityElement.getPersister() ) );
		}

		entityBinding.setMetaAttributeContext(
				Helper.extractMetaAttributeContext(
						entityElement.getMeta(), true, bindingContext.getMetaAttributeContext()
				)
		);

		entityBinding.setDynamicUpdate( entityElement.isDynamicUpdate() );
		entityBinding.setDynamicInsert( entityElement.isDynamicInsert() );
		entityBinding.setBatchSize( Helper.getIntValue( entityElement.getBatchSize(), 0 ) );
		entityBinding.setSelectBeforeUpdate( entityElement.isSelectBeforeUpdate() );
		entityBinding.setAbstract( entityElement.isAbstract() );

		if ( entityElement.getLoader() != null ) {
			entityBinding.setCustomLoaderName( entityElement.getLoader().getQueryRef() );
		}

		final XMLSqlInsertElement sqlInsert = entityElement.getSqlInsert();
		if ( sqlInsert != null ) {
			entityBinding.setCustomInsert( Helper.buildCustomSql( sqlInsert ) );
		}

		final XMLSqlDeleteElement sqlDelete = entityElement.getSqlDelete();
		if ( sqlDelete != null ) {
			entityBinding.setCustomDelete( Helper.buildCustomSql( sqlDelete ) );
		}

		final XMLSqlUpdateElement sqlUpdate = entityElement.getSqlUpdate();
		if ( sqlUpdate != null ) {
			entityBinding.setCustomUpdate( Helper.buildCustomSql( sqlUpdate ) );
		}

		if ( entityElement.getSynchronize() != null ) {
			for ( XMLSynchronizeElement synchronize : entityElement.getSynchronize() ) {
				entityBinding.addSynchronizedTable( synchronize.getTable() );
			}
		}
	}

	private String extractCustomTuplizerClassName(EntityElement entityMapping, EntityMode entityMode) {
		if ( entityMapping.getTuplizer() == null ) {
			return null;
		}
		for ( XMLTuplizerElement tuplizerElement : entityMapping.getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
	}

	private void bindPrimaryTable(EntitySourceInformation entitySourceInformation, EntityBinding entityBinding) {
		final EntityElement entityElement = entitySourceInformation.getEntityElement();
		final HbmBindingContext bindingContext = entitySourceInformation.getSourceMappingDocument().getMappingLocalBindingContext();

		if ( XMLSubclassElement.class.isInstance( entityElement ) ) {
			// todo : need to look it up from root entity, or have persister manage it
		}
		else {
			// todo : add mixin interface
			final String explicitTableName;
			final String explicitSchemaName;
			final String explicitCatalogName;
			if ( XMLHibernateMapping.XMLClass.class.isInstance( entityElement ) ) {
				explicitTableName = ( (XMLHibernateMapping.XMLClass) entityElement ).getTable();
				explicitSchemaName = ( (XMLHibernateMapping.XMLClass) entityElement ).getSchema();
				explicitCatalogName = ( (XMLHibernateMapping.XMLClass) entityElement ).getCatalog();
			}
			else if ( XMLJoinedSubclassElement.class.isInstance( entityElement ) ) {
				explicitTableName = ( (XMLJoinedSubclassElement) entityElement ).getTable();
				explicitSchemaName = ( (XMLJoinedSubclassElement) entityElement ).getSchema();
				explicitCatalogName = ( (XMLJoinedSubclassElement) entityElement ).getCatalog();
			}
			else if ( XMLUnionSubclassElement.class.isInstance( entityElement ) ) {
				explicitTableName = ( (XMLUnionSubclassElement) entityElement ).getTable();
				explicitSchemaName = ( (XMLUnionSubclassElement) entityElement ).getSchema();
				explicitCatalogName = ( (XMLUnionSubclassElement) entityElement ).getCatalog();
			}
			else {
				// throw up
				explicitTableName = null;
				explicitSchemaName = null;
				explicitCatalogName = null;
			}
			final NamingStrategy namingStrategy = bindingContext.getMetadataImplementor()
					.getOptions()
					.getNamingStrategy();
			final String tableName = explicitTableName != null
					? namingStrategy.tableName( explicitTableName )
					: namingStrategy.tableName( namingStrategy.classToTableName( entityBinding.getEntity().getName() ) );

			final String schemaName = explicitSchemaName == null
					? bindingContext.getMappingDefaults().getSchemaName()
					: explicitSchemaName;
			final String catalogName = explicitCatalogName == null
					? bindingContext.getMappingDefaults().getCatalogName()
					: explicitCatalogName;

			final Schema schema = metadata.getDatabase().getSchema( new Schema.Name( schemaName, catalogName ) );
			entityBinding.setBaseTable( schema.locateOrCreateTable( Identifier.toIdentifier( tableName ) ) );
		}
	}


	private Stack<TableSpecification> attributeColumnTableStack = new Stack<TableSpecification>();

	private void bindIdentifier(EntityBinding entityBinding, EntitySourceInformation entitySourceInfo) {
		final XMLHibernateMapping.XMLClass rootClassElement = (XMLHibernateMapping.XMLClass) entitySourceInfo.getEntityElement();
		if ( rootClassElement.getId() != null ) {
			bindSimpleIdentifierAttribute( entityBinding, entitySourceInfo );
		}
		else if ( rootClassElement.getCompositeId() != null ) {
			bindCompositeIdentifierAttribute( entityBinding, entitySourceInfo );
		}
	}

	private void bindSimpleIdentifierAttribute(EntityBinding entityBinding, EntitySourceInformation entitySourceInfo) {
		final XMLHibernateMapping.XMLClass.XMLId idElement = ( (XMLHibernateMapping.XMLClass) entitySourceInfo.getEntityElement() ).getId();
		final String idAttributeName = idElement.getName() == null
				? "id"
				: idElement.getName();

		final SimpleAttributeBinding idAttributeBinding = doBasicSimpleAttributeBindingCreation(
				idAttributeName,
				idElement,
				entityBinding
		);

		idAttributeBinding.setInsertable( false );
		idAttributeBinding.setUpdatable( false );
		idAttributeBinding.setGeneration( PropertyGeneration.INSERT );
		idAttributeBinding.setLazy( false );
		idAttributeBinding.setIncludedInOptimisticLocking( false );

		final org.hibernate.metamodel.relational.Value relationalValue = makeValue(
				new RelationValueMetadataSource() {
					@Override
					public String getColumnAttribute() {
						return idElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List getColumnOrFormulaElements() {
						return idElement.getColumn();
					}
				},
				idAttributeBinding
		);

		idAttributeBinding.setValue( relationalValue );
		if ( SimpleValue.class.isInstance( relationalValue ) ) {
			if ( !Column.class.isInstance( relationalValue ) ) {
				// this should never ever happen..
				throw new MappingException( "Simple ID is not a column.", currentBindingContext.getOrigin() );
			}
			entityBinding.getBaseTable().getPrimaryKey().addColumn( Column.class.cast( relationalValue ) );
		}
		else {
			for ( SimpleValue subValue : ( (Tuple) relationalValue ).values() ) {
				if ( Column.class.isInstance( subValue ) ) {
					entityBinding.getBaseTable().getPrimaryKey().addColumn( Column.class.cast( subValue ) );
				}
			}
		}
	}

	private void bindCompositeIdentifierAttribute(
			EntityBinding entityBinding,
			EntitySourceInformation entitySourceInfo) {
		//To change body of created methods use File | Settings | File Templates.
	}

	private void bindAttributes(final EntitySourceInformation entitySourceInformation, EntityBinding entityBinding) {
		// todo : we really need the notion of a Stack here for the table from which the columns come for binding these attributes.
		// todo : adding the concept (interface) of a source of attribute metadata would allow reuse of this method for entity, component, unique-key, etc
		// for now, simply assume all columns come from the base table....

		attributeColumnTableStack.push( entityBinding.getBaseTable() );
		try {
			bindAttributes(
					new AttributeMetadataContainer() {
						@Override
						public List<Object> getAttributeElements() {
							return entitySourceInformation.getEntityElement().getPropertyOrManyToOneOrOneToOne();
						}
					},
					entityBinding
			);
		}
		finally {
			attributeColumnTableStack.pop();
		}

	}

	private void bindAttributes(AttributeMetadataContainer attributeMetadataContainer, EntityBinding entityBinding) {
		for ( Object attribute : attributeMetadataContainer.getAttributeElements() ) {

			if ( XMLPropertyElement.class.isInstance( attribute ) ) {
				XMLPropertyElement property = XMLPropertyElement.class.cast( attribute );
				bindProperty( property, entityBinding );
			}
			else if ( XMLManyToOneElement.class.isInstance( attribute ) ) {
				XMLManyToOneElement manyToOne = XMLManyToOneElement.class.cast( attribute );
				makeManyToOneAttributeBinding( manyToOne, entityBinding );
			}
			else if ( XMLOneToOneElement.class.isInstance( attribute ) ) {
// todo : implement
// value = new OneToOne( mappings, table, persistentClass );
// bindOneToOne( subElement, (OneToOne) value, propertyName, true, mappings );
			}
			else if ( XMLBagElement.class.isInstance( attribute ) ) {
				XMLBagElement collection = XMLBagElement.class.cast( attribute );
				BagBinding collectionBinding = makeBagAttributeBinding( collection, entityBinding );
				metadata.addCollection( collectionBinding );
			}
			else if ( XMLIdbagElement.class.isInstance( attribute ) ) {
				XMLIdbagElement collection = XMLIdbagElement.class.cast( attribute );
//BagBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
//bindIdbag( collection, bagBinding, entityBinding, PluralAttributeNature.BAG, collection.getName() );
// todo: handle identifier
//attributeBinding = collectionBinding;
//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLSetElement.class.isInstance( attribute ) ) {
				XMLSetElement collection = XMLSetElement.class.cast( attribute );
//BagBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
//bindSet( collection, collectionBinding, entityBinding, PluralAttributeNature.SET, collection.getName() );
//attributeBinding = collectionBinding;
//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLListElement.class.isInstance( attribute ) ) {
				XMLListElement collection = XMLListElement.class.cast( attribute );
//ListBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
//bindList( collection, bagBinding, entityBinding, PluralAttributeNature.LIST, collection.getName() );
// todo : handle list index
//attributeBinding = collectionBinding;
//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLMapElement.class.isInstance( attribute ) ) {
				XMLMapElement collection = XMLMapElement.class.cast( attribute );
//BagBinding bagBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
//bindMap( collection, bagBinding, entityBinding, PluralAttributeNature.MAP, collection.getName() );
// todo : handle map key
//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLAnyElement.class.isInstance( attribute ) ) {
// todo : implement
// value = new Any( mappings, table );
// bindAny( subElement, (Any) value, nullable, mappings );
			}
			else if ( XMLComponentElement.class.isInstance( attribute )
			|| XMLDynamicComponentElement.class.isInstance( attribute )
			|| XMLPropertiesElement.class.isInstance( attribute ) ) {
// todo : implement
// String subpath = StringHelper.qualify( entityName, propertyName );
// value = new Component( mappings, persistentClass );
//
// bindComponent(
// subElement,
// (Component) value,
// persistentClass.getClassName(),
// propertyName,
// subpath,
// true,
// "properties".equals( subElementName ),
// mappings,
// inheritedMetas,
// false
// );
			}
		}
	}

	private void bindProperty(final XMLPropertyElement property, EntityBinding entityBinding) {
		SimpleAttributeBinding attributeBinding = doBasicSimpleAttributeBindingCreation( property.getName(), property, entityBinding );

		attributeBinding.setInsertable( Helper.getBooleanValue( property.isInsert(), true ) );
		attributeBinding.setUpdatable( Helper.getBooleanValue( property.isUpdate(), true ) );
		attributeBinding.setGeneration( PropertyGeneration.parse( property.getGenerated() ) );
		attributeBinding.setLazy( property.isLazy() );
		attributeBinding.setIncludedInOptimisticLocking( property.isOptimisticLock() );

// todo : implement.  Is this meant to indicate the natural-id?
//		attributeBinding.setAlternateUniqueKey( ... );

		attributeBinding.setValue(
				makeValue(
						new RelationValueMetadataSource() {
							@Override
							public String getColumnAttribute() {
								return property.getColumn();
							}

							@Override
							public String getFormulaAttribute() {
								return property.getFormula();
							}

							@Override
							public List getColumnOrFormulaElements() {
								return property.getColumnOrFormula();
							}
						},
						attributeBinding
				)
		);
	}

	private SimpleAttributeBinding doBasicSimpleAttributeBindingCreation(
			String attributeName,
			SingularAttributeSource attributeSource,
			EntityBinding entityBinding) {
		// todo : the need to pass in the attribute name here could be alleviated by making name required on <id/> etc
		SingularAttribute attribute = entityBinding.getEntity().locateOrCreateSingularAttribute( attributeName );
		SimpleAttributeBinding attributeBinding = entityBinding.makeSimpleAttributeBinding( attribute );
		resolveTypeInformation( attributeSource, attributeBinding );

		attributeBinding.setPropertyAccessorName(
				Helper.getPropertyAccessorName(
						attributeSource.getAccess(),
						false,
						currentBindingContext.getMappingDefaults().getPropertyAccessorName()
				)
		);

		attributeBinding.setMetaAttributeContext(
				Helper.extractMetaAttributeContext( attributeSource.getMeta(), entityBinding.getMetaAttributeContext() )
		);

		return attributeBinding;
	}

	private void resolveTypeInformation(SingularAttributeSource property, final SimpleAttributeBinding attributeBinding) {
		final Class<?> attributeJavaType = determineJavaType( attributeBinding.getAttribute() );
		if ( attributeJavaType != null ) {
			attributeBinding.getHibernateTypeDescriptor().setJavaTypeName( attributeJavaType.getName() );
			( (AbstractAttributeContainer.SingularAttributeImpl) attributeBinding.getAttribute() ).resolveType(
					currentBindingContext.makeJavaType( attributeJavaType.getName() )
			);
		}

		// prefer type attribute over nested <type/> element
		if ( property.getTypeAttribute() != null ) {
			final String explicitTypeName = property.getTypeAttribute();
			final TypeDef typeDef = currentBindingContext.getMetadataImplementor().getTypeDefinition( explicitTypeName );
			if ( typeDef != null ) {
				attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( typeDef.getTypeClass() );
				attributeBinding.getHibernateTypeDescriptor().getTypeParameters().putAll( typeDef.getParameters() );
			}
			else {
				attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( explicitTypeName );
			}
		}
		else if ( property.getType() != null ) {
			// todo : consider changing in-line type definitions to implicitly generate uniquely-named type-defs
			attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( property.getType().getName() );
			for ( XMLParamElement xmlParamElement : property.getType().getParam() ) {
				attributeBinding.getHibernateTypeDescriptor().getTypeParameters().put(
						xmlParamElement.getName(),
						xmlParamElement.getValue()
				);
			}
		}
		else {
			if ( attributeJavaType == null ) {
				// we will have problems later determining the Hibernate Type to use.  Should we throw an
				// exception now?  Might be better to get better contextual info
			}
		}
	}

	private Class<?> determineJavaType(final Attribute attribute) {
		try {
			final Class ownerClass = attribute.getAttributeContainer().getClassReference();
			AttributeJavaTypeDeterminerDelegate delegate = new AttributeJavaTypeDeterminerDelegate( attribute.getName() );
			BeanInfoHelper.visitBeanInfo( ownerClass, delegate );
			return delegate.javaType;
		}
		catch ( Exception ignore ) {
			// todo : log it?
		}
		return null;
	}

	private static class AttributeJavaTypeDeterminerDelegate implements BeanInfoHelper.BeanInfoDelegate {
		private final String attributeName;
		private Class<?> javaType = null;

		private AttributeJavaTypeDeterminerDelegate(String attributeName) {
			this.attributeName = attributeName;
		}

		@Override
		public void processBeanInfo(BeanInfo beanInfo) throws Exception {
			for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
				if ( propertyDescriptor.getName().equals( attributeName ) ) {
					javaType = propertyDescriptor.getPropertyType();
					break;
				}
			}
		}
	}

	private static interface RelationValueMetadataSource {
		public String getColumnAttribute();
		public String getFormulaAttribute();
		public List getColumnOrFormulaElements();
	}

	private org.hibernate.metamodel.relational.Value makeValue(
			RelationValueMetadataSource relationValueMetadataSource,
			SimpleAttributeBinding attributeBinding) {
		// todo : to be completely correct, we need to know which table the value belongs to.
		// 		There is a note about this somewhere else with ideas on the subject.
		//		For now, just use the entity's base table.
		final TableSpecification valueSource = attributeBinding.getEntityBinding().getBaseTable();

		if ( StringHelper.isNotEmpty( relationValueMetadataSource.getColumnAttribute() ) ) {
			if ( relationValueMetadataSource.getColumnOrFormulaElements() != null
					&& ! relationValueMetadataSource.getColumnOrFormulaElements().isEmpty() ) {
				throw new MappingException(
						"column/formula attribute may not be used together with <column>/<formula> subelement",
						currentBindingContext.getOrigin()
				);
			}
			if ( StringHelper.isNotEmpty( relationValueMetadataSource.getFormulaAttribute() ) ) {
				throw new MappingException(
						"column and formula attributes may not be used together",
						currentBindingContext.getOrigin()
				);
			}
			return valueSource.locateOrCreateColumn( relationValueMetadataSource.getColumnAttribute() );
		}
		else if ( StringHelper.isNotEmpty( relationValueMetadataSource.getFormulaAttribute() ) ) {
			if ( relationValueMetadataSource.getColumnOrFormulaElements() != null
					&& ! relationValueMetadataSource.getColumnOrFormulaElements().isEmpty() ) {
				throw new MappingException(
						"column/formula attribute may not be used together with <column>/<formula> subelement",
						currentBindingContext.getOrigin()
				);
			}
			// column/formula attribute combo checked already
			return valueSource.locateOrCreateDerivedValue( relationValueMetadataSource.getFormulaAttribute() );
		}
		else if ( relationValueMetadataSource.getColumnOrFormulaElements() != null
				&& ! relationValueMetadataSource.getColumnOrFormulaElements().isEmpty() ) {
			List<SimpleValue> values = new ArrayList<SimpleValue>();
			for ( Object columnOrFormula : relationValueMetadataSource.getColumnOrFormulaElements() ) {
				final SimpleValue value;
				if ( XMLColumnElement.class.isInstance( columnOrFormula ) ) {
					final XMLColumnElement columnElement = (XMLColumnElement) columnOrFormula;
					final Column column = valueSource.locateOrCreateColumn( columnElement.getName() );
					column.setNullable( ! columnElement.isNotNull() );
					column.setDefaultValue( columnElement.getDefault() );
					column.setSqlType( columnElement.getSqlType() );
					column.setSize(
							new Size(
									Helper.getIntValue( columnElement.getPrecision(), -1 ),
									Helper.getIntValue( columnElement.getScale(), -1 ),
									Helper.getLongValue( columnElement.getLength(), -1 ),
									Size.LobMultiplier.NONE
							)
					);
					column.setDatatype( null ); // todo : ???
					column.setReadFragment( columnElement.getRead() );
					column.setWriteFragment( columnElement.getWrite() );
					column.setUnique( columnElement.isUnique() );
					column.setCheckCondition( columnElement.getCheck() );
					column.setComment( columnElement.getComment() );
					value = column;
				}
				else {
					value = valueSource.locateOrCreateDerivedValue( (String) columnOrFormula );
				}
				if ( value != null ) {
					values.add( value );
				}
			}

			if ( values.size() == 1 ) {
				return values.get( 0 );
			}

			final Tuple tuple = valueSource.createTuple(
					attributeBinding.getEntityBinding().getEntity().getName() + '.'
							+ attributeBinding.getAttribute().getName()
			);
			for ( SimpleValue value : values ) {
				tuple.addValue( value );
			}
			return tuple;
		}
		else {
			// assume a column named based on the NamingStrategy
			final String name = metadata.getOptions()
					.getNamingStrategy()
					.propertyToColumnName( attributeBinding.getAttribute().getName() );
			return valueSource.locateOrCreateColumn( name );
		}
	}

	private void makeManyToOneAttributeBinding(XMLManyToOneElement manyToOne, EntityBinding entityBinding) {
		//To change body of created methods use File | Settings | File Templates.
	}

	private BagBinding makeBagAttributeBinding(XMLBagElement collection, EntityBinding entityBinding) {
		return null;  //To change body of created methods use File | Settings | File Templates.
	}

	private void bindSecondaryTables(EntitySourceInformation entitySourceInfo, EntityBinding entityBinding) {
		final EntityElement entityElement = entitySourceInfo.getEntityElement();

		if ( ! ( entityElement instanceof JoinElementSource) ) {
			return;
		}

		for ( XMLJoinElement join : ( (JoinElementSource) entityElement ).getJoin() ) {
			// todo : implement
			// Join join = new Join();
			// join.setPersistentClass( persistentClass );
			// bindJoin( subElement, join, mappings, inheritedMetas );
			// persistentClass.addJoin( join );
		}
	}

	private void bindTableUniqueConstraints(EntityBinding entityBinding) {
		//To change body of created methods use File | Settings | File Templates.
	}

	private static interface AttributeMetadataContainer {
		public List<Object> getAttributeElements();
	}
}
