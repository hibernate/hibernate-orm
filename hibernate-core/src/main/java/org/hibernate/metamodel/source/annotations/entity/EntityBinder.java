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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.GenerationType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.binding.state.DiscriminatorBindingState;
import org.hibernate.metamodel.binding.state.ManyToOneAttributeBindingState;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.AttributeContainer;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.UniqueKey;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.source.annotations.attribute.SimpleAttribute;
import org.hibernate.metamodel.source.annotations.attribute.state.binding.AttributeBindingStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.binding.DiscriminatorBindingStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.binding.ManyToOneBindingStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.relational.ColumnRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.relational.ManyToOneRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.relational.TupleRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.binding.EntityBindingStateImpl;
import org.hibernate.metamodel.source.annotations.global.IdGeneratorBinder;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Creates the domain and relational metamodel for a configured class and <i>binds</i> them together.
 *
 * @author Hardy Ferentschik
 */
public class EntityBinder {
	private final EntityClass entityClass;
	private final MetadataImplementor meta;
	private final Hierarchical superType;

	public EntityBinder(MetadataImplementor metadata, EntityClass entityClass, Hierarchical superType) {
		this.entityClass = entityClass;
		this.meta = metadata;
		this.superType = superType;
	}

	public EntityBinding bind() {
		EntityBinding entityBinding = new EntityBinding();
		EntityBindingStateImpl entityBindingState = new EntityBindingStateImpl( superType, entityClass );

		bindJpaEntityAnnotation( entityBindingState );
		bindHibernateEntityAnnotation( entityBindingState ); // optional hibernate specific @org.hibernate.annotations.Entity
		bindTable( entityBinding );

		// bind entity level annotations
		bindWhereFilter( entityBindingState );
		bindJpaCaching( entityBindingState );
		bindHibernateCaching( entityBindingState );
		bindProxy( entityBindingState );
		bindSynchronize( entityBindingState );
		bindCustomSQL( entityBindingState );
		bindRowId( entityBindingState );
		bindBatchSize( entityBindingState );
		entityBinding.initialize( meta, entityBindingState );

		bindInheritance( entityBinding );

		// bind all attributes - simple as well as associations
		bindAttributes( entityBinding );
		bindEmbeddedAttributes( entityBinding );

		// take care of the id, attributes and relations
		if ( entityClass.isEntityRoot() ) {
			bindId( entityBinding );
		}

		bindTableUniqueConstraints( entityBinding );

		// last, but not least we initialize and register the new EntityBinding
		meta.addEntity( entityBinding );
		return entityBinding;
	}

	private void bindTableUniqueConstraints(EntityBinding entityBinding) {
		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(),
				JPADotNames.TABLE
		);
		if ( tableAnnotation == null ) {
			return;
		}
		TableSpecification table = entityBinding.getBaseTable();
		bindUniqueConstraints( tableAnnotation, table );
	}

	/**
	 * Bind {@link javax.persistence.UniqueConstraint} to table as a {@link UniqueKey}
	 *
	 * @param tableAnnotation JPA annotations which has a {@code uniqueConstraints} attribute.
	 * @param table Table which the UniqueKey bind to.
	 */
	private void bindUniqueConstraints(AnnotationInstance tableAnnotation, TableSpecification table) {
		AnnotationValue value = tableAnnotation.value( "uniqueConstraints" );
		if ( value == null ) {
			return;
		}
		AnnotationInstance[] uniqueConstraints = value.asNestedArray();
		for ( AnnotationInstance unique : uniqueConstraints ) {
			String name = unique.value( "name" ).asString();
			UniqueKey uniqueKey = table.getOrCreateUniqueKey( name );
			String[] columnNames = unique.value( "columnNames" ).asStringArray();
			if ( columnNames.length == 0 ) {
				//todo throw exception?
			}
			for ( String columnName : columnNames ) {
				uniqueKey.addColumn( table.getOrCreateColumn( columnName ) );
			}
		}
	}

	private void bindInheritance(EntityBinding entityBinding) {
		entityBinding.setInheritanceType( entityClass.getInheritanceType() );
		switch ( entityClass.getInheritanceType() ) {
			case SINGLE_TABLE: {
				bindDiscriminatorColumn( entityBinding );
				break;
			}
			case JOINED: {
				// todo
				break;
			}
			case TABLE_PER_CLASS: {
				// todo
				break;
			}
			default: {
				// do nothing
			}
		}
	}

	private void bindDiscriminatorColumn(EntityBinding entityBinding) {
		final Map<DotName, List<AnnotationInstance>> typeAnnotations = JandexHelper.getTypeAnnotations(
				entityClass.getClassInfo()
		);
		SimpleAttribute discriminatorAttribute = SimpleAttribute.createDiscriminatorAttribute( typeAnnotations );
		bindSingleMappedAttribute( entityBinding, entityBinding.getEntity(), discriminatorAttribute );
	}

	private void bindWhereFilter(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.WHERE
		);
		if ( whereAnnotation != null ) {
			// no null check needed, it is a required attribute
			entityBindingState.setWhereFilter( JandexHelper.getValueAsString( whereAnnotation, "clause" ) );
		}
	}

	private void bindHibernateCaching(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance cacheAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.CACHE
		);
		if ( cacheAnnotation == null ) {
			return;
		}

		String region;
		if ( cacheAnnotation.value( "region" ) != null ) {
			region = cacheAnnotation.value( "region" ).asString();
		}
		else {
			region = entityBindingState.getEntityName();
		}

		boolean cacheLazyProperties = true;
		if ( cacheAnnotation.value( "include" ) != null ) {
			String tmp = cacheAnnotation.value( "include" ).asString();
			if ( "all".equalsIgnoreCase( tmp ) ) {
				cacheLazyProperties = true;
			}
			else if ( "non-lazy".equalsIgnoreCase( tmp ) ) {
				cacheLazyProperties = false;
			}
			else {
				throw new AnnotationException( "Unknown lazy property annotations: " + tmp );
			}
		}

		CacheConcurrencyStrategy strategy = CacheConcurrencyStrategy.valueOf(
				cacheAnnotation.value( "usage" ).asEnum()
		);
		Caching caching = new Caching( region, strategy.toAccessType(), cacheLazyProperties );
		entityBindingState.setCaching( caching );
	}

	// This does not take care of any inheritance of @Cacheable within a class hierarchy as specified in JPA2.
	// This is currently not supported (HF)
	private void bindJpaCaching(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance cacheAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), JPADotNames.CACHEABLE
		);

		boolean cacheable = true; // true is the default
		if ( cacheAnnotation != null && cacheAnnotation.value() != null ) {
			cacheable = cacheAnnotation.value().asBoolean();
		}

		Caching caching = null;
		switch ( meta.getOptions().getSharedCacheMode() ) {
			case ALL: {
				caching = createCachingForCacheableAnnotation( entityBindingState );
				break;
			}
			case ENABLE_SELECTIVE: {
				if ( cacheable ) {
					caching = createCachingForCacheableAnnotation( entityBindingState );
				}
				break;
			}
			case DISABLE_SELECTIVE: {
				if ( cacheAnnotation == null || cacheable ) {
					caching = createCachingForCacheableAnnotation( entityBindingState );
				}
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				break;
			}
		}
		if ( caching != null ) {
			entityBindingState.setCaching( caching );
		}
	}

	private void bindProxy(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance proxyAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.PROXY
		);
		boolean lazy = true;
		String proxyInterfaceClass = null;

		if ( proxyAnnotation != null ) {
			AnnotationValue lazyValue = proxyAnnotation.value( "lazy" );
			if ( lazyValue != null ) {
				lazy = lazyValue.asBoolean();
			}

			AnnotationValue proxyClassValue = proxyAnnotation.value( "proxyClass" );
			if ( proxyClassValue != null ) {
				proxyInterfaceClass = proxyClassValue.asString();
			}
		}

		entityBindingState.setLazy( lazy );
		entityBindingState.setProxyInterfaceName( proxyInterfaceClass );
	}

	private void bindSynchronize(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance synchronizeAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);

		if ( synchronizeAnnotation != null ) {
			String[] tableNames = synchronizeAnnotation.value().asStringArray();
			for ( String tableName : tableNames ) {
				entityBindingState.addSynchronizedTableName( tableName );
			}
		}
	}

	private void bindCustomSQL(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance sqlInsertAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SQL_INSERT
		);
		entityBindingState.setCustomInsert( createCustomSQL( sqlInsertAnnotation ) );

		AnnotationInstance sqlUpdateAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SQL_UPDATE
		);
		entityBindingState.setCustomUpdate( createCustomSQL( sqlUpdateAnnotation ) );

		AnnotationInstance sqlDeleteAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SQL_DELETE
		);
		entityBindingState.setCustomDelete( createCustomSQL( sqlDeleteAnnotation ) );

		AnnotationInstance sqlDeleteAllAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SQL_DELETE_ALL
		);
		if ( sqlDeleteAllAnnotation != null ) {
			entityBindingState.setCustomDelete( createCustomSQL( sqlDeleteAllAnnotation ) );
		}
	}

	private CustomSQL createCustomSQL(AnnotationInstance customSQLAnnotation) {
		if ( customSQLAnnotation == null ) {
			return null;
		}

		String sql = customSQLAnnotation.value( "sql" ).asString();
		boolean isCallable = false;
		AnnotationValue callableValue = customSQLAnnotation.value( "callable" );
		if ( callableValue != null ) {
			isCallable = callableValue.asBoolean();
		}

		ResultCheckStyle checkStyle = ResultCheckStyle.NONE;
		AnnotationValue checkStyleValue = customSQLAnnotation.value( "check" );
		if ( checkStyleValue != null ) {
			checkStyle = Enum.valueOf( ResultCheckStyle.class, checkStyleValue.asEnum() );
		}

		return new CustomSQL(
				sql,
				isCallable,
				Enum.valueOf( ExecuteUpdateResultCheckStyle.class, checkStyle.toString() )
		);
	}

	private void bindRowId(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance rowIdAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.ROW_ID
		);

		if ( rowIdAnnotation != null ) {
			entityBindingState.setRowId( rowIdAnnotation.value().asString() );
		}
	}

	private void bindBatchSize(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance batchSizeAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.BATCH_SIZE
		);

		if ( batchSizeAnnotation != null ) {
			entityBindingState.setBatchSize( batchSizeAnnotation.value( "size" ).asInt() );
		}
	}

	private Caching createCachingForCacheableAnnotation(EntityBindingStateImpl entityBindingState) {
		String region = entityBindingState.getEntityName();
		RegionFactory regionFactory = meta.getServiceRegistry().getService( RegionFactory.class );
		AccessType defaultAccessType = regionFactory.getDefaultAccessType();
		return new Caching( region, defaultAccessType, true );
	}

	private Table createTable() {
		String schemaName = null;
		String catalogName = null;
		String tableName = null;

		// is there an explicit @Table annotation?
		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), JPADotNames.TABLE
		);
		if ( tableAnnotation != null ) {
			schemaName = JandexHelper.getValueAsString( tableAnnotation, "schema" );
			catalogName = JandexHelper.getValueAsString( tableAnnotation, "catalog" );
			String explicitTableName = JandexHelper.getValueAsString( tableAnnotation, "name" );
			if ( StringHelper.isNotEmpty( explicitTableName ) ) {
				tableName = meta.getNamingStrategy().tableName( explicitTableName );
			}
		}

		// no explicit table name given, let's use the entity name as table name (taking inheritance into consideration
		if ( StringHelper.isEmpty( tableName ) ) {
			tableName = meta.getNamingStrategy().classToTableName( entityClass.getClassNameForTable() );
		}

		// check whether the names should be globally quoted
		if ( meta.isGloballyQuotedIdentifiers() ) {
			schemaName = StringHelper.quote( schemaName );
			catalogName = StringHelper.quote( catalogName );
			tableName = StringHelper.quote( tableName );
		}

		// last, but not least create the metamodel relational objects
		final Identifier tableNameIdentifier = Identifier.toIdentifier( tableName );
		final Schema schema = meta.getDatabase().getSchema( new Schema.Name( schemaName, catalogName ) );
		Table table = schema.getTable( tableNameIdentifier );
		if ( table == null ) {
			table = schema.createTable( tableNameIdentifier );
		}
		return table;
	}


	private void bindTable(EntityBinding entityBinding) {
		Table table = createTable();
		entityBinding.setBaseTable( table );

		AnnotationInstance checkAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.CHECK
		);
		if ( checkAnnotation != null ) {
			table.addCheckConstraint( checkAnnotation.value( "constraints" ).asString() );
		}
	}

	private void bindId(EntityBinding entityBinding) {
		switch ( entityClass.getIdType() ) {
			case SIMPLE: {
				bindSingleIdAnnotation( entityBinding );
				break;
			}
			case COMPOSED: {
				// todo
				break;
			}
			case EMBEDDED: {
				bindEmbeddedIdAnnotation( entityBinding );
				break;
			}
			default: {
			}
		}
	}

	private void bindJpaEntityAnnotation(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), JPADotNames.ENTITY
		);
		String name;
		if ( jpaEntityAnnotation.value( "name" ) == null ) {
			name = entityClass.getName();
		}
		else {
			name = jpaEntityAnnotation.value( "name" ).asString();
		}
		entityBindingState.setJpaEntityName( name );
	}

	private void bindEmbeddedIdAnnotation(EntityBinding entityBinding) {
		AnnotationInstance idAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), JPADotNames.EMBEDDED_ID
		);

		String idName = JandexHelper.getPropertyName( idAnnotation.target() );
		MappedAttribute idAttribute = entityClass.getMappedAttribute( idName );
		if ( !( idAttribute instanceof SimpleAttribute ) ) {
			throw new AssertionFailure( "Unexpected attribute type for id attribute" );
		}

		SingularAttribute attribute = entityBinding.getEntity().getOrCreateComponentAttribute( idName );


		SimpleAttributeBinding attributeBinding = entityBinding.makeSimpleIdAttributeBinding( attribute );

		attributeBinding.initialize( new AttributeBindingStateImpl( (SimpleAttribute) idAttribute ) );

		TupleRelationalStateImpl state = new TupleRelationalStateImpl();
		EmbeddableClass embeddableClass = entityClass.getEmbeddedClasses().get( idName );
		for ( SimpleAttribute attr : embeddableClass.getSimpleAttributes() ) {
			state.addValueState( new ColumnRelationalStateImpl( attr, meta ) );
		}
		attributeBinding.initialize( state );
		Map<String, String> parms = new HashMap<String, String>( 1 );
		parms.put( IdentifierGenerator.ENTITY_NAME, entityBinding.getEntity().getName() );
		IdGenerator generator = new IdGenerator( "NAME", "assigned", parms );
		entityBinding.getEntityIdentifier().setIdGenerator( generator );
		// entityBinding.getEntityIdentifier().createIdentifierGenerator( meta.getIdentifierGeneratorFactory() );
	}

	private void bindSingleIdAnnotation(EntityBinding entityBinding) {
		// we know we are dealing w/ a single @Id, but potentially it is defined in a mapped super class
		ConfiguredClass configuredClass = entityClass;
		EntityClass superEntity = entityClass.getEntityParent();
		Hierarchical container = entityBinding.getEntity();
		Iterator<SimpleAttribute> iter = null;
		while ( configuredClass != null && configuredClass != superEntity ) {
			iter = configuredClass.getIdAttributes().iterator();
			if ( iter.hasNext() ) {
				break;
			}
			configuredClass = configuredClass.getParent();
			container = container.getSuperType();
		}

		// if we could not find the attribute our assumptions were wrong
		if ( iter == null || !iter.hasNext() ) {
			throw new AnnotationException(
					String.format(
							"Unable to find id attribute for class %s",
							entityClass.getName()
					)
			);
		}

		// now that we have the id attribute we can create the attribute and binding
		MappedAttribute idAttribute = iter.next();
		Attribute attribute = container.getOrCreateSingularAttribute( idAttribute.getName() );

		SimpleAttributeBinding attributeBinding = entityBinding.makeSimpleIdAttributeBinding( attribute );
		attributeBinding.initialize( new AttributeBindingStateImpl( (SimpleAttribute) idAttribute ) );
		attributeBinding.initialize( new ColumnRelationalStateImpl( (SimpleAttribute) idAttribute, meta ) );
		bindSingleIdGeneratedValue( entityBinding, idAttribute.getName() );
	}

	private void bindSingleIdGeneratedValue(EntityBinding entityBinding, String idPropertyName) {
		AnnotationInstance generatedValueAnn = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), JPADotNames.GENERATED_VALUE
		);
		if ( generatedValueAnn == null ) {
			return;
		}

		String idName = JandexHelper.getPropertyName( generatedValueAnn.target() );
		if ( !idPropertyName.equals( idName ) ) {
			throw new AssertionFailure(
					String.format(
							"Attribute[%s.%s] with @GeneratedValue doesn't have a @Id.",
							entityClass.getName(),
							idPropertyName
					)
			);
		}
		String generator = JandexHelper.getValueAsString( generatedValueAnn, "generator" );
		IdGenerator idGenerator = null;
		if ( StringHelper.isNotEmpty( generator ) ) {
			idGenerator = meta.getIdGenerator( generator );
			if ( idGenerator == null ) {
				throw new MappingException(
						String.format(
								"@GeneratedValue on %s.%s referring an undefined generator [%s]",
								entityClass.getName(),
								idName,
								generator
						)
				);
			}
			entityBinding.getEntityIdentifier().setIdGenerator( idGenerator );
		}
		GenerationType generationType = JandexHelper.getValueAsEnum(
				generatedValueAnn,
				"strategy",
				GenerationType.class
		);
		String strategy = IdGeneratorBinder.generatorType(
				generationType,
				meta.getOptions().useNewIdentifierGenerators()
		);
		if ( idGenerator != null && !strategy.equals( idGenerator.getStrategy() ) ) {
			//todo how to ?
			throw new MappingException(
					String.format(
							"Inconsistent Id Generation strategy of @GeneratedValue on %s.%s",
							entityClass.getName(),
							idName
					)
			);
		}
		if ( idGenerator == null ) {
			idGenerator = new IdGenerator( "NAME", strategy, new HashMap<String, String>() );
			entityBinding.getEntityIdentifier().setIdGenerator( idGenerator );
		}
//        entityBinding.getEntityIdentifier().createIdentifierGenerator( meta.getIdentifierGeneratorFactory() );
	}

	private void bindAttributes(EntityBinding entityBinding) {
		// bind the attributes of this entity
		AttributeContainer entity = entityBinding.getEntity();
		bindAttributes( entityBinding, entity, entityClass );

		// bind potential mapped super class attributes
		ConfiguredClass parent = entityClass.getParent();
		Hierarchical superTypeContainer = entityBinding.getEntity().getSuperType();
		while ( containsPotentialMappedSuperclassAttributes( parent ) ) {
			bindAttributes( entityBinding, superTypeContainer, parent );
			parent = parent.getParent();
			superTypeContainer = superTypeContainer.getSuperType();
		}
	}

	private boolean containsPotentialMappedSuperclassAttributes(ConfiguredClass parent) {
		return parent != null && ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( parent.getConfiguredClassType() ) ||
				ConfiguredClassType.NON_ENTITY.equals( parent.getConfiguredClassType() ) );
	}

	private void bindAttributes(EntityBinding entityBinding, AttributeContainer attributeContainer, ConfiguredClass configuredClass) {
		for ( SimpleAttribute simpleAttribute : configuredClass.getSimpleAttributes() ) {
			bindSingleMappedAttribute(
					entityBinding,
					attributeContainer,
					simpleAttribute
			);
		}
		for ( AssociationAttribute associationAttribute : configuredClass.getAssociationAttributes() ) {
			bindAssociationAttribute(
					entityBinding,
					attributeContainer,
					associationAttribute
			);
		}
	}

	private void bindEmbeddedAttributes(EntityBinding entityBinding) {
		AttributeContainer entity = entityBinding.getEntity();
		bindEmbeddedAttributes( entityBinding, entity, entityClass );

		// bind potential mapped super class embeddables
		ConfiguredClass parent = entityClass.getParent();
		Hierarchical superTypeContainer = entityBinding.getEntity().getSuperType();
		while ( containsPotentialMappedSuperclassAttributes( parent ) ) {
			bindEmbeddedAttributes( entityBinding, superTypeContainer, parent );
			parent = parent.getParent();
			superTypeContainer = superTypeContainer.getSuperType();
		}
	}

	private void bindEmbeddedAttributes(EntityBinding entityBinding, AttributeContainer attributeContainer, ConfiguredClass configuredClass) {
		for ( Map.Entry<String, EmbeddableClass> entry : configuredClass.getEmbeddedClasses().entrySet() ) {
			String attributeName = entry.getKey();
			EmbeddableClass embeddedClass = entry.getValue();
			SingularAttribute component = attributeContainer.getOrCreateComponentAttribute( attributeName );
			for ( SimpleAttribute simpleAttribute : embeddedClass.getSimpleAttributes() ) {
				bindSingleMappedAttribute(
						entityBinding,
						component.getAttributeContainer(),
						simpleAttribute
				);
			}
			for ( AssociationAttribute associationAttribute : embeddedClass.getAssociationAttributes() ) {
				bindAssociationAttribute(
						entityBinding,
						component.getAttributeContainer(),
						associationAttribute
				);
			}
		}
	}

	private void bindAssociationAttribute(EntityBinding entityBinding, AttributeContainer container, AssociationAttribute associationAttribute) {
		switch ( associationAttribute.getAssociationType() ) {
			case MANY_TO_ONE: {
				container.getOrCreateSingularAttribute( associationAttribute.getName() );
				ManyToOneAttributeBinding manyToOneAttributeBinding = entityBinding.makeManyToOneAttributeBinding(
						associationAttribute.getName()
				);

				ManyToOneAttributeBindingState bindingState = new ManyToOneBindingStateImpl( associationAttribute );
				manyToOneAttributeBinding.initialize( bindingState );

				ManyToOneRelationalStateImpl relationalState = new ManyToOneRelationalStateImpl();
				if ( entityClass.hasOwnTable() ) {
					ColumnRelationalStateImpl columnRelationsState = new ColumnRelationalStateImpl(
							associationAttribute, meta
					);
					relationalState.addValueState( columnRelationsState );
				}
				manyToOneAttributeBinding.initialize( relationalState );
				break;
			}
			default: {
				// todo
			}
		}
	}

	private void bindSingleMappedAttribute(EntityBinding entityBinding, AttributeContainer container, SimpleAttribute simpleAttribute) {
		if ( simpleAttribute.isId() ) {
			return;
		}

		Attribute attribute = container.getOrCreateSingularAttribute( simpleAttribute.getName() );
		SimpleAttributeBinding attributeBinding;

		if ( simpleAttribute.isDiscriminator() ) {
			EntityDiscriminator entityDiscriminator = entityBinding.makeEntityDiscriminator( attribute );
			DiscriminatorBindingState bindingState = new DiscriminatorBindingStateImpl( simpleAttribute );
			entityDiscriminator.initialize( bindingState );
			attributeBinding = entityDiscriminator.getValueBinding();
		}
		else if ( simpleAttribute.isVersioned() ) {
			attributeBinding = entityBinding.makeVersionBinding( attribute );
			SimpleAttributeBindingState bindingState = new AttributeBindingStateImpl( simpleAttribute );
			attributeBinding.initialize( bindingState );
		}
		else {
			attributeBinding = entityBinding.makeSimpleAttributeBinding( attribute );
			SimpleAttributeBindingState bindingState = new AttributeBindingStateImpl( simpleAttribute );
			attributeBinding.initialize( bindingState );
		}

		if ( entityClass.hasOwnTable() ) {
			ColumnRelationalStateImpl columnRelationsState = new ColumnRelationalStateImpl(
					simpleAttribute, meta
			);
			TupleRelationalStateImpl relationalState = new TupleRelationalStateImpl();
			relationalState.addValueState( columnRelationsState );

			attributeBinding.initialize( relationalState );
		}
	}

	private void bindHibernateEntityAnnotation(EntityBindingStateImpl entityBindingState) {
		// initialize w/ the defaults
		boolean mutable = true;
		boolean dynamicInsert = false;
		boolean dynamicUpdate = false;
		boolean selectBeforeUpdate = false;
		PolymorphismType polymorphism = PolymorphismType.IMPLICIT;
		OptimisticLockType optimisticLock = OptimisticLockType.VERSION;

		AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.ENTITY
		);

		if ( hibernateEntityAnnotation != null ) {
			if ( hibernateEntityAnnotation.value( "mutable" ) != null ) {
				mutable = hibernateEntityAnnotation.value( "mutable" ).asBoolean();
			}

			if ( hibernateEntityAnnotation.value( "dynamicInsert" ) != null ) {
				dynamicInsert = hibernateEntityAnnotation.value( "dynamicInsert" ).asBoolean();
			}

			if ( hibernateEntityAnnotation.value( "dynamicUpdate" ) != null ) {
				dynamicUpdate = hibernateEntityAnnotation.value( "dynamicUpdate" ).asBoolean();
			}

			if ( hibernateEntityAnnotation.value( "selectBeforeUpdate" ) != null ) {
				selectBeforeUpdate = hibernateEntityAnnotation.value( "selectBeforeUpdate" ).asBoolean();
			}

			if ( hibernateEntityAnnotation.value( "polymorphism" ) != null ) {
				polymorphism = PolymorphismType.valueOf( hibernateEntityAnnotation.value( "polymorphism" ).asEnum() );
			}

			if ( hibernateEntityAnnotation.value( "optimisticLock" ) != null ) {
				optimisticLock = OptimisticLockType.valueOf(
						hibernateEntityAnnotation.value( "optimisticLock" ).asEnum()
				);
			}

			if ( hibernateEntityAnnotation.value( "persister" ) != null ) {
				final String persisterClassName = ( hibernateEntityAnnotation.value( "persister" ).toString() );
				entityBindingState.setPersisterClass( meta.<EntityPersister>locateClassByName( persisterClassName ) );
			}
		}

		// also check for the immutable annotation
		AnnotationInstance immutableAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.IMMUTABLE
		);
		if ( immutableAnnotation != null ) {
			mutable = false;
		}

		entityBindingState.setMutable( mutable );
		entityBindingState.setDynamicInsert( dynamicInsert );
		entityBindingState.setDynamicUpdate( dynamicUpdate );
		entityBindingState.setSelectBeforeUpdate( selectBeforeUpdate );
		entityBindingState.setExplicitPolymorphism( PolymorphismType.EXPLICIT.equals( polymorphism ) );
		entityBindingState.setOptimisticLock( optimisticLock );
	}
}

