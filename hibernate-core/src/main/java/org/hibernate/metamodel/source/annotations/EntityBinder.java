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
package org.hibernate.metamodel.source.annotations;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.source.annotations.state.binding.AnnotationsAttributeBindingState;
import org.hibernate.metamodel.source.annotations.state.binding.AnnotationsDiscriminatorBindingState;
import org.hibernate.metamodel.source.annotations.state.relational.AttributeColumnRelationalState;
import org.hibernate.metamodel.source.annotations.state.relational.AttributeTupleRelationalState;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.state.binding.DiscriminatorBindingState;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Creates the domain and relational metamodel for a configured class and binds them together.
 *
 * @author Hardy Ferentschik
 */
public class EntityBinder {
	private final ConfiguredClass configuredClass;
	private final MetadataImpl meta;

	private Schema.Name schemaName;

	public EntityBinder(MetadataImpl metadata, ConfiguredClass configuredClass) {
		this.configuredClass = configuredClass;
		this.meta = metadata;
	}

	public void bind() {
		EntityBinding entityBinding = new EntityBinding();

		bindJpaEntityAnnotation( entityBinding );
		bindHibernateEntityAnnotation( entityBinding ); // optional hibernate specific @org.hibernate.annotations.Entity

		schemaName = createSchemaName();
		bindTable( entityBinding );

		entityBinding.setInheritanceType( configuredClass.getInheritanceType() );
		bindInheritance( entityBinding );

		bindWhereFilter( entityBinding );

		bindJpaCaching( entityBinding );
		bindHibernateCaching( entityBinding );

		if ( configuredClass.isRoot() ) {
			bindId( entityBinding );
		}
		bindAttributes( entityBinding );

		meta.addEntity( entityBinding );
	}

	private void bindInheritance(EntityBinding entityBinding) {
		switch ( configuredClass.getInheritanceType() ) {
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
		MappedAttribute discriminatorAttribute = MappedAttribute.createDiscriminatorAttribute(
				configuredClass.getClassInfo().annotations()
		);

		bindSingleMappedAttribute( entityBinding, discriminatorAttribute );

		if ( !( discriminatorAttribute.getColumnValues() instanceof DiscriminatorColumnValues ) ) {
			throw new AssertionFailure( "Expected discriminator column values" );
		}

		// TODO: move this into DiscriminatorBindingState
		DiscriminatorColumnValues discriminatorColumnvalues = (DiscriminatorColumnValues) discriminatorAttribute.getColumnValues();
		entityBinding.setDiscriminatorValue( discriminatorColumnvalues.getDiscriminatorValue() );
	}

	private void bindWhereFilter(EntityBinding entityBinding) {
		AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.WHERE
		);
		if ( whereAnnotation != null ) {
			// no null check needed, it is a required attribute
			String clause = whereAnnotation.value( "clause" ).asString();
			entityBinding.setWhereFilter( clause );
		}
	}

	private void bindHibernateCaching(EntityBinding entityBinding) {
		AnnotationInstance cacheAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.CACHE
		);
		if ( cacheAnnotation == null ) {
			return;
		}

		String region;
		if ( cacheAnnotation.value( "region" ) != null ) {
			region = cacheAnnotation.value( "region" ).asString();
		}
		else {
			region = entityBinding.getEntity().getName();
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
		entityBinding.setCaching( caching );
	}

	// This does not take care of any inheritance of @Cacheable within a class hierarchy as specified in JPA2.
	// This is currently not supported (HF)
	private void bindJpaCaching(EntityBinding entityBinding) {
		AnnotationInstance cacheAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.CACHEABLE
		);

		boolean cacheable = true; // true is the default
		if ( cacheAnnotation != null && cacheAnnotation.value() != null ) {
			cacheable = cacheAnnotation.value().asBoolean();
		}

		Caching caching = null;
		switch ( meta.getSharedCacheMode() ) {
			case ALL: {
				caching = createCachingForCacheableAnnotation( entityBinding );
				break;
			}
			case ENABLE_SELECTIVE: {
				if ( cacheable ) {
					caching = createCachingForCacheableAnnotation( entityBinding );
				}
				break;
			}
			case DISABLE_SELECTIVE: {
				if ( cacheAnnotation == null || cacheable ) {
					caching = createCachingForCacheableAnnotation( entityBinding );
				}
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				break;
			}
		}
		if ( caching != null ) {
			entityBinding.setCaching( caching );
		}
	}

	private Caching createCachingForCacheableAnnotation(EntityBinding entityBinding) {
		String region = entityBinding.getEntity().getName();
		RegionFactory regionFactory = meta.getServiceRegistry().getService( RegionFactory.class );
		AccessType defaultAccessType = regionFactory.getDefaultAccessType();
		return new Caching( region, defaultAccessType, true );
	}

	private Schema.Name createSchemaName() {
		String schema = null;
		String catalog = null;

		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.TABLE
		);
		if ( tableAnnotation != null ) {
			AnnotationValue schemaValue = tableAnnotation.value( "schema" );
			AnnotationValue catalogValue = tableAnnotation.value( "catalog" );

			schema = schemaValue != null ? schemaValue.asString() : null;
			catalog = catalogValue != null ? catalogValue.asString() : null;
		}

		return new Schema.Name( schema, catalog );
	}

	private void bindTable(EntityBinding entityBinding) {
		final Schema schema = meta.getDatabase().getSchema( schemaName );
		final Identifier tableName = Identifier.toIdentifier( configuredClass.getPrimaryTableName() );
		org.hibernate.metamodel.relational.Table table = schema.getTable( tableName );
		if ( table == null ) {
			table = schema.createTable( tableName );
		}
		entityBinding.setBaseTable( table );

		AnnotationInstance checkAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.CHECK
		);
		if ( checkAnnotation != null ) {
			table.addCheckConstraint( checkAnnotation.value( "constraints" ).asString() );
		}
	}

	private void bindId(EntityBinding entityBinding) {
		switch ( configuredClass.getIdType() ) {
			case SIMPLE: {
				bindSingleIdAnnotation( entityBinding );
				break;
			}
			case COMPOSED: {
				// todo
				break;
			}
			case EMBEDDED: {
				// todo
				break;
			}
			default: {
			}
		}
	}

	private void bindJpaEntityAnnotation(EntityBinding entityBinding) {
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.ENTITY
		);
		String name;
		if ( jpaEntityAnnotation.value( "name" ) == null ) {
			name = StringHelper.unqualify( configuredClass.getName() );
		}
		else {
			name = jpaEntityAnnotation.value( "name" ).asString();
		}
		entityBinding.setEntity( new Entity( name, getSuperType() ) );
	}

	private void bindSingleIdAnnotation(EntityBinding entityBinding) {
		AnnotationInstance idAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.ID
		);

		String idName = JandexHelper.getPropertyName( idAnnotation.target() );
		MappedAttribute idAttribute = configuredClass.getMappedProperty( idName );

		entityBinding.getEntity().getOrCreateSingularAttribute( idName );
		entityBinding.makeSimpleIdAttributeBinding( idName )
				.initialize( new AnnotationsAttributeBindingState( idAttribute ) )
				.initialize( new AttributeColumnRelationalState( idAttribute, meta ) );
	}

	private void bindAttributes(EntityBinding entityBinding) {
		for ( MappedAttribute mappedAttribute : configuredClass.getMappedAttributes() ) {
			bindSingleMappedAttribute( entityBinding, mappedAttribute );
		}
	}

	private void bindSingleMappedAttribute(EntityBinding entityBinding, MappedAttribute mappedAttribute) {
		if ( mappedAttribute.isId() ) {
			return;
		}

		String attributeName = mappedAttribute.getName();
		entityBinding.getEntity().getOrCreateSingularAttribute( attributeName );
		SimpleAttributeBinding attributeBinding;

		if ( mappedAttribute.isDiscriminator() ) {
			attributeBinding = entityBinding.makeEntityDiscriminator( attributeName )
					.initialize( new AnnotationsDiscriminatorBindingState( mappedAttribute ) )
					.getValueBinding();
		}
		else {
			attributeBinding = mappedAttribute.isVersioned() ?
					entityBinding.makeVersionBinding( attributeName ) :
					entityBinding.makeSimpleAttributeBinding( attributeName );
			attributeBinding.initialize(  new AnnotationsAttributeBindingState( mappedAttribute ) );
		}

		if ( configuredClass.hasOwnTable() ) {
			AttributeColumnRelationalState columnRelationsState = new AttributeColumnRelationalState(
					mappedAttribute, meta
			);
			AttributeTupleRelationalState relationalState = new AttributeTupleRelationalState();
			relationalState.addValueState( columnRelationsState );
			// TODO: if this really just binds a column, then it can be changed to
			// attributeBinding.initialize( columnRelationsState );
			attributeBinding.initialize( relationalState );
		}
	}

	private void bindHibernateEntityAnnotation(EntityBinding entityBinding) {
		// initialize w/ the defaults
		boolean mutable = true;
		boolean dynamicInsert = false;
		boolean dynamicUpdate = false;
		boolean selectBeforeUpdate = false;
		PolymorphismType polymorphism = PolymorphismType.IMPLICIT;
		OptimisticLockType optimisticLock = OptimisticLockType.VERSION;

		AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.ENTITY
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
				String persister = ( hibernateEntityAnnotation.value( "persister" ).toString() );
				ClassLoaderService classLoaderService = meta.getServiceRegistry()
						.getService( ClassLoaderService.class );
				Class<?> persisterClass = classLoaderService.classForName( persister );
				entityBinding.setEntityPersisterClass( persisterClass );
			}
		}

		// also check for the immutable annotation
		AnnotationInstance immutableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.IMMUTABLE
		);
		if ( immutableAnnotation != null ) {
			mutable = false;
		}

		entityBinding.setMutable( mutable );
		entityBinding.setDynamicInsert( dynamicInsert );
		entityBinding.setDynamicUpdate( dynamicUpdate );
		entityBinding.setSelectBeforeUpdate( selectBeforeUpdate );
		entityBinding.setExplicitPolymorphism( PolymorphismType.EXPLICIT.equals( polymorphism ) );
		entityBinding.setOptimisticLockMode( optimisticLock.ordinal() );
	}

	private Hierarchical getSuperType() {
		ConfiguredClass parent = configuredClass.getParent();
		if ( parent == null ) {
			return null;
		}

		EntityBinding parentBinding = meta.getEntityBinding( parent.getSimpleName() );
		if ( parentBinding == null ) {
			throw new AssertionFailure(
					"Parent entity " + parent.getName() + " of entity " + configuredClass.getName() + " not yet created!"
			);
		}

		return parentBinding.getEntity();
	}
}

