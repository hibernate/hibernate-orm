/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.AccessType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.attribute.PrimaryKeyJoinColumn;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPAListenerHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

import com.fasterxml.classmate.ResolvedTypeWithMembers;

/**
 * Represents an entity or mapped superclass configured via annotations/orm-xml.
 *
 * @author Hardy Ferentschik
 */
public class EntityClass extends ConfiguredClass {
	private static final String NATURAL_ID_CACHE_SUFFIX = "##NaturalId";

	private final InheritanceType inheritanceType;

	private final String explicitEntityName;
	private final String customLoaderQueryName;
	private final String[] synchronizedTableNames;
	private final int batchSize;

	private boolean isImmutable;
	private boolean isExplicitPolymorphism;
	private OptimisticLockStyle optimisticLockStyle;
	private String whereClause;
	private String rowId;
	private Caching caching;
	private Caching naturalIdCaching;
	private boolean isDynamicInsert;
	private boolean isDynamicUpdate;
	private boolean isSelectBeforeUpdate;
	private String customPersister;

	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;


	private final String inverseForeignKeyName;
	private final String explicitForeignKeyName;

	private final OnDeleteAction onDeleteAction;

	private boolean isLazy;
	private String proxy;

	private String discriminatorMatchValue;

	private  List<JpaCallbackSource> jpaCallbacks;
	private final List<PrimaryKeyJoinColumn> joinedSubclassPrimaryKeyJoinColumnSources;

	/**
	 * Constructor used for entities within a hierarchy (non entity roots)
	 *
	 * @param classInfo the jandex class info this this entity
	 * @param parent the parent entity
	 * @param hierarchyAccessType the default access type
	 * @param inheritanceType the inheritance type this entity
	 * @param context the binding context
	 */
	public EntityClass(
			ClassInfo classInfo,
			ResolvedTypeWithMembers fullyResolvedType,
			EntityClass parent,
			AccessType hierarchyAccessType,
			InheritanceType inheritanceType,
			AnnotationBindingContext context) {
		super( classInfo, fullyResolvedType, hierarchyAccessType, parent, context );
		this.inheritanceType = inheritanceType;

		this.explicitEntityName = determineExplicitEntityName();
		this.customLoaderQueryName = determineCustomLoader();
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();

		this.customInsert = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_INSERT,
				getClassInfo().annotations(),
				getClassInfo()
		);
		this.customUpdate = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_UPDATE,
				getClassInfo().annotations(),
				getClassInfo()
		);
		this.customDelete = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_DELETE,
				getClassInfo().annotations(),
				getClassInfo()
		);


		processHibernateEntitySpecificAnnotations();
		processProxyGeneration();
		processDiscriminatorValue();

		AnnotationInstance foreignKey = JandexHelper.getSingleAnnotation(
				classInfo,
				HibernateDotNames.FOREIGN_KEY,
				ClassInfo.class
		);
		this.joinedSubclassPrimaryKeyJoinColumnSources = determinePrimaryKeyJoinColumns();
		if ( foreignKey != null ) {
			ensureJoinedSubEntity();
			explicitForeignKeyName = JandexHelper.getValue( foreignKey, "name", String.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
			String temp = JandexHelper.getValue( foreignKey, "inverseName", String.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
			inverseForeignKeyName = StringHelper.isNotEmpty( temp ) ? temp : null;
		}
		else {
			explicitForeignKeyName = null;
			inverseForeignKeyName = null;
		}
		this.onDeleteAction = determineOnDeleteAction();

	}

	private OnDeleteAction determineOnDeleteAction() {
		final AnnotationInstance onDeleteAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.ON_DELETE,
				ClassInfo.class
		);
		if ( onDeleteAnnotation != null ) {
			ensureJoinedSubEntity();
			return JandexHelper.getEnumValue( onDeleteAnnotation, "action", OnDeleteAction.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}
		return null;
	}

	private void ensureJoinedSubEntity() {
		if ( !( getParent() != null && inheritanceType == InheritanceType.JOINED ) ) {
			throw getLocalBindingContext().makeMappingException( explicitEntityName + "is not a joined sub entity" );
		}
	}

	public boolean isExplicitPolymorphism() {
		return isExplicitPolymorphism;
	}

	public boolean isMutable() {
		return !isImmutable;
	}

	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public String getRowId() {
		return rowId;
	}

	public Caching getCaching() {
		return caching;
	}

	public Caching getNaturalIdCaching() {
		return naturalIdCaching;
	}

	public String getExplicitEntityName() {
		return explicitEntityName;
	}

	public String getEntityName() {
		return getConfiguredClass().getSimpleName();
	}

	public boolean isDynamicInsert() {
		return isDynamicInsert;
	}

	public boolean isDynamicUpdate() {
		return isDynamicUpdate;
	}

	public boolean isSelectBeforeUpdate() {
		return isSelectBeforeUpdate;
	}

	public String getCustomLoaderQueryName() {
		return customLoaderQueryName;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public String[] getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	public List<PrimaryKeyJoinColumn> getJoinedSubclassPrimaryKeyJoinColumnSources() {
		return joinedSubclassPrimaryKeyJoinColumnSources;
	}

	public String getCustomPersister() {
		return customPersister;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public String getProxy() {
		return proxy;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public boolean isEntityRoot() {
		return getParent() == null;
	}

	public String getDiscriminatorMatchValue() {
		return discriminatorMatchValue;
	}

	public List<JpaCallbackSource> getJpaCallbacks() {
		if ( jpaCallbacks == null ) {
			jpaCallbacks = new JPAListenerHelper( this ).bindJPAListeners();
		}
		return jpaCallbacks;
	}

	public String getInverseForeignKeyName() {
		return inverseForeignKeyName;
	}
	public String getExplicitForeignKeyName(){
		return explicitForeignKeyName;
	}
	public List<MappedSuperclass> getMappedSuperclasses() {
		return Collections.emptyList();
	}

	public OnDeleteAction getOnDeleteAction() {
		return onDeleteAction;
	}

	public boolean definesItsOwnTable() {
		return !InheritanceType.SINGLE_TABLE.equals( inheritanceType ) || isEntityRoot();
	}

	private String determineExplicitEntityName() {
		final AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.ENTITY
		);
		return JandexHelper.getValue( jpaEntityAnnotation, "name", String.class,
				getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
	}

	protected List<PrimaryKeyJoinColumn> determinePrimaryKeyJoinColumns() {
		final AnnotationInstance primaryKeyJoinColumns = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				JPADotNames.PRIMARY_KEY_JOIN_COLUMNS,
				ClassInfo.class
		);
		final AnnotationInstance primaryKeyJoinColumn = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				JPADotNames.PRIMARY_KEY_JOIN_COLUMN,
				ClassInfo.class
		);

		if ( primaryKeyJoinColumn != null || primaryKeyJoinColumns != null ) {
			ensureJoinedSubEntity();
		}

		final List<PrimaryKeyJoinColumn> results;
		if ( primaryKeyJoinColumns != null ) {
			AnnotationInstance[] values = primaryKeyJoinColumns.value().asNestedArray();
			results = new ArrayList<PrimaryKeyJoinColumn>( values.length );
			for ( final AnnotationInstance annotationInstance : values ) {
				results.add( new PrimaryKeyJoinColumn( annotationInstance ) );
			}
		}
		else if ( primaryKeyJoinColumn != null ) {
			results = new ArrayList<PrimaryKeyJoinColumn>( 1 );
			results.add( new PrimaryKeyJoinColumn( primaryKeyJoinColumn ) );
		}
		else {
			results = null;
		}
		return results;
	}


	private void processDiscriminatorValue() {
		final AnnotationInstance discriminatorValueAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.DISCRIMINATOR_VALUE
		);
		if ( discriminatorValueAnnotation != null ) {
			this.discriminatorMatchValue = discriminatorValueAnnotation.value().asString();
		}
	}

	private void processHibernateEntitySpecificAnnotations() {
		// see HHH-6400
		PolymorphismType polymorphism = PolymorphismType.IMPLICIT;
		final AnnotationInstance polymorphismAnnotation = JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.POLYMORPHISM );
		if ( polymorphismAnnotation != null && polymorphismAnnotation.value( "type" ) != null ) {
			polymorphism = PolymorphismType.valueOf( polymorphismAnnotation.value( "type" ).asEnum() );
		}
		isExplicitPolymorphism = polymorphism == PolymorphismType.EXPLICIT;

		// see HHH-6401
		OptimisticLockType optimisticLockType = OptimisticLockType.VERSION;
		final AnnotationInstance optimisticLockAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.OPTIMISTIC_LOCK,
				ClassInfo.class
		);
		if ( optimisticLockAnnotation != null ) {
			optimisticLockType = JandexHelper.getEnumValue(
					optimisticLockAnnotation,
					"type",
					OptimisticLockType.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class )
			);
		}
		optimisticLockStyle = OptimisticLockStyle.valueOf( optimisticLockType.name() );

		final AnnotationInstance hibernateImmutableAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.IMMUTABLE,
				ClassInfo.class
		);
		isImmutable = hibernateImmutableAnnotation != null ;
		final AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.WHERE
		);
		whereClause = whereAnnotation != null && whereAnnotation.value( "clause" ) != null ?
				whereAnnotation.value( "clause" ).asString() : null;

		final AnnotationInstance rowIdAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.ROW_ID
		);
		rowId = rowIdAnnotation != null && rowIdAnnotation.value() != null
				? rowIdAnnotation.value().asString() : null;

		caching = determineCachingSettings();

		naturalIdCaching = determineNaturalIdCachingSettings( caching );

		// see HHH-6397
		final AnnotationInstance dynamicInsertAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.DYNAMIC_INSERT
		);
		if ( dynamicInsertAnnotation != null ) {
			isDynamicInsert = JandexHelper.getValue( dynamicInsertAnnotation, "value", Boolean.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}

		// see HHH-6398
		final AnnotationInstance dynamicUpdateAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.DYNAMIC_UPDATE
		);
		if ( dynamicUpdateAnnotation != null ) {
			isDynamicUpdate = JandexHelper.getValue( dynamicUpdateAnnotation, "value", Boolean.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}


		// see HHH-6399
		final AnnotationInstance selectBeforeUpdateAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.SELECT_BEFORE_UPDATE
		);
		if ( selectBeforeUpdateAnnotation != null ) {
			isSelectBeforeUpdate = JandexHelper.getValue( selectBeforeUpdateAnnotation, "value", Boolean.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}

		// Custom persister
		String entityPersisterClass = null;
		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.PERSISTER, ClassInfo.class
		);
		if ( persisterAnnotation != null && persisterAnnotation.value( "impl" ) != null ) {
			entityPersisterClass = persisterAnnotation.value( "impl" ).asString();
		}
		this.customPersister = entityPersisterClass;
	}

	private Caching determineNaturalIdCachingSettings(final Caching entityCache) {
		final AnnotationInstance naturalIdCacheAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.NATURAL_ID_CACHE
		);
		if ( naturalIdCacheAnnotation == null ) {
			return null;
		}
		final String region;
		if ( naturalIdCacheAnnotation.value( "region" ) == null || StringHelper.isEmpty(
				naturalIdCacheAnnotation.value(
						"region"
				).asString()
		) ) {
			region = entityCache == null ? getEntityName() + NATURAL_ID_CACHE_SUFFIX : entityCache.getRegion() + NATURAL_ID_CACHE_SUFFIX;
		}
		else {
			region = naturalIdCacheAnnotation.value( "region" ).asString();
		}
		return new Caching( region, null, false );
	}

	private Caching determineCachingSettings() {
		final List<AnnotationInstance> annotationInstanceList = getClassInfo().annotations().get( HibernateDotNames.CACHE );
		if ( CollectionHelper.isNotEmpty( annotationInstanceList ) ) {
			for ( final AnnotationInstance hibernateCacheAnnotation : annotationInstanceList ) {
				if ( ClassInfo.class.isInstance( hibernateCacheAnnotation.target() ) ) {
					final org.hibernate.cache.spi.access.AccessType accessType = hibernateCacheAnnotation.value( "usage" ) == null
							? getLocalBindingContext().getMappingDefaults().getCacheAccessType()
							: CacheConcurrencyStrategy.parse( hibernateCacheAnnotation.value( "usage" ).asEnum() )
							.toAccessType();
					return new Caching(
							hibernateCacheAnnotation.value( "region" ) == null
									? getName()
									: hibernateCacheAnnotation.value( "region" ).asString(),
							accessType,
							hibernateCacheAnnotation.value( "include" ) != null
									&& "all".equals( hibernateCacheAnnotation.value( "include" ).asString() )
					);
				}
			}
		}

		final AnnotationInstance jpaCacheableAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.CACHEABLE
		);

		final boolean doCaching;
		switch ( getLocalBindingContext().getMetadataImplementor().getOptions().getSharedCacheMode() ) {
			case ALL: {
				doCaching = true;
				break;
			}
			case ENABLE_SELECTIVE: {
				doCaching = jpaCacheableAnnotation != null
						&& JandexHelper.getValue( jpaCacheableAnnotation, "value", Boolean.class,
								getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
				break;
			}
			case DISABLE_SELECTIVE: {
				doCaching = jpaCacheableAnnotation == null
						|| !JandexHelper.getValue( jpaCacheableAnnotation, "value", Boolean.class,
								getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				doCaching = false;
				break;
			}
		}

		if ( !doCaching ) {
			return null;
		}

		return new Caching(
				getName(),
				getLocalBindingContext().getMappingDefaults().getCacheAccessType(),
				true
		);
	}

	public boolean hasMultiTenancySourceInformation() {
		return JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.MULTI_TENANT ) != null
				|| JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.TENANT_COLUMN ) != null
				|| JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.TENANT_FORMULA ) != null;
	}

	private String determineCustomLoader() {
		String customLoader = null;
		// Custom sql loader
		final AnnotationInstance sqlLoaderAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.LOADER
		);
		if ( sqlLoaderAnnotation != null && sqlLoaderAnnotation.target() instanceof ClassInfo) {
			customLoader = sqlLoaderAnnotation.value( "namedQuery" ).asString();
		}
		return customLoader;
	}

	private String[] determineSynchronizedTableNames() {
		final AnnotationInstance synchronizeAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);
		if ( synchronizeAnnotation != null ) {
			return synchronizeAnnotation.value().asStringArray();
		}
		else {
			return StringHelper.EMPTY_STRINGS;
		}
	}

	private void processProxyGeneration() {
		// Proxy generation
		final AnnotationInstance hibernateProxyAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.PROXY
		);
		if ( hibernateProxyAnnotation != null ) {
			isLazy = hibernateProxyAnnotation.value( "lazy" ) == null
					|| hibernateProxyAnnotation.value( "lazy" ).asBoolean();
			if ( isLazy ) {
				final AnnotationValue proxyClassValue = hibernateProxyAnnotation.value( "proxyClass" );
				proxy = proxyClassValue == null ? getName() : proxyClassValue.asString();
			}
			else {
				proxy = null;
			}
		}
		else {
			isLazy = true;
			proxy = getName();
		}
	}

	private int determineBatchSize() {
		final AnnotationInstance batchSizeAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.BATCH_SIZE
		);
		return batchSizeAnnotation == null ? -1 : batchSizeAnnotation.value( "size" ).asInt();
	}

}
