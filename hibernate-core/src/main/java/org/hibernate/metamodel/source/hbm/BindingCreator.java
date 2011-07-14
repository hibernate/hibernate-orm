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
import java.util.HashMap;
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
import org.hibernate.metamodel.binding.BagBinding;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.MetaAttribute;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.EntityElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.JoinElementSource;
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
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLMetaElement;
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
		bindVersion( entityBinding, entitySourceInfo );
		bindDiscriminator( entityBinding, entitySourceInfo );

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
		final SimpleAttributeBinding idAttributeBinding = doBasicSimpleAttributeBindingCreation(
				new SimpleIdentifierAttributeSource( idElement ),
				entityBinding
		);

		entityBinding.getEntityIdentifier().setValueBinding( idAttributeBinding );

		final org.hibernate.metamodel.relational.Value relationalValue = idAttributeBinding.getValue();

		if ( idElement.getGenerator() != null ) {
			final String generatorName = idElement.getGenerator().getClazz();
			IdGenerator idGenerator = currentBindingContext.getMetadataImplementor().getIdGenerator( generatorName );
			if ( idGenerator == null ) {
				idGenerator = new IdGenerator(
						entityBinding.getEntity().getName() + generatorName,
						generatorName,
						extractParameters( idElement.getGenerator().getParam() )
				);
			}
			entityBinding.getEntityIdentifier().setIdGenerator( idGenerator );
		}

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

	private SimpleAttributeBinding doBasicSimpleAttributeBindingCreation(
			SimpleAttributeSource simpleAttributeSource,
			EntityBinding entityBinding) {
		final SingularAttribute attribute = entityBinding.getEntity().locateOrCreateSingularAttribute( simpleAttributeSource.getName() );
		final SimpleAttributeBinding attributeBinding = entityBinding.makeSimpleAttributeBinding( attribute );
		resolveTypeInformation( simpleAttributeSource.getTypeInformation(), attributeBinding );

		attributeBinding.setInsertable( simpleAttributeSource.isInsertable() );
		attributeBinding.setUpdatable( simpleAttributeSource.isUpdatable() );
		attributeBinding.setGeneration( simpleAttributeSource.getGeneration() );
		attributeBinding.setLazy( simpleAttributeSource.isLazy() );
		attributeBinding.setIncludedInOptimisticLocking( simpleAttributeSource.isIncludedInOptimisticLocking() );

		attributeBinding.setPropertyAccessorName(
				Helper.getPropertyAccessorName(
						simpleAttributeSource.getPropertyAccessorName(),
						false,
						currentBindingContext.getMappingDefaults().getPropertyAccessorName()
				)
		);

		final org.hibernate.metamodel.relational.Value relationalValue = makeValue(
				simpleAttributeSource.getValueInformation(), attributeBinding
		);
		attributeBinding.setValue( relationalValue );

		attributeBinding.setMetaAttributeContext(
				extractMetaAttributeContext(
						simpleAttributeSource.metaAttributes(),
						entityBinding.getMetaAttributeContext()
				)
		);

		return attributeBinding;
	}

	private void bindCompositeIdentifierAttribute(
			EntityBinding entityBinding,
			EntitySourceInformation entitySourceInfo) {
		//To change body of created methods use File | Settings | File Templates.
	}

	private void bindVersion(EntityBinding entityBinding, EntitySourceInformation entitySourceInfo) {
		final XMLHibernateMapping.XMLClass rootClassElement = (XMLHibernateMapping.XMLClass) entitySourceInfo.getEntityElement();
		final XMLHibernateMapping.XMLClass.XMLVersion versionElement = rootClassElement.getVersion();
		final XMLHibernateMapping.XMLClass.XMLTimestamp timestampElement = rootClassElement.getTimestamp();

		if ( versionElement == null && timestampElement == null ) {
			return;
		}
		else if ( versionElement != null && timestampElement != null ) {
			throw new MappingException( "version and timestamp elements cannot be specified together", currentBindingContext.getOrigin() );
		}

		final SimpleAttributeBinding attributeBinding;
		if ( versionElement != null ) {
			attributeBinding = doBasicSimpleAttributeBindingCreation(
					new VersionAttributeSource( versionElement ),
					entityBinding
			);
		}
		else {
			attributeBinding = doBasicSimpleAttributeBindingCreation(
					new TimestampAttributeSource( timestampElement ),
					entityBinding
			);
		}

		entityBinding.setVersionBinding( attributeBinding );
	}

	private void bindDiscriminator(EntityBinding entityBinding, EntitySourceInformation entitySourceInfo) {
		// discriminator is a tad different in that it is a "virtual attribute" because it does not exist in the
		// actual domain model.
		final XMLHibernateMapping.XMLClass rootClassElement = (XMLHibernateMapping.XMLClass) entitySourceInfo.getEntityElement();
		final XMLHibernateMapping.XMLClass.XMLDiscriminator discriminatorElement = rootClassElement.getDiscriminator();
		if ( discriminatorElement == null ) {
			return;
		}

		// todo ...
	}

	private void bindAttributes(final EntitySourceInformation entitySourceInformation, EntityBinding entityBinding) {
		// todo : we really need the notion of a Stack here for the table from which the columns come for binding these attributes.
		// todo : adding the concept (interface) of a source of attribute metadata would allow reuse of this method for entity, component, unique-key, etc
		// for now, simply assume all columns come from the base table....

		// todo : intg with "attribute source" concept below as means for single hbm/annotation handling

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
		doBasicSimpleAttributeBindingCreation( new PropertyAttributeSource( property ), entityBinding );
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


	// Initial prototype/sandbox for notion of "orchestrated information collection from sources" ~~~~~~~~~~~~~~~~~~~~~~

	private static enum SimpleAttributeNature { BASIC, MANY_TO_ONE, ONE_TO_ONE, ANY };

	private interface SimpleAttributeSource {
		public String getName();
		public ExplicitHibernateTypeSource getTypeInformation();
		public String getPropertyAccessorName();
		public boolean isInsertable();
		public boolean isUpdatable();
		public PropertyGeneration getGeneration();
		public boolean isLazy();
		public boolean isIncludedInOptimisticLocking();

		public SimpleAttributeNature getNature();

		public boolean isVirtualAttribute();

		public RelationValueMetadataSource getValueInformation();

		public Iterable<MetaAttributeSource> metaAttributes();
	}

	private interface ExplicitHibernateTypeSource {
		public String getName();
		public Map<String,String> getParameters();
	}

	private interface MetaAttributeSource {
		public String getName();
		public String getValue();
		public boolean isInheritable();
	}

	private static interface RelationValueMetadataSource {
		public String getColumnAttribute();
		public String getFormulaAttribute();
		public List getColumnOrFormulaElements();
	}


	// HBM specific implementations of "attribute sources" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Implementation for {@code <property/>} mappings
	 */
	private class PropertyAttributeSource implements SimpleAttributeSource {
		private final XMLPropertyElement propertyElement;
		private final ExplicitHibernateTypeSource typeSource;
		private final RelationValueMetadataSource valueSource;

		private PropertyAttributeSource(final XMLPropertyElement propertyElement) {
			this.propertyElement = propertyElement;
			this.typeSource = new ExplicitHibernateTypeSource() {
				private final String name = propertyElement.getTypeAttribute() != null
						? propertyElement.getTypeAttribute()
						: propertyElement.getType() != null
								? propertyElement.getType().getName()
								: null;
				private final Map<String, String> parameters = ( propertyElement.getType() != null )
						? extractParameters( propertyElement.getType().getParam() )
						: null;

				@Override
				public String getName() {
					return name;
				}

				@Override
				public Map<String, String> getParameters() {
					return parameters;
				}
			};
			this.valueSource = new RelationValueMetadataSource() {
				@Override
				public String getColumnAttribute() {
					return propertyElement.getColumn();
				}

				@Override
				public String getFormulaAttribute() {
					return propertyElement.getFormula();
				}

				@Override
				public List getColumnOrFormulaElements() {
					return propertyElement.getColumnOrFormula();
				}
			};
		}

		@Override
		public String getName() {
			return propertyElement.getName();
		}

		@Override
		public ExplicitHibernateTypeSource getTypeInformation() {
			return typeSource;
		}

		@Override
		public String getPropertyAccessorName() {
			return propertyElement.getAccess();
		}

		@Override
		public boolean isInsertable() {
			return Helper.getBooleanValue( propertyElement.isInsert(), true );
		}

		@Override
		public boolean isUpdatable() {
			return Helper.getBooleanValue( propertyElement.isUpdate(), true );
		}

		@Override
		public PropertyGeneration getGeneration() {
			return PropertyGeneration.parse( propertyElement.getGenerated() );
		}

		@Override
		public boolean isLazy() {
			return Helper.getBooleanValue( propertyElement.isLazy(), false );
		}

		@Override
		public boolean isIncludedInOptimisticLocking() {
			return Helper.getBooleanValue( propertyElement.isOptimisticLock(), true );
		}

		@Override
		public SimpleAttributeNature getNature() {
			return SimpleAttributeNature.BASIC;
		}

		@Override
		public boolean isVirtualAttribute() {
			return false;
		}

		@Override
		public RelationValueMetadataSource getValueInformation() {
			return valueSource;
		}

		@Override
		public Iterable<MetaAttributeSource> metaAttributes() {
			return buildMetaAttributeSources( propertyElement.getMeta() );
		}
	}

	/**
	 * Implementation for {@code <id/>} mappings
	 */
	private class SimpleIdentifierAttributeSource implements SimpleAttributeSource {
		private final XMLHibernateMapping.XMLClass.XMLId idElement;
		private final ExplicitHibernateTypeSource typeSource;
		private final RelationValueMetadataSource valueSource;

		public SimpleIdentifierAttributeSource(final XMLHibernateMapping.XMLClass.XMLId idElement) {
			this.idElement = idElement;
			this.typeSource = new ExplicitHibernateTypeSource() {
				private final String name = idElement.getTypeAttribute() != null
						? idElement.getTypeAttribute()
						: idElement.getType() != null
								? idElement.getType().getName()
								: null;
				private final Map<String, String> parameters = ( idElement.getType() != null )
						? extractParameters( idElement.getType().getParam() )
						: null;

				@Override
				public String getName() {
					return name;
				}

				@Override
				public Map<String, String> getParameters() {
					return parameters;
				}
			};
			this.valueSource = new RelationValueMetadataSource() {
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
			};
		}

		@Override
		public String getName() {
			return idElement.getName() == null
					? "id"
					: idElement.getName();
		}

		@Override
		public ExplicitHibernateTypeSource getTypeInformation() {
			return typeSource;
		}

		@Override
		public String getPropertyAccessorName() {
			return idElement.getAccess();
		}

		@Override
		public boolean isInsertable() {
			return true;
		}

		@Override
		public boolean isUpdatable() {
			return false;
		}

		@Override
		public PropertyGeneration getGeneration() {
			return PropertyGeneration.INSERT;
		}

		@Override
		public boolean isLazy() {
			return false;
		}

		@Override
		public boolean isIncludedInOptimisticLocking() {
			return false;
		}

		@Override
		public SimpleAttributeNature getNature() {
			return SimpleAttributeNature.BASIC;
		}

		@Override
		public boolean isVirtualAttribute() {
			return false;
		}

		@Override
		public RelationValueMetadataSource getValueInformation() {
			return valueSource;
		}

		@Override
		public Iterable<MetaAttributeSource> metaAttributes() {
			return buildMetaAttributeSources( idElement.getMeta() );
		}
	}

	/**
	 * Implementation for {@code <version/>} mappings
	 */
	private class VersionAttributeSource implements SimpleAttributeSource {
		private final XMLHibernateMapping.XMLClass.XMLVersion versionElement;

		private VersionAttributeSource(XMLHibernateMapping.XMLClass.XMLVersion versionElement) {
			this.versionElement = versionElement;
		}

		private final ExplicitHibernateTypeSource typeSource = new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				return versionElement.getType() == null ? "integer" : versionElement.getType();
			}

			@Override
			public Map<String, String> getParameters() {
				return null;
			}
		};

		private final RelationValueMetadataSource valueSource = new RelationValueMetadataSource() {
			@Override
			public String getColumnAttribute() {
				return versionElement.getColumnAttribute();
			}

			@Override
			public String getFormulaAttribute() {
				return null;
			}

			@Override
			public List getColumnOrFormulaElements() {
				return versionElement.getColumn();
			}
		};

		@Override
		public String getName() {
			return versionElement.getName();
		}

		@Override
		public ExplicitHibernateTypeSource getTypeInformation() {
			return typeSource;
		}

		@Override
		public String getPropertyAccessorName() {
			return versionElement.getAccess();
		}

		@Override
		public boolean isInsertable() {
			return Helper.getBooleanValue( versionElement.isInsert(), true );
		}

		@Override
		public boolean isUpdatable() {
			return true;
		}

		private Value<PropertyGeneration> propertyGenerationValue = new Value<PropertyGeneration>(
				new Value.DeferredInitializer<PropertyGeneration>() {
					@Override
					public PropertyGeneration initialize() {
						final PropertyGeneration propertyGeneration = versionElement.getGenerated() == null
								? PropertyGeneration.NEVER
								: PropertyGeneration.parse( versionElement.getGenerated().value() );
						if ( propertyGeneration == PropertyGeneration.INSERT ) {
							throw new MappingException(
									"'generated' attribute cannot be 'insert' for versioning property",
									currentBindingContext.getOrigin()
							);
						}
						return propertyGeneration;
					}
				}
		);

		@Override
		public PropertyGeneration getGeneration() {
			return propertyGenerationValue.getValue();
		}

		@Override
		public boolean isLazy() {
			return false;
		}

		@Override
		public boolean isIncludedInOptimisticLocking() {
			return false;
		}

		@Override
		public SimpleAttributeNature getNature() {
			return SimpleAttributeNature.BASIC;
		}

		@Override
		public boolean isVirtualAttribute() {
			return false;
		}

		@Override
		public RelationValueMetadataSource getValueInformation() {
			return valueSource;
		}

		@Override
		public Iterable<MetaAttributeSource> metaAttributes() {
			return buildMetaAttributeSources( versionElement.getMeta() );
		}
	}

	/**
	 * Implementation for {@code <timestamp/>} mappings
	 */
	private class TimestampAttributeSource implements SimpleAttributeSource {
		private final XMLHibernateMapping.XMLClass.XMLTimestamp timestampElement;

		private TimestampAttributeSource(XMLHibernateMapping.XMLClass.XMLTimestamp timestampElement) {
			this.timestampElement = timestampElement;
		}

		private final ExplicitHibernateTypeSource typeSource = new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				return "db".equals( timestampElement.getSource() ) ? "dbtimestamp" : "timestamp";
			}

			@Override
			public Map<String, String> getParameters() {
				return null;
			}
		};

		private final RelationValueMetadataSource valueSource = new RelationValueMetadataSource() {
			@Override
			public String getColumnAttribute() {
				return timestampElement.getColumn();
			}

			@Override
			public String getFormulaAttribute() {
				return null;
			}

			@Override
			public List getColumnOrFormulaElements() {
				return null;
			}
		};

		@Override
		public String getName() {
			return timestampElement.getName();
		}

		@Override
		public ExplicitHibernateTypeSource getTypeInformation() {
			return typeSource;
		}

		@Override
		public String getPropertyAccessorName() {
			return timestampElement.getAccess();
		}

		@Override
		public boolean isInsertable() {
			return true;
		}

		@Override
		public boolean isUpdatable() {
			return true;
		}

		private Value<PropertyGeneration> propertyGenerationValue = new Value<PropertyGeneration>(
				new Value.DeferredInitializer<PropertyGeneration>() {
					@Override
					public PropertyGeneration initialize() {
						final PropertyGeneration propertyGeneration = timestampElement.getGenerated() == null
								? PropertyGeneration.NEVER
								: PropertyGeneration.parse( timestampElement.getGenerated().value() );
						if ( propertyGeneration == PropertyGeneration.INSERT ) {
							throw new MappingException(
									"'generated' attribute cannot be 'insert' for versioning property",
									currentBindingContext.getOrigin()
							);
						}
						return propertyGeneration;
					}
				}
		);

		@Override
		public PropertyGeneration getGeneration() {
			return propertyGenerationValue.getValue();
		}

		@Override
		public boolean isLazy() {
			return false;
		}

		@Override
		public boolean isIncludedInOptimisticLocking() {
			return false;
		}

		@Override
		public SimpleAttributeNature getNature() {
			return SimpleAttributeNature.BASIC;
		}

		@Override
		public boolean isVirtualAttribute() {
			return false;
		}

		@Override
		public RelationValueMetadataSource getValueInformation() {
			return valueSource;
		}

		@Override
		public Iterable<MetaAttributeSource> metaAttributes() {
			return buildMetaAttributeSources( timestampElement.getMeta() );
		}
	}


	// Helpers for building "attribute sources" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void resolveTypeInformation(ExplicitHibernateTypeSource typeSource, SimpleAttributeBinding attributeBinding) {
		final Class<?> attributeJavaType = determineJavaType( attributeBinding.getAttribute() );
		if ( attributeJavaType != null ) {
			attributeBinding.getHibernateTypeDescriptor().setJavaTypeName( attributeJavaType.getName() );
			attributeBinding.getAttribute().resolveType( currentBindingContext.makeJavaType( attributeJavaType.getName() ) );
		}

		final String explicitTypeName = typeSource.getName();
		if ( explicitTypeName != null ) {
			final TypeDef typeDef = currentBindingContext.getMetadataImplementor().getTypeDefinition( explicitTypeName );
			if ( typeDef != null ) {
				attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( typeDef.getTypeClass() );
				attributeBinding.getHibernateTypeDescriptor().getTypeParameters().putAll( typeDef.getParameters() );
			}
			else {
				attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( explicitTypeName );
			}
			final Map<String,String> parameters = typeSource.getParameters();
			if ( parameters != null ) {
				attributeBinding.getHibernateTypeDescriptor().getTypeParameters().putAll( parameters );
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

	private Map<String, String> extractParameters(List<XMLParamElement> xmlParamElements) {
		if ( xmlParamElements == null || xmlParamElements.isEmpty() ) {
			return null;
		}
		final HashMap<String,String> params = new HashMap<String, String>();
		for ( XMLParamElement paramElement : xmlParamElements ) {
			params.put( paramElement.getName(), paramElement.getValue() );
		}
		return params;
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

	private MetaAttributeContext extractMetaAttributeContext(Iterable<MetaAttributeSource> sources, MetaAttributeContext parentContext) {
		return extractMetaAttributeContext( sources, false, parentContext );
	}

	public static MetaAttributeContext extractMetaAttributeContext(
			Iterable<MetaAttributeSource> sources,
			boolean onlyInheritable,
			MetaAttributeContext parentContext) {
		final MetaAttributeContext subContext = new MetaAttributeContext( parentContext );

		for ( MetaAttributeSource source : sources ) {
			if ( onlyInheritable & !source.isInheritable() ) {
				continue;
			}

			final String name = source.getName();
			final MetaAttribute inheritedMetaAttribute = parentContext.getMetaAttribute( name );
			MetaAttribute metaAttribute = subContext.getLocalMetaAttribute( name );
			if ( metaAttribute == null || metaAttribute == inheritedMetaAttribute ) {
				metaAttribute = new MetaAttribute( name );
				subContext.add( metaAttribute );
			}
			metaAttribute.addValue( source.getValue() );
		}

		return subContext;
	}

	private Iterable<MetaAttributeSource> buildMetaAttributeSources(List<XMLMetaElement> metaElements) {
		ArrayList<MetaAttributeSource> result = new ArrayList<MetaAttributeSource>();
		if ( metaElements == null || metaElements.isEmpty() ) {
			// do nothing
		}
		else {
			for ( final XMLMetaElement metaElement : metaElements ) {
				result.add(
						new MetaAttributeSource() {
							@Override
							public String getName() {
								return metaElement.getAttribute();
							}

							@Override
							public String getValue() {
								return metaElement.getValue();
							}

							@Override
							public boolean isInheritable() {
								return metaElement.isInherit();
							}
						}
				);
			}
		}
		return result;
	}
}
