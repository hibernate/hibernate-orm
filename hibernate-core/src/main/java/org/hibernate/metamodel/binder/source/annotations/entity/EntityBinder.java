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
package org.hibernate.metamodel.binder.source.annotations.entity;

import javax.persistence.GenerationType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.binder.source.annotations.AnnotationsBindingContext;
import org.hibernate.metamodel.binder.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.binder.source.annotations.JPADotNames;
import org.hibernate.metamodel.binder.source.annotations.JandexHelper;
import org.hibernate.metamodel.binder.source.annotations.UnknownInheritanceTypeException;
import org.hibernate.metamodel.binder.source.annotations.global.IdGeneratorBinder;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.binding.state.DiscriminatorBindingState;
import org.hibernate.metamodel.binding.state.ManyToOneAttributeBindingState;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.AttributeContainer;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.UniqueKey;
import org.hibernate.metamodel.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.annotations.attribute.DiscriminatorColumnValues;
import org.hibernate.metamodel.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.source.annotations.attribute.SimpleAttribute;
import org.hibernate.metamodel.source.annotations.attribute.state.binding.AttributeBindingStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.binding.DiscriminatorBindingStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.binding.ManyToOneBindingStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.relational.ColumnRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.relational.ManyToOneRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.attribute.state.relational.TupleRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * Creates the domain and relational metamodel for a configured class and <i>binds</i> them together.
 *
 * @author Hardy Ferentschik
 */
public class EntityBinder {
	private final EntityClass entityClass;
	private final Hierarchical superType;
	private final AnnotationsBindingContext bindingContext;

	private final Schema.Name schemaName;

	public EntityBinder(EntityClass entityClass, Hierarchical superType, AnnotationsBindingContext bindingContext) {
		this.entityClass = entityClass;
		this.superType = superType;
		this.bindingContext = bindingContext;
		this.schemaName = determineSchemaName();
	}

	private Schema.Name determineSchemaName() {
		String schema = bindingContext.getMappingDefaults().getSchemaName();
		String catalog = bindingContext.getMappingDefaults().getCatalogName();

		final AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), JPADotNames.TABLE
		);
		if ( tableAnnotation != null ) {
			final AnnotationValue schemaValue = tableAnnotation.value( "schema" );
			if ( schemaValue != null ) {
				schema = schemaValue.asString();
			}

			final AnnotationValue catalogValue = tableAnnotation.value( "catalog" );
			if ( catalogValue != null ) {
				catalog = catalogValue.asString();
			}
		}

		return new Schema.Name( schema, catalog );
	}

	public EntityBinding bind(List<String> processedEntityNames) {
		if ( processedEntityNames.contains( entityClass.getName() ) ) {
			return bindingContext.getMetadataImplementor().getEntityBinding( entityClass.getName() );
		}

		final EntityBinding entityBinding = doEntityBindingCreation();

		bindingContext.getMetadataImplementor().addEntity( entityBinding );
		processedEntityNames.add( entityBinding.getEntity().getName() );

		return entityBinding;
	}

	private EntityBinding doEntityBindingCreation() {
		final EntityBinding entityBinding = buildBasicEntityBinding();

		// bind all attributes - simple as well as associations
		bindAttributes( entityBinding );
		bindEmbeddedAttributes( entityBinding );

		bindTableUniqueConstraints( entityBinding );

		bindingContext.getMetadataImplementor().addEntity( entityBinding );
		return entityBinding;
	}

	private EntityBinding buildBasicEntityBinding() {
		switch ( entityClass.getInheritanceType() ) {
			case NO_INHERITANCE: {
				return doRootEntityBindingCreation();
			}
			case SINGLE_TABLE: {
				return doDiscriminatedSubclassBindingCreation();
			}
			case JOINED: {
				return doJoinedSubclassBindingCreation();
			}
			case TABLE_PER_CLASS: {
				return doUnionSubclassBindingCreation();
			}
			default: {
				throw new UnknownInheritanceTypeException( "Unknown InheritanceType : " + entityClass.getInheritanceType() );
			}
		}
	}

	private EntityBinding doRootEntityBindingCreation() {
		EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.NO_INHERITANCE );
		entityBinding.setRoot( true );

		doBasicEntityBinding( entityBinding );

		// technically the rest of these binds should only apply to root entities, but they are really available on all
		// because we do not currently subtype EntityBinding

		final AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.ENTITY
		);

		// see HHH-6400
		PolymorphismType polymorphism = PolymorphismType.IMPLICIT;
		if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "polymorphism" ) != null ) {
			polymorphism = PolymorphismType.valueOf( hibernateEntityAnnotation.value( "polymorphism" ).asEnum() );
		}
		entityBinding.setExplicitPolymorphism( polymorphism == PolymorphismType.EXPLICIT );

		// see HHH-6401
		OptimisticLockType optimisticLockType = OptimisticLockType.VERSION;
		if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "optimisticLock" ) != null ) {
			optimisticLockType = OptimisticLockType.valueOf( hibernateEntityAnnotation.value( "optimisticLock" ).asEnum() );
		}
		entityBinding.setOptimisticLockStyle( OptimisticLockStyle.valueOf( optimisticLockType.name() ) );

		final AnnotationInstance hibernateImmutableAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.IMMUTABLE
		);
		final boolean mutable = hibernateImmutableAnnotation == null
				&& hibernateEntityAnnotation != null
				&& hibernateEntityAnnotation.value( "mutable" ) != null
				&& hibernateEntityAnnotation.value( "mutable" ).asBoolean();
		entityBinding.setMutable( mutable );

		final AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.WHERE
		);
		entityBinding.setWhereFilter(
				whereAnnotation != null && whereAnnotation.value( "clause" ) != null
						? whereAnnotation.value( "clause" ).asString()
						: null
		);

		final AnnotationInstance rowIdAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.ROW_ID
		);
		entityBinding.setRowId(
				rowIdAnnotation != null && rowIdAnnotation.value() != null
						? rowIdAnnotation.value().asString()
						: null
		);

		entityBinding.setCaching( interpretCaching( entityClass, bindingContext ) );

		bindPrimaryTable( entityBinding );
		bindId( entityBinding );

		if ( entityBinding.getInheritanceType() == InheritanceType.SINGLE_TABLE ) {
			bindDiscriminatorColumn( entityBinding );
		}

		// todo : version

		return entityBinding;
	}

	private Caching interpretCaching(ConfiguredClass configuredClass, AnnotationsBindingContext bindingContext) {
		final AnnotationInstance hibernateCacheAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.CACHE
		);
		if ( hibernateCacheAnnotation != null ) {
			final AccessType accessType = hibernateCacheAnnotation.value( "usage" ) == null
					? bindingContext.getMappingDefaults().getCacheAccessType()
					: CacheConcurrencyStrategy.parse( hibernateCacheAnnotation.value( "usage" ).asEnum() ).toAccessType();
			return new Caching(
					hibernateCacheAnnotation.value( "region" ) == null
							? configuredClass.getName()
							: hibernateCacheAnnotation.value( "region" ).asString(),
					accessType,
					hibernateCacheAnnotation.value( "include" ) != null
							&& "all".equals( hibernateCacheAnnotation.value( "include" ).asString() )
			);
		}

		final AnnotationInstance jpaCacheableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.CACHEABLE
		);

		boolean cacheable = true; // true is the default
		if ( jpaCacheableAnnotation != null && jpaCacheableAnnotation.value() != null ) {
			cacheable = jpaCacheableAnnotation.value().asBoolean();
		}

		final boolean doCaching;
		switch ( bindingContext.getMetadataImplementor().getOptions().getSharedCacheMode() ) {
			case ALL: {
				doCaching = true;
				break;
			}
			case ENABLE_SELECTIVE: {
				doCaching = cacheable;
				break;
			}
			case DISABLE_SELECTIVE: {
				doCaching = jpaCacheableAnnotation == null || cacheable;
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				doCaching = false;
				break;
			}
		}

		if ( ! doCaching ) {
			return null;
		}

		return new Caching(
				configuredClass.getName(),
				bindingContext.getMappingDefaults().getCacheAccessType(),
				true
		);
	}

	private EntityBinding doDiscriminatedSubclassBindingCreation() {
		EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.SINGLE_TABLE );

		doBasicEntityBinding( entityBinding );

		// todo : bind discriminator-based subclassing specifics...

		return entityBinding;
	}

	private EntityBinding doJoinedSubclassBindingCreation() {
		EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.JOINED );

		doBasicEntityBinding( entityBinding );

		// todo : bind join-based subclassing specifics...

		return entityBinding;
	}

	private EntityBinding doUnionSubclassBindingCreation() {
		EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.TABLE_PER_CLASS );

		doBasicEntityBinding( entityBinding );

		// todo : bind union-based subclassing specifics...

		return entityBinding;
	}

	private void doBasicEntityBinding(EntityBinding entityBinding) {
		entityBinding.setEntityMode( EntityMode.POJO );

		final Entity entity = new Entity(
				entityClass.getName(),
				entityClass.getName(),
				bindingContext.makeClassReference( entityClass.getName() ),
				superType
		);
		entityBinding.setEntity( entity );

		final AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), JPADotNames.ENTITY
		);

		final AnnotationValue explicitJpaEntityName = jpaEntityAnnotation.value( "name" );
		if ( explicitJpaEntityName == null ) {
			entityBinding.setJpaEntityName( entityClass.getName() );
		}
		else {
			entityBinding.setJpaEntityName( explicitJpaEntityName.asString() );
		}

		final AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.ENTITY
		);

		// see HHH-6397
		entityBinding.setDynamicInsert(
				hibernateEntityAnnotation != null
						&& hibernateEntityAnnotation.value( "dynamicInsert" ) != null
						&& hibernateEntityAnnotation.value( "dynamicInsert" ).asBoolean()
		);

		// see HHH-6398
		entityBinding.setDynamicUpdate(
				hibernateEntityAnnotation != null
						&& hibernateEntityAnnotation.value( "dynamicUpdate" ) != null
						&& hibernateEntityAnnotation.value( "dynamicUpdate" ).asBoolean()
		);

		// see HHH-6399
		entityBinding.setSelectBeforeUpdate(
				hibernateEntityAnnotation != null
						&& hibernateEntityAnnotation.value( "selectBeforeUpdate" ) != null
						&& hibernateEntityAnnotation.value( "selectBeforeUpdate" ).asBoolean()
		);

		// Custom sql loader
		final AnnotationInstance sqlLoaderAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.LOADER
		);
		if ( sqlLoaderAnnotation != null ) {
			entityBinding.setCustomLoaderName( sqlLoaderAnnotation.value( "namedQuery" ).asString() );
		}

		// Custom sql insert
		final AnnotationInstance sqlInsertAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SQL_INSERT
		);
		entityBinding.setCustomInsert( createCustomSQL( sqlInsertAnnotation ) );

		// Custom sql update
		final AnnotationInstance sqlUpdateAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SQL_UPDATE
		);
		entityBinding.setCustomInsert( createCustomSQL( sqlUpdateAnnotation ) );

		// Custom sql delete
		final AnnotationInstance sqlDeleteAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SQL_DELETE
		);
		entityBinding.setCustomInsert( createCustomSQL( sqlDeleteAnnotation ) );

		// Batch size
		final AnnotationInstance batchSizeAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.BATCH_SIZE
		);
		entityBinding.setBatchSize( batchSizeAnnotation == null ? -1 : batchSizeAnnotation.value( "size" ).asInt());

		// Proxy generation
		final boolean lazy;
		final Value<Class<?>> proxyInterfaceType;
		final AnnotationInstance hibernateProxyAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.PROXY
		);
		if ( hibernateProxyAnnotation != null ) {
			lazy = hibernateProxyAnnotation.value( "lazy" ) == null
					|| hibernateProxyAnnotation.value( "lazy" ).asBoolean();
			final AnnotationValue proxyClassValue = hibernateProxyAnnotation.value( "proxyClass" );
			if ( proxyClassValue == null ) {
				proxyInterfaceType = entity.getClassReferenceUnresolved();
			}
			else {
				proxyInterfaceType = bindingContext.makeClassReference( proxyClassValue.asString() );
			}
		}
		else {
			lazy = true;
			proxyInterfaceType = entity.getClassReferenceUnresolved();
		}
		entityBinding.setLazy( lazy );
		entityBinding.setProxyInterfaceType( proxyInterfaceType );

		// Custom persister
		final Class<? extends EntityPersister> entityPersisterClass;
		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.PERSISTER
		);
		if ( persisterAnnotation == null || persisterAnnotation.value( "impl" ) == null ) {
			if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "persister" ) != null ) {
				entityPersisterClass = bindingContext.locateClassByName( hibernateEntityAnnotation.value( "persister" ).asString() );
			}
			else {
				entityPersisterClass = null;
			}
		}
		else {
			if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "persister" ) != null ) {
				// todo : error?
			}
			entityPersisterClass = bindingContext.locateClassByName( persisterAnnotation.value( "impl" ).asString() );
		}
		entityBinding.setCustomEntityPersisterClass( entityPersisterClass );

		// Custom tuplizer
		final AnnotationInstance pojoTuplizerAnnotation = locatePojoTuplizerAnnotation();
		if ( pojoTuplizerAnnotation != null ) {
			final Class<? extends EntityTuplizer> tuplizerClass =
					bindingContext.locateClassByName( pojoTuplizerAnnotation.value( "impl" ).asString() );
			entityBinding.setCustomEntityTuplizerClass( tuplizerClass );
		}

		// table synchronizations
		final AnnotationInstance synchronizeAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);
		if ( synchronizeAnnotation != null ) {
			final String[] tableNames = synchronizeAnnotation.value().asStringArray();
			entityBinding.addSynchronizedTableNames( Arrays.asList( tableNames ) );
		}
	}

	private CustomSQL createCustomSQL(AnnotationInstance customSqlAnnotation) {
		if ( customSqlAnnotation == null ) {
			return null;
		}

		final String sql = customSqlAnnotation.value( "sql" ).asString();
		final boolean isCallable = customSqlAnnotation.value( "callable" ) != null
				&& customSqlAnnotation.value( "callable" ).asBoolean();

		final ExecuteUpdateResultCheckStyle checkStyle = customSqlAnnotation.value( "check" ) == null
				? isCallable
						? ExecuteUpdateResultCheckStyle.NONE
						: ExecuteUpdateResultCheckStyle.COUNT
				: ExecuteUpdateResultCheckStyle.valueOf( customSqlAnnotation.value( "check" ).asEnum() );

		return new CustomSQL( sql, isCallable, checkStyle );
	}

	private AnnotationInstance locatePojoTuplizerAnnotation() {
		final AnnotationInstance tuplizersAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);
		if ( tuplizersAnnotation == null ) {
			return null;
		}

		for ( AnnotationInstance tuplizerAnnotation : JandexHelper.getValue( tuplizersAnnotation, "value", AnnotationInstance[].class ) ) {
			if ( EntityMode.valueOf( tuplizerAnnotation.value( "entityModeType" ).asEnum() ) == EntityMode.POJO ) {
				return tuplizerAnnotation;
			}
		}

		return null;
	}

	private void bindDiscriminatorColumn(EntityBinding entityBinding) {
		final Map<DotName, List<AnnotationInstance>> typeAnnotations = JandexHelper.getTypeAnnotations(
				entityClass.getClassInfo()
		);
		SimpleAttribute discriminatorAttribute = SimpleAttribute.createDiscriminatorAttribute( typeAnnotations );
		bindSingleMappedAttribute( entityBinding, entityBinding.getEntity(), discriminatorAttribute );

		if ( !( discriminatorAttribute.getColumnValues() instanceof DiscriminatorColumnValues) ) {
			throw new AssertionFailure( "Expected discriminator column values" );
		}
	}

	private void bindPrimaryTable(EntityBinding entityBinding) {
		final Schema schema = bindingContext.getMetadataImplementor().getDatabase().getSchema( schemaName );

		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(),
				JPADotNames.TABLE
		);

		String tableName = null;
		if ( tableAnnotation != null ) {
			String explicitTableName = JandexHelper.getValue( tableAnnotation, "name", String.class );
			if ( StringHelper.isNotEmpty( explicitTableName ) ) {
				tableName = bindingContext.getNamingStrategy().tableName( explicitTableName );
			}
		}

		// no explicit table name given, let's use the entity name as table name (taking inheritance into consideration
		if ( StringHelper.isEmpty( tableName ) ) {
			tableName = bindingContext.getNamingStrategy().classToTableName( entityClass.getClassNameForTable() );
		}

		org.hibernate.metamodel.relational.Table table = schema.locateOrCreateTable( Identifier.toIdentifier( tableName ) );
		entityBinding.setBaseTable( table );

		AnnotationInstance checkAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), HibernateDotNames.CHECK
		);
		if ( checkAnnotation != null ) {
			table.addCheckConstraint( checkAnnotation.value( "constraints" ).asString() );
		}
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
				uniqueKey.addColumn( table.locateOrCreateColumn( columnName ) );
			}
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

	private void bindEmbeddedIdAnnotation(EntityBinding entityBinding) {
		AnnotationInstance idAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(), JPADotNames.EMBEDDED_ID
		);

		String idName = JandexHelper.getPropertyName( idAnnotation.target() );
		MappedAttribute idAttribute = entityClass.getMappedAttribute( idName );
		if ( !( idAttribute instanceof SimpleAttribute ) ) {
			throw new AssertionFailure( "Unexpected attribute type for id attribute" );
		}

		SingularAttribute attribute = entityBinding.getEntity().locateOrCreateComponentAttribute( idName );

		SimpleAttributeBinding attributeBinding = entityBinding.makeSimpleIdAttributeBinding( attribute );

		attributeBinding.initialize( new AttributeBindingStateImpl( (SimpleAttribute) idAttribute ) );
		attributeBinding.initialize( new ColumnRelationalStateImpl( (SimpleAttribute) idAttribute, bindingContext.getMetadataImplementor() ) );
		bindSingleIdGeneratedValue( entityBinding, idName );

		TupleRelationalStateImpl state = new TupleRelationalStateImpl();
		EmbeddableClass embeddableClass = entityClass.getEmbeddedClasses().get( idName );
		for ( SimpleAttribute attr : embeddableClass.getSimpleAttributes() ) {
			state.addValueState( new ColumnRelationalStateImpl( attr, bindingContext.getMetadataImplementor() ) );
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
		Attribute attribute = container.locateOrCreateSingularAttribute( idAttribute.getName() );

		SimpleAttributeBinding attributeBinding = entityBinding.makeSimpleIdAttributeBinding( attribute );
		attributeBinding.initialize( new AttributeBindingStateImpl( (SimpleAttribute) idAttribute ) );
		attributeBinding.initialize( new ColumnRelationalStateImpl( (SimpleAttribute) idAttribute, bindingContext.getMetadataImplementor() ) );
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
		String generator = JandexHelper.getValue( generatedValueAnn, "generator", String.class );
		IdGenerator idGenerator = null;
		if ( StringHelper.isNotEmpty( generator ) ) {
			idGenerator = bindingContext.getMetadataImplementor().getIdGenerator( generator );
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
				bindingContext.getMetadataImplementor().getOptions().useNewIdentifierGenerators()
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
		// collect attribute overrides as we map the attributes
		Map<String, AttributeOverride> attributeOverrideMap = new HashMap<String, AttributeOverride>();

		// bind the attributes of this entity
		AttributeContainer entity = entityBinding.getEntity();
		bindAttributes( entityBinding, entity, entityClass, attributeOverrideMap );

		// bind potential mapped super class attributes
		attributeOverrideMap.putAll( entityClass.getAttributeOverrideMap() );
		ConfiguredClass parent = entityClass.getParent();
		Hierarchical superTypeContainer = entityBinding.getEntity().getSuperType();
		while ( containsMappedSuperclassAttributes( parent ) ) {
			bindAttributes( entityBinding, superTypeContainer, parent, attributeOverrideMap );
			addNewOverridesToMap( parent, attributeOverrideMap );
			parent = parent.getParent();
			superTypeContainer = superTypeContainer.getSuperType();
		}
	}

	private void addNewOverridesToMap(ConfiguredClass parent, Map<String, AttributeOverride> attributeOverrideMap) {
		Map<String, AttributeOverride> overrides = parent.getAttributeOverrideMap();
		for ( Map.Entry<String, AttributeOverride> entry : overrides.entrySet() ) {
			if ( !attributeOverrideMap.containsKey( entry.getKey() ) ) {
				attributeOverrideMap.put( entry.getKey(), entry.getValue() );
			}
		}
	}

	private boolean containsMappedSuperclassAttributes(ConfiguredClass parent) {
		return parent != null && ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( parent.getConfiguredClassType() ) ||
				ConfiguredClassType.NON_ENTITY.equals( parent.getConfiguredClassType() ) );
	}

	private void bindAttributes(
				EntityBinding entityBinding,
				AttributeContainer attributeContainer,
				ConfiguredClass configuredClass,
				Map<String,AttributeOverride> attributeOverrideMap) {
		for ( SimpleAttribute simpleAttribute : configuredClass.getSimpleAttributes() ) {
			String attributeName = simpleAttribute.getName();

			// if there is a override apply it
			AttributeOverride override = attributeOverrideMap.get( attributeName );
			if ( override != null ) {
				simpleAttribute = SimpleAttribute.createSimpleAttribute( simpleAttribute, override.getColumnValues() );
			}

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
		while ( containsMappedSuperclassAttributes( parent ) ) {
			bindEmbeddedAttributes( entityBinding, superTypeContainer, parent );
			parent = parent.getParent();
			superTypeContainer = superTypeContainer.getSuperType();
		}
	}

	private void bindEmbeddedAttributes(
				EntityBinding entityBinding,
				AttributeContainer attributeContainer,
				ConfiguredClass configuredClass) {
		for ( Map.Entry<String, EmbeddableClass> entry : configuredClass.getEmbeddedClasses().entrySet() ) {
			String attributeName = entry.getKey();
			EmbeddableClass embeddedClass = entry.getValue();
			SingularAttribute component = attributeContainer.locateOrCreateComponentAttribute( attributeName );
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

	private void bindAssociationAttribute(
				EntityBinding entityBinding,
				AttributeContainer container,
				AssociationAttribute associationAttribute) {
		switch ( associationAttribute.getAssociationType() ) {
			case MANY_TO_ONE: {
				entityBinding.getEntity().locateOrCreateSingularAttribute( associationAttribute.getName() );
				ManyToOneAttributeBinding manyToOneAttributeBinding = entityBinding.makeManyToOneAttributeBinding(
						associationAttribute.getName()
				);

				ManyToOneAttributeBindingState bindingState = new ManyToOneBindingStateImpl( associationAttribute );
				manyToOneAttributeBinding.initialize( bindingState );

				ManyToOneRelationalStateImpl relationalState = new ManyToOneRelationalStateImpl();
				if ( entityClass.hasOwnTable() ) {
					ColumnRelationalStateImpl columnRelationsState = new ColumnRelationalStateImpl(
							associationAttribute, bindingContext.getMetadataImplementor()
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

	private void bindSingleMappedAttribute(
				EntityBinding entityBinding,
				AttributeContainer container,
				SimpleAttribute simpleAttribute) {
		if ( simpleAttribute.isId() ) {
			return;
		}

		String attributeName = simpleAttribute.getName();
		SingularAttribute attribute = entityBinding.getEntity().locateOrCreateSingularAttribute( attributeName );
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
					simpleAttribute, bindingContext.getMetadataImplementor()
			);
			TupleRelationalStateImpl relationalState = new TupleRelationalStateImpl();
			relationalState.addValueState( columnRelationsState );

			attributeBinding.initialize( relationalState );
		}
	}

}

