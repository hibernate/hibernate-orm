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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AccessType;
import javax.persistence.PersistenceException;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.JpaCallbackSourceImpl;
import org.hibernate.metamodel.internal.source.annotations.PrimaryKeyJoinColumnSourceImpl;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.internal.source.annotations.xml.PseudoJpaDotNames;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;

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
	private final List<String> synchronizedTableNames;
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

	private boolean isLazy;
	private String proxy;

	private String discriminatorMatchValue;

	private final List<JpaCallbackSource> jpaCallbacks;
	private final List<PrimaryKeyJoinColumnSource> joinedSubclassPrimaryKeyJoinColumnSources;

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
			EntityClass parent,
			AccessType hierarchyAccessType,
			InheritanceType inheritanceType,
			AnnotationBindingContext context) {
		super( classInfo, hierarchyAccessType, parent, context );
		this.inheritanceType = inheritanceType;

		this.explicitEntityName = determineExplicitEntityName();
		this.customLoaderQueryName = determineCustomLoader();
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();
		this.jpaCallbacks = determineEntityListeners();

		this.customInsert = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_INSERT,
				getClassInfo().annotations()
		);
		this.customUpdate = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_UPDATE,
				getClassInfo().annotations()
		);
		this.customDelete = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_DELETE,
				getClassInfo().annotations()
		);

		this.joinedSubclassPrimaryKeyJoinColumnSources = determinPrimaryKeyJoinColumns();
		processHibernateEntitySpecificAnnotations();
		processProxyGeneration();
		processDiscriminatorValue();
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

	public List<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	public List<PrimaryKeyJoinColumnSource> getJoinedSubclassPrimaryKeyJoinColumnSources() {
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
		return jpaCallbacks;
	}

	public boolean definesItsOwnTable() {
		return !InheritanceType.SINGLE_TABLE.equals( inheritanceType ) || isEntityRoot();
	}

	private String determineExplicitEntityName() {
		final AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.ENTITY
		);
		return JandexHelper.getValue( jpaEntityAnnotation, "name", String.class );
	}

	private List<PrimaryKeyJoinColumnSource> determinPrimaryKeyJoinColumns() {
		if ( inheritanceType != InheritanceType.JOINED ) {
			return null;
		}
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
		final List<PrimaryKeyJoinColumnSource> results;
		if ( primaryKeyJoinColumns != null ) {
			AnnotationInstance[] values = primaryKeyJoinColumns.value().asNestedArray();
			results = new ArrayList<PrimaryKeyJoinColumnSource>( values.length );
			for ( final AnnotationInstance annotationInstance : values ) {
				results.add( new PrimaryKeyJoinColumnSourceImpl( annotationInstance ) );
			}
		}
		else if ( primaryKeyJoinColumn != null ) {
			results = new ArrayList<PrimaryKeyJoinColumnSource>( 1 );
			results.add( new PrimaryKeyJoinColumnSourceImpl( primaryKeyJoinColumn ) );
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
		final AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.ENTITY
		);

		// see HHH-6400
		PolymorphismType polymorphism = PolymorphismType.IMPLICIT;
		final AnnotationInstance polymorphismAnnotation = JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.POLYMORPHISM );
		if ( polymorphismAnnotation != null && polymorphismAnnotation.value( "type" ) != null ) {
			polymorphism = PolymorphismType.valueOf( polymorphismAnnotation.value( "type" ).asEnum() );
		}
		else if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "polymorphism" ) != null ) {
			polymorphism = PolymorphismType.valueOf( hibernateEntityAnnotation.value( "polymorphism" ).asEnum() );
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
					OptimisticLockType.class
			);
		}
		else if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "optimisticLock" ) != null ) {
			optimisticLockType = OptimisticLockType.valueOf(
					hibernateEntityAnnotation.value( "optimisticLock" )
							.asEnum()
			);
		}
		optimisticLockStyle = OptimisticLockStyle.valueOf( optimisticLockType.name() );

		final AnnotationInstance hibernateImmutableAnnotation = JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.IMMUTABLE, ClassInfo.class );
		if ( hibernateImmutableAnnotation != null ) {
			isImmutable = true;
		}
		else if (hibernateEntityAnnotation != null
				&& hibernateEntityAnnotation.value( "mutable" ) != null){

			isImmutable = !hibernateEntityAnnotation.value( "mutable" ).asBoolean();
		} else {
			isImmutable = false;
		}

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
			isDynamicInsert = JandexHelper.getValue( dynamicInsertAnnotation, "value", Boolean.class );
		}
		else {
			isDynamicInsert =
					hibernateEntityAnnotation != null
							&& hibernateEntityAnnotation.value( "dynamicInsert" ) != null
							&& hibernateEntityAnnotation.value( "dynamicInsert" ).asBoolean();
		}

		// see HHH-6398
		final AnnotationInstance dynamicUpdateAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.DYNAMIC_UPDATE
		);
		if ( dynamicUpdateAnnotation != null ) {
			isDynamicUpdate = JandexHelper.getValue( dynamicUpdateAnnotation, "value", Boolean.class );
		}
		else {
			isDynamicUpdate =
					hibernateEntityAnnotation != null
							&& hibernateEntityAnnotation.value( "dynamicUpdate" ) != null
							&& hibernateEntityAnnotation.value( "dynamicUpdate" ).asBoolean();
		}


		// see HHH-6399
		final AnnotationInstance selectBeforeUpdateAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.SELECT_BEFORE_UPDATE
		);
		if ( selectBeforeUpdateAnnotation != null ) {
			isSelectBeforeUpdate = JandexHelper.getValue( selectBeforeUpdateAnnotation, "value", Boolean.class );
		}
		else {
			isSelectBeforeUpdate =
					hibernateEntityAnnotation != null
							&& hibernateEntityAnnotation.value( "selectBeforeUpdate" ) != null
							&& hibernateEntityAnnotation.value( "selectBeforeUpdate" ).asBoolean();
		}

		// Custom persister
		final String entityPersisterClass;
		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.PERSISTER
		);
		if ( persisterAnnotation == null || persisterAnnotation.value( "impl" ) == null ) {
			if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "persister" ) != null ) {
				entityPersisterClass = hibernateEntityAnnotation.value( "persister" ).asString();
			}
			else {
				entityPersisterClass = null;
			}
		}
		else {
			if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "persister" ) != null ) {
				// todo : error?
			}
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
						&& JandexHelper.getValue( jpaCacheableAnnotation, "value", Boolean.class );
				break;
			}
			case DISABLE_SELECTIVE: {
				doCaching = jpaCacheableAnnotation == null
						|| !JandexHelper.getValue( jpaCacheableAnnotation, "value", Boolean.class );
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

	private List<String> determineSynchronizedTableNames() {
		final AnnotationInstance synchronizeAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);
		if ( synchronizeAnnotation != null ) {
			final String[] tableNames = synchronizeAnnotation.value().asStringArray();
			return Arrays.asList( tableNames );
		}
		else {
			return Collections.emptyList();
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

	private List<JpaCallbackSource> determineEntityListeners() {
		List<JpaCallbackSource> callbackClassList = new ArrayList<JpaCallbackSource>();

		// Bind default JPA entity listener callbacks (unless excluded), using superclasses first (unless excluded)
		if ( JandexHelper.getSingleAnnotation( getClassInfo(), JPADotNames.EXCLUDE_DEFAULT_LISTENERS ) == null ) {
			Collection<AnnotationInstance> defaultEntityListenerAnnotations = getLocalBindingContext().getIndex()
					.getAnnotations( PseudoJpaDotNames.DEFAULT_ENTITY_LISTENERS );
			for ( AnnotationInstance annotation : defaultEntityListenerAnnotations ) {
				for ( Type callbackClass : annotation.value().asClassArray() ) {
					String callbackClassName = callbackClass.name().toString();
					try {
						processDefaultJpaCallbacks( callbackClassName, callbackClassList );
					}
					catch ( PersistenceException error ) {
						throw new PersistenceException( error.getMessage() + "default entity listener " + callbackClassName );
					}
				}
			}
		}

		// Bind JPA entity listener callbacks, using superclasses first (unless excluded)
		List<AnnotationInstance> annotationList = getClassInfo().annotations().get( JPADotNames.ENTITY_LISTENERS );
		if ( annotationList != null ) {
			for ( AnnotationInstance annotation : annotationList ) {
				for ( Type callbackClass : annotation.value().asClassArray() ) {
					String callbackClassName = callbackClass.name().toString();
					try {
						processJpaCallbacks( callbackClassName, true, callbackClassList );
					}
					catch ( PersistenceException error ) {
						throw new PersistenceException( error.getMessage() + "entity listener " + callbackClassName );
					}
				}
			}
		}

		// Bind JPA entity.mapped superclass callbacks, using superclasses first (unless excluded)
		try {
			processJpaCallbacks( getName(), false, callbackClassList );
		}
		catch ( PersistenceException error ) {
			throw new PersistenceException(
					error.getMessage() + "entity/mapped superclass " + getClassInfo().name().toString()
			);
		}

		return callbackClassList;
	}

	private void processDefaultJpaCallbacks(String instanceCallbackClassName, List<JpaCallbackSource> jpaCallbackClassList) {
		ClassInfo callbackClassInfo = getLocalBindingContext().getClassInfo( instanceCallbackClassName );

		// Process superclass first if available and not excluded
		if ( JandexHelper.getSingleAnnotation( callbackClassInfo, JPADotNames.EXCLUDE_SUPERCLASS_LISTENERS ) != null ) {
			DotName superName = callbackClassInfo.superName();
			if ( superName != null ) {
				processDefaultJpaCallbacks( instanceCallbackClassName, jpaCallbackClassList );
			}
		}

		String callbackClassName = callbackClassInfo.name().toString();
		Map<Class<?>, String> callbacksByType = new HashMap<Class<?>, String>();
		createDefaultCallback(
				PrePersist.class, PseudoJpaDotNames.DEFAULT_PRE_PERSIST, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PreRemove.class, PseudoJpaDotNames.DEFAULT_PRE_REMOVE, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PreUpdate.class, PseudoJpaDotNames.DEFAULT_PRE_UPDATE, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PostLoad.class, PseudoJpaDotNames.DEFAULT_POST_LOAD, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PostPersist.class, PseudoJpaDotNames.DEFAULT_POST_PERSIST, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PostRemove.class, PseudoJpaDotNames.DEFAULT_POST_REMOVE, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PostUpdate.class, PseudoJpaDotNames.DEFAULT_POST_UPDATE, callbackClassName, callbacksByType
		);
		if ( !callbacksByType.isEmpty() ) {
			jpaCallbackClassList.add( new JpaCallbackSourceImpl( instanceCallbackClassName, callbacksByType, true ) );
		}
	}

	private void processJpaCallbacks(String instanceCallbackClassName, boolean isListener, List<JpaCallbackSource> callbackClassList) {

		ClassInfo callbackClassInfo = getLocalBindingContext().getClassInfo( instanceCallbackClassName );

		// Process superclass first if available and not excluded
		if ( JandexHelper.getSingleAnnotation( callbackClassInfo, JPADotNames.EXCLUDE_SUPERCLASS_LISTENERS ) != null ) {
			DotName superName = callbackClassInfo.superName();
			if ( superName != null ) {
				processJpaCallbacks(
						instanceCallbackClassName,
						isListener,
						callbackClassList
				);
			}
		}

		Map<Class<?>, String> callbacksByType = new HashMap<Class<?>, String>();
		createCallback( PrePersist.class, JPADotNames.PRE_PERSIST, callbacksByType, callbackClassInfo, isListener );
		createCallback( PreRemove.class, JPADotNames.PRE_REMOVE, callbacksByType, callbackClassInfo, isListener );
		createCallback( PreUpdate.class, JPADotNames.PRE_UPDATE, callbacksByType, callbackClassInfo, isListener );
		createCallback( PostLoad.class, JPADotNames.POST_LOAD, callbacksByType, callbackClassInfo, isListener );
		createCallback( PostPersist.class, JPADotNames.POST_PERSIST, callbacksByType, callbackClassInfo, isListener );
		createCallback( PostRemove.class, JPADotNames.POST_REMOVE, callbacksByType, callbackClassInfo, isListener );
		createCallback( PostUpdate.class, JPADotNames.POST_UPDATE, callbacksByType, callbackClassInfo, isListener );
		if ( !callbacksByType.isEmpty() ) {
			callbackClassList.add(
					new JpaCallbackSourceImpl(
							instanceCallbackClassName,
							callbacksByType,
							isListener
					)
			);
		}
	}

	private void createDefaultCallback(Class callbackTypeClass,
									   DotName callbackTypeName,
									   String callbackClassName,
									   Map<Class<?>, String> callbacksByClass) {
		for ( AnnotationInstance callback : getLocalBindingContext().getIndex().getAnnotations( callbackTypeName ) ) {
			MethodInfo methodInfo = ( MethodInfo ) callback.target();
			validateMethod( methodInfo, callbackTypeClass, callbacksByClass, true );
			if ( methodInfo.declaringClass().name().toString().equals( callbackClassName ) ) {
				if ( methodInfo.args().length != 1 ) {
					throw new PersistenceException(
							String.format(
									"Callback method %s must have exactly one argument defined as either Object or %s in ",
									methodInfo.name(),
									getEntityName()
							)
					);
				}
				callbacksByClass.put( callbackTypeClass, methodInfo.name() );
			}
		}
	}

	private void createCallback(Class callbackTypeClass,
								DotName callbackTypeName,
								Map<Class<?>, String> callbacksByClass,
								ClassInfo callbackClassInfo,
								boolean isListener) {
		Map<DotName, List<AnnotationInstance>> annotations = callbackClassInfo.annotations();
		List<AnnotationInstance> annotationInstances = annotations.get( callbackTypeName );
		if ( annotationInstances == null ) {
			return;
		}
		for ( AnnotationInstance callbackAnnotation : annotationInstances ) {
			MethodInfo methodInfo = ( MethodInfo ) callbackAnnotation.target();
			validateMethod( methodInfo, callbackTypeClass, callbacksByClass, isListener );
			callbacksByClass.put( callbackTypeClass, methodInfo.name() );
		}
	}

	private void validateMethod(MethodInfo methodInfo,
								Class callbackTypeClass,
								Map<Class<?>, String> callbacksByClass,
								boolean isListener) {
		if ( methodInfo.returnType().kind() != Kind.VOID ) {
			throw new PersistenceException( "Callback method " + methodInfo.name() + " must have a void return type in " );
		}
		if ( Modifier.isStatic( methodInfo.flags() ) || Modifier.isFinal( methodInfo.flags() ) ) {
			throw new PersistenceException( "Callback method " + methodInfo.name() + " must not be static or final in " );
		}
		Type[] argTypes = methodInfo.args();
		if ( isListener ) {
			if ( argTypes.length != 1 ) {
				throw new PersistenceException( "Callback method " + methodInfo.name() + " must have exactly one argument in " );
			}
			String argTypeName = argTypes[0].name().toString();
			if ( !argTypeName.equals( Object.class.getName() ) && !argTypeName.equals( getName() ) ) {
				throw new PersistenceException(
						"The argument for callback method " + methodInfo.name() +
								" must be defined as either Object or " + getEntityName() + " in "
				);
			}
		}
		else if ( argTypes.length != 0 ) {
			throw new PersistenceException( "Callback method " + methodInfo.name() + " must have no arguments in " );
		}
		if ( callbacksByClass.containsKey( callbackTypeClass ) ) {
			throw new PersistenceException(
					"Only one method may be annotated as a " + callbackTypeClass.getSimpleName() +
							" callback method in "
			);
		}
	}
}
