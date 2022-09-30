/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.Cacheable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SharedCacheMode;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.annotations.Tables;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.NamingStrategyHelper;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.AnnotatedJoinColumn;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.Value;

import org.hibernate.persister.entity.EntityPersister;
import org.jboss.logging.Logger;

import static org.hibernate.cfg.AnnotatedJoinColumn.buildJoinColumn;
import static org.hibernate.cfg.BinderHelper.isEmptyAnnotationValue;
import static org.hibernate.cfg.BinderHelper.toAliasEntityMap;
import static org.hibernate.cfg.BinderHelper.toAliasTableMap;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.fromExternalName;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;


/**
 * Stateful holder and processor for binding Entity information
 *
 * @author Emmanuel Bernard
 */
public class EntityBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, EntityBinder.class.getName() );
	private static final String NATURAL_ID_CACHE_SUFFIX = "##NaturalId";

	private MetadataBuildingContext context;

	private String name;
	private XClass annotatedClass;
	private PersistentClass persistentClass;
	private String discriminatorValue = "";
	private Boolean forceDiscriminator;
	private Boolean insertableDiscriminator;
	private boolean dynamicInsert;
	private boolean dynamicUpdate;
	private OptimisticLockType optimisticLockType;
	private PolymorphismType polymorphismType;
	private boolean selectBeforeUpdate;
	private int batchSize;
	private boolean lazy;
	private XClass proxyClass;
	private String where;
	// todo : we should defer to InFlightMetadataCollector.EntityTableXref for secondary table tracking;
	//		atm we use both from here; HBM binding solely uses InFlightMetadataCollector.EntityTableXref
	private final java.util.Map<String, Join> secondaryTables = new HashMap<>();
	private final java.util.Map<String, Object> secondaryTableJoins = new HashMap<>();
	private final java.util.Map<String, Join> secondaryTablesFromAnnotation = new HashMap<>();
	private final java.util.Map<String, Object> secondaryTableFromAnnotationJoins = new HashMap<>();

	private final List<Filter> filters = new ArrayList<>();
	private boolean ignoreIdAnnotations;
	private AccessType propertyAccessType = AccessType.DEFAULT;
	private boolean wrapIdsInEmbeddedComponents;
	private String subselect;

	private boolean isCached;
	private String cacheConcurrentStrategy;
	private String cacheRegion;
	private boolean cacheLazyProperty;
	private String naturalIdCacheRegion;

	public boolean wrapIdsInEmbeddedComponents() {
		return wrapIdsInEmbeddedComponents;
	}

	/**
	 * Use as a fake one for Collection of elements
	 */
	public EntityBinder() {
	}

	public EntityBinder(
			XClass annotatedClass,
			PersistentClass persistentClass,
			MetadataBuildingContext context) {
		this.context = context;
		this.persistentClass = persistentClass;
		this.annotatedClass = annotatedClass;
		bindEntityAnnotation();
		bindHibernateAnnotation();
	}

	/**
	 * For the most part, this is a simple delegation to {@link PersistentClass#isPropertyDefinedInHierarchy},
	 * after verifying that PersistentClass is indeed set here.
	 *
	 * @param name The name of the property to check
	 *
	 * @return {@code true} if a property by that given name does already exist in the super hierarchy.
	 */
	public boolean isPropertyDefinedInSuperHierarchy(String name) {
		// Yes, yes... persistentClass can be null because EntityBinder can be used
		// to bind components as well, of course...
		return  persistentClass != null && persistentClass.isPropertyDefinedInSuperHierarchy( name );
	}

	private void bindHibernateAnnotation() {
		final DynamicInsert dynamicInsertAnn = annotatedClass.getAnnotation( DynamicInsert.class );
		dynamicInsert = dynamicInsertAnn != null && dynamicInsertAnn.value();
		final DynamicUpdate dynamicUpdateAnn = annotatedClass.getAnnotation( DynamicUpdate.class );
		dynamicUpdate = dynamicUpdateAnn != null && dynamicUpdateAnn.value();
		final SelectBeforeUpdate selectBeforeUpdateAnn = annotatedClass.getAnnotation( SelectBeforeUpdate.class );
		selectBeforeUpdate = selectBeforeUpdateAnn != null && selectBeforeUpdateAnn.value();
		final OptimisticLocking optimisticLockingAnn = annotatedClass.getAnnotation( OptimisticLocking.class );
		optimisticLockType = optimisticLockingAnn == null ? OptimisticLockType.VERSION : optimisticLockingAnn.type();
		final Polymorphism polymorphismAnn = annotatedClass.getAnnotation( Polymorphism.class );
		polymorphismType = polymorphismAnn == null ? PolymorphismType.IMPLICIT : polymorphismAnn.type();
	}

	private void bindEntityAnnotation() {
		Entity entity = annotatedClass.getAnnotation( Entity.class );
		if ( entity == null ) {
			throw new AssertionFailure( "@Entity should never be missing" );
		}
		name = isEmptyAnnotationValue( entity.name() ) ? unqualify( annotatedClass.getName() ) : entity.name();
	}

	public boolean isRootEntity() {
		// This is the best option I can think of here since PersistentClass is most likely not yet fully populated
		return persistentClass instanceof RootClass;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public void setForceDiscriminator(boolean forceDiscriminator) {
		this.forceDiscriminator = forceDiscriminator;
	}

	public void setInsertableDiscriminator(boolean insertableDiscriminator) {
		this.insertableDiscriminator = insertableDiscriminator;
	}

	public void bindEntity() {
		persistentClass.setAbstract( annotatedClass.isAbstract() );
		persistentClass.setClassName( annotatedClass.getName() );
		persistentClass.setJpaEntityName(name);
		//persistentClass.setDynamic(false); //no longer needed with the Entity name refactoring?
		persistentClass.setEntityName( annotatedClass.getName() );
		bindDiscriminatorValue();

		persistentClass.setLazy( lazy );
		if ( proxyClass != null ) {
			persistentClass.setProxyInterfaceName( proxyClass.getName() );
		}
		persistentClass.setDynamicInsert( dynamicInsert );
		persistentClass.setDynamicUpdate( dynamicUpdate );

		if ( persistentClass instanceof RootClass ) {
			RootClass rootClass = (RootClass) persistentClass;

			boolean mutable = !annotatedClass.isAnnotationPresent( Immutable.class );
			rootClass.setMutable( mutable );
			rootClass.setExplicitPolymorphism( isExplicitPolymorphism( polymorphismType ) );

			if ( isNotEmpty( where ) ) {
				rootClass.setWhere( where );
			}

			if ( cacheConcurrentStrategy != null ) {
				rootClass.setCacheConcurrencyStrategy( cacheConcurrentStrategy );
				rootClass.setCacheRegionName( cacheRegion );
				rootClass.setLazyPropertiesCacheable( cacheLazyProperty );
			}

			rootClass.setNaturalIdCacheRegionName( naturalIdCacheRegion );

			boolean forceDiscriminatorInSelects = forceDiscriminator == null
					? context.getBuildingOptions().shouldImplicitlyForceDiscriminatorInSelect()
					: forceDiscriminator;

			rootClass.setForceDiscriminator( forceDiscriminatorInSelects );

			if ( insertableDiscriminator != null ) {
				rootClass.setDiscriminatorInsertable( insertableDiscriminator );
			}
		}
		else {
			if ( annotatedClass.isAnnotationPresent(Immutable.class) ) {
				LOG.immutableAnnotationOnNonRoot( annotatedClass.getName() );
			}
		}

		persistentClass.setCached( isCached );

		persistentClass.setOptimisticLockStyle( getVersioning( optimisticLockType ) );
		persistentClass.setSelectBeforeUpdate( selectBeforeUpdate );

		bindCustomPersister();

		persistentClass.setBatchSize( batchSize );

		bindCustomSql();
		bindSynchronize();
		bindhandleFilters();

		LOG.debugf( "Import with entity name %s", name );
		try {
			context.getMetadataCollector().addImport( name, persistentClass.getEntityName() );
			String entityName = persistentClass.getEntityName();
			if ( !entityName.equals( name ) ) {
				context.getMetadataCollector().addImport( entityName, entityName );
			}
		}
		catch (MappingException me) {
			throw new AnnotationException( "Use of the same entity name twice: " + name, me );
		}

		processNamedEntityGraphs();
	}

	private void bindCustomSql() {
		//SQL overriding
		SQLInsert sqlInsert = annotatedClass.getAnnotation( SQLInsert.class );
		if ( sqlInsert != null ) {
			persistentClass.setCustomSQLInsert(
					sqlInsert.sql().trim(),
					sqlInsert.callable(),
					fromExternalName( sqlInsert.check().toString().toLowerCase(Locale.ROOT) )
			);

		}

		SQLUpdate sqlUpdate = annotatedClass.getAnnotation( SQLUpdate.class );
		if ( sqlUpdate != null ) {
			persistentClass.setCustomSQLUpdate(
					sqlUpdate.sql().trim(),
					sqlUpdate.callable(),
					fromExternalName( sqlUpdate.check().toString().toLowerCase(Locale.ROOT) )
			);
		}

		SQLDelete sqlDelete = annotatedClass.getAnnotation( SQLDelete.class );
		if ( sqlDelete != null ) {
			persistentClass.setCustomSQLDelete(
					sqlDelete.sql().trim(),
					sqlDelete.callable(),
					fromExternalName( sqlDelete.check().toString().toLowerCase(Locale.ROOT) )
			);
		}

		SQLDeleteAll sqlDeleteAll = annotatedClass.getAnnotation( SQLDeleteAll.class );
		if ( sqlDeleteAll != null ) {
			persistentClass.setCustomSQLDelete(
					sqlDeleteAll.sql().trim(),
					sqlDeleteAll.callable(),
					fromExternalName( sqlDeleteAll.check().toString().toLowerCase(Locale.ROOT) )
			);
		}

		Loader loader = annotatedClass.getAnnotation( Loader.class );
		if ( loader != null ) {
			persistentClass.setLoaderName( loader.namedQuery() );
		}

		Subselect subselect = annotatedClass.getAnnotation( Subselect.class );
		if ( subselect != null ) {
			this.subselect = subselect.value();
		}
	}

	private void bindhandleFilters() {
		for ( Filter filter : filters ) {
			final String filterName = filter.name();
			String condition = filter.condition();
			if ( isEmptyAnnotationValue( condition ) ) {
				final FilterDefinition definition = context.getMetadataCollector().getFilterDefinition( filterName );
				condition = definition == null ? null : definition.getDefaultFilterCondition();
				if ( isEmpty( condition ) ) {
					throw new AnnotationException( "no filter condition found for filter "
							+ filterName + " in " + this.name );
				}
			}
			persistentClass.addFilter(
					filterName,
					condition,
					filter.deduceAliasInjectionPoints(),
					toAliasTableMap( filter.aliases() ),
					toAliasEntityMap( filter.aliases() )
			);
		}
	}

	private void bindSynchronize() {
		if ( annotatedClass.isAnnotationPresent( Synchronize.class ) ) {
			final JdbcEnvironment jdbcEnvironment = context.getMetadataCollector().getDatabase().getJdbcEnvironment();
			for ( String table : annotatedClass.getAnnotation(Synchronize.class).value() ) {
				persistentClass.addSynchronizedTable(
						context.getBuildingOptions().getPhysicalNamingStrategy().toPhysicalTableName(
								jdbcEnvironment.getIdentifierHelper().toIdentifier( table ),
								jdbcEnvironment
						).render( jdbcEnvironment.getDialect() )
				);
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void bindCustomPersister() {
		//set persister if needed
		Persister persisterAnn = annotatedClass.getAnnotation( Persister.class );
		if ( persisterAnn != null ) {
			Class clazz = persisterAnn.impl();
			if ( !EntityPersister.class.isAssignableFrom(clazz) ) {
				throw new AnnotationException( "persister class does not implement EntityPersister: " + clazz.getName() );
			}
			persistentClass.setEntityPersisterClass( clazz );
		}
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	private void processNamedEntityGraphs() {
		processNamedEntityGraph( annotatedClass.getAnnotation( NamedEntityGraph.class ) );
		final NamedEntityGraphs graphs = annotatedClass.getAnnotation( NamedEntityGraphs.class );
		if ( graphs != null ) {
			for ( NamedEntityGraph graph : graphs.value() ) {
				processNamedEntityGraph( graph );
			}
		}
	}

	private void processNamedEntityGraph(NamedEntityGraph annotation) {
		if ( annotation == null ) {
			return;
		}
		context.getMetadataCollector().addNamedEntityGraph(
				new NamedEntityGraphDefinition( annotation, name, persistentClass.getEntityName() )
		);
	}

	public void bindDiscriminatorValue() {
		if ( isEmpty( discriminatorValue ) ) {
			Value discriminator = persistentClass.getDiscriminator();
			if ( discriminator == null ) {
				persistentClass.setDiscriminatorValue( name );
			}
			else if ( "character".equals( discriminator.getType().getName() ) ) {
				throw new AnnotationException(
						"Using default @DiscriminatorValue for a discriminator of type CHAR is not safe"
				);
			}
			else if ( "integer".equals( discriminator.getType().getName() ) ) {
				persistentClass.setDiscriminatorValue( String.valueOf( name.hashCode() ) );
			}
			else {
				persistentClass.setDiscriminatorValue( name ); //Spec compliant
			}
		}
		else {
			//persistentClass.getDiscriminator()
			persistentClass.setDiscriminatorValue( discriminatorValue );
		}
	}

	OptimisticLockStyle getVersioning(OptimisticLockType type) {
		switch ( type ) {
			case VERSION:
				return OptimisticLockStyle.VERSION;
			case NONE:
				return OptimisticLockStyle.NONE;
			case DIRTY:
				return OptimisticLockStyle.DIRTY;
			case ALL:
				return OptimisticLockStyle.ALL;
			default:
				throw new AssertionFailure( "optimistic locking not supported: " + type );
		}
	}

	private boolean isExplicitPolymorphism(PolymorphismType type) {
		switch ( type ) {
			case IMPLICIT:
				return false;
			case EXPLICIT:
				return true;
			default:
				throw new AssertionFailure( "Unknown polymorphism type: " + type );
		}
	}

	public void setBatchSize(BatchSize sizeAnn) {
		if ( sizeAnn != null ) {
			batchSize = sizeAnn.size();
		}
		else {
			batchSize = -1;
		}
	}

	public void setProxy(Proxy proxy) {
		if ( proxy != null ) {
			lazy = proxy.lazy();
			if ( !lazy ) {
				proxyClass = null;
			}
			else {
				final ReflectionManager reflectionManager = context.getBootstrapContext().getReflectionManager();
				if ( AnnotationBinder.isDefault( reflectionManager.toXClass( proxy.proxyClass() ), context ) ) {
					proxyClass = annotatedClass;
				}
				else {
					proxyClass = reflectionManager.toXClass( proxy.proxyClass() );
				}
			}
		}
		else {
			lazy = true; //needed to allow association lazy loading.
			proxyClass = annotatedClass;
		}
	}

	public void setWhere(Where whereAnn) {
		if ( whereAnn != null ) {
			where = whereAnn.clause();
		}
	}

	public void setWrapIdsInEmbeddedComponents(boolean wrapIdsInEmbeddedComponents) {
		this.wrapIdsInEmbeddedComponents = wrapIdsInEmbeddedComponents;
	}

	public void applyCaching(
			XClass clazzToProcess,
			SharedCacheMode sharedCacheMode,
			MetadataBuildingContext context) {
		bindCache( clazzToProcess, sharedCacheMode, context );
		bindNaturalIdCache( clazzToProcess );
	}

	private void bindNaturalIdCache(XClass clazzToProcess) {
		naturalIdCacheRegion = null;
		final NaturalIdCache naturalIdCacheAnn = clazzToProcess.getAnnotation( NaturalIdCache.class );
		if ( naturalIdCacheAnn != null ) {
			if ( isEmptyAnnotationValue( naturalIdCacheAnn.region() ) ) {
				final Cache explicitCacheAnn = clazzToProcess.getAnnotation( Cache.class );
				naturalIdCacheRegion = explicitCacheAnn != null && isNotEmpty( explicitCacheAnn.region() )
						? explicitCacheAnn.region() + NATURAL_ID_CACHE_SUFFIX
						: clazzToProcess.getName() + NATURAL_ID_CACHE_SUFFIX;
			}
			else {
				naturalIdCacheRegion = naturalIdCacheAnn.region();
			}
		}
	}

	private void bindCache(XClass clazzToProcess, SharedCacheMode sharedCacheMode, MetadataBuildingContext context) {
		isCached = false;
		cacheConcurrentStrategy = null;
		cacheRegion = null;
		cacheLazyProperty = true;
		if ( persistentClass instanceof RootClass ) {
			bindRootClassCache( clazzToProcess, sharedCacheMode, context );
		}
		else {
			bindSubclassCache( clazzToProcess, sharedCacheMode );
		}
	}

	private void bindSubclassCache(XClass clazzToProcess, SharedCacheMode sharedCacheMode) {
		final Cache cache = clazzToProcess.getAnnotation( Cache.class );
		final Cacheable cacheable = clazzToProcess.getAnnotation( Cacheable.class );
		if ( cache != null ) {
			LOG.cacheOrCacheableAnnotationOnNonRoot(
					persistentClass.getClassName() == null
							? annotatedClass.getName()
							: persistentClass.getClassName()
			);
		}
		else if ( cacheable == null && persistentClass.getSuperclass() != null ) {
			// we should inherit our super's caching config
			isCached = persistentClass.getSuperclass().isCached();
		}
		else {
			isCached = isCacheable( sharedCacheMode, cacheable );
		}
	}

	private void bindRootClassCache(XClass clazzToProcess, SharedCacheMode sharedCacheMode, MetadataBuildingContext context) {
		final Cache cache = clazzToProcess.getAnnotation( Cache.class );
		final Cacheable cacheable = clazzToProcess.getAnnotation( Cacheable.class );
		final Cache effectiveCache;
		if ( cache != null ) {
			// preserve legacy behavior of circumventing SharedCacheMode when Hibernate's @Cache is used.
			isCached = true;
			effectiveCache = cache;
		}
		else {
			effectiveCache = buildCacheMock( clazzToProcess.getName(), context );
			isCached = isCacheable( sharedCacheMode, cacheable );
		}
		cacheConcurrentStrategy = resolveCacheConcurrencyStrategy( effectiveCache.usage() );
		cacheRegion = effectiveCache.region();
		cacheLazyProperty = isCacheLazy( effectiveCache, annotatedClass );
	}

	private static boolean isCacheLazy(Cache effectiveCache, XClass annotatedClass) {
		switch ( effectiveCache.include().toLowerCase( Locale.ROOT ) ) {
			case "all":
				return true;
			case "non-lazy":
				return false;
			default:
				throw new AnnotationException( "Unknown @Cache.include value [" + effectiveCache.include() + "] : "
						+ annotatedClass.getName()
				);
		}
	}

	private static boolean isCacheable(SharedCacheMode sharedCacheMode, Cacheable explicitCacheableAnn) {
		switch (sharedCacheMode) {
			case ALL:
				// all entities should be cached
				return true;
			case ENABLE_SELECTIVE:
				// only entities with @Cacheable(true) should be cached
				return explicitCacheableAnn != null && explicitCacheableAnn.value();
			case DISABLE_SELECTIVE:
				// only entities with @Cacheable(false) should not be cached
				return explicitCacheableAnn == null || explicitCacheableAnn.value();
			default:
				// treat both NONE and UNSPECIFIED the same
				return false;
		}
	}

	private static String resolveCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		final org.hibernate.cache.spi.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	private static Cache buildCacheMock(String region, MetadataBuildingContext context) {
		return new LocalCacheAnnotationStub( region, determineCacheConcurrencyStrategy( context ) );
	}

	@SuppressWarnings("ClassExplicitlyAnnotation")
	private static class LocalCacheAnnotationStub implements Cache {
		private final String region;
		private final CacheConcurrencyStrategy usage;

		private LocalCacheAnnotationStub(String region, CacheConcurrencyStrategy usage) {
			this.region = region;
			this.usage = usage;
		}

		public CacheConcurrencyStrategy usage() {
			return usage;
		}

		public String region() {
			return region;
		}

		public String include() {
			return "all";
		}

		public Class<? extends Annotation> annotationType() {
			return Cache.class;
		}
	}

	private static CacheConcurrencyStrategy determineCacheConcurrencyStrategy(MetadataBuildingContext context) {
		return CacheConcurrencyStrategy.fromAccessType( context.getBuildingOptions().getImplicitCacheAccessType() );
	}

	private static class EntityTableNamingStrategyHelper implements NamingStrategyHelper {
		private final String className;
		private final String entityName;
		private final String jpaEntityName;

		private EntityTableNamingStrategyHelper(String className, String entityName, String jpaEntityName) {
			this.className = className;
			this.entityName = entityName;
			this.jpaEntityName = jpaEntityName;
		}

		public Identifier determineImplicitName(final MetadataBuildingContext buildingContext) {
			return buildingContext.getBuildingOptions().getImplicitNamingStrategy().determinePrimaryTableName(
					new ImplicitEntityNameSource() {
						private final EntityNaming entityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return className;
							}

							@Override
							public String getEntityName() {
								return entityName;
							}

							@Override
							public String getJpaEntityName() {
								return jpaEntityName;
							}
						};

						@Override
						public EntityNaming getEntityNaming() {
							return entityNaming;
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return buildingContext;
						}
					}
			);
		}

		@Override
		public Identifier handleExplicitName(String explicitName, MetadataBuildingContext buildingContext) {
			return buildingContext.getMetadataCollector()
					.getDatabase()
					.getJdbcEnvironment()
					.getIdentifierHelper()
					.toIdentifier( explicitName );
		}

		@Override
		public Identifier toPhysicalName(Identifier logicalName, MetadataBuildingContext buildingContext) {
			return buildingContext.getBuildingOptions().getPhysicalNamingStrategy().toPhysicalTableName(
					logicalName,
					buildingContext.getMetadataCollector().getDatabase().getJdbcEnvironment()
			);
		}
	}

	public void bindTableForDiscriminatedSubclass(InFlightMetadataCollector.EntityTableXref superTableXref) {
		if ( !(persistentClass instanceof SingleTableSubclass) ) {
			throw new AssertionFailure(
					"Was expecting a discriminated subclass [" + SingleTableSubclass.class.getName() +
							"] but found [" + persistentClass.getClass().getName() + "] for entity [" +
							persistentClass.getEntityName() + "]"
			);
		}

		context.getMetadataCollector().addEntityTableXref(
				persistentClass.getEntityName(),
				context.getMetadataCollector().getDatabase().toIdentifier(
						context.getMetadataCollector().getLogicalTableName( superTableXref.getPrimaryTable() )
				),
				superTableXref.getPrimaryTable(),
				superTableXref
		);
	}

	public void bindTable(
			String schema,
			String catalog,
			String tableName,
			List<UniqueConstraintHolder> uniqueConstraints,
			String constraints,
			InFlightMetadataCollector.EntityTableXref denormalizedSuperTableXref) {

		final EntityTableNamingStrategyHelper namingStrategyHelper = new EntityTableNamingStrategyHelper(
				persistentClass.getClassName(),
				persistentClass.getEntityName(),
				name
		);
		final Identifier logicalName = isNotEmpty( tableName )
				? namingStrategyHelper.handleExplicitName( tableName, context )
				: namingStrategyHelper.determineImplicitName( context );

		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				logicalName,
				persistentClass.isAbstract(),
				uniqueConstraints,
				null,
				constraints,
				context,
				subselect,
				denormalizedSuperTableXref
		);
		final RowId rowId = annotatedClass.getAnnotation( RowId.class );
		if ( rowId != null ) {
			table.setRowId( rowId.value() );
		}
		final Comment comment = annotatedClass.getAnnotation( Comment.class );
		if ( comment != null ) {
			table.setComment( comment.value() );
		}

		context.getMetadataCollector().addEntityTableXref(
				persistentClass.getEntityName(),
				logicalName,
				table,
				denormalizedSuperTableXref
		);

		if ( persistentClass instanceof TableOwner ) {
			LOG.debugf( "Bind entity %s on table %s", persistentClass.getEntityName(), table.getName() );
			( (TableOwner) persistentClass ).setTable( table );
		}
		else {
			throw new AssertionFailure( "binding a table for a subclass" );
		}
	}

	public void finalSecondaryTableBinding(PropertyHolder propertyHolder) {
		 // This operation has to be done after the id definition of the persistence class.
		 // ie after the properties parsing
		Iterator<Object> joinColumns = secondaryTableJoins.values().iterator();
		for ( Map.Entry<String, Join> entrySet : secondaryTables.entrySet() ) {
			if ( !secondaryTablesFromAnnotation.containsKey( entrySet.getKey() ) ) {
				createPrimaryColumnsToSecondaryTable( joinColumns.next(), propertyHolder, entrySet.getValue() );
			}
		}
	}

	public void finalSecondaryTableFromAnnotationBinding(PropertyHolder propertyHolder) {
		 // This operation has to be done before the end of the FK second pass processing in order
		 // to find the join columns belonging to secondary tables
		Iterator<Object> joinColumns = secondaryTableFromAnnotationJoins.values().iterator();
		for ( Map.Entry<String, Join> entrySet : secondaryTables.entrySet() ) {
			if ( secondaryTablesFromAnnotation.containsKey( entrySet.getKey() ) ) {
				createPrimaryColumnsToSecondaryTable( joinColumns.next(), propertyHolder, entrySet.getValue() );
			}
		}
	}

	private void createPrimaryColumnsToSecondaryTable(Object column, PropertyHolder propertyHolder, Join join) {
		final AnnotatedJoinColumn[] annotatedJoinColumns;
		final PrimaryKeyJoinColumn[] pkColumnsAnn = column instanceof PrimaryKeyJoinColumn[]
				? (PrimaryKeyJoinColumn[]) column
				: null;
		final JoinColumn[] joinColumnsAnn = column instanceof JoinColumn[]
				? (JoinColumn[]) column
				: null;
		annotatedJoinColumns = pkColumnsAnn == null && joinColumnsAnn == null
				? createDefaultJoinColumn( propertyHolder )
				: createJoinColumns( propertyHolder, pkColumnsAnn, joinColumnsAnn );

		for (AnnotatedJoinColumn joinColumn : annotatedJoinColumns) {
			joinColumn.forceNotNull();
		}
		bindJoinToPersistentClass( join, annotatedJoinColumns, context );
	}

	private AnnotatedJoinColumn[] createDefaultJoinColumn(PropertyHolder propertyHolder) {
		final AnnotatedJoinColumn[] annotatedJoinColumns = new AnnotatedJoinColumn[1];
		annotatedJoinColumns[0] = buildJoinColumn(
				null,
				null,
				persistentClass.getIdentifier(),
				secondaryTables,
				propertyHolder,
				context
		);
		return annotatedJoinColumns;
	}

	private AnnotatedJoinColumn[] createJoinColumns(
			PropertyHolder propertyHolder,
			PrimaryKeyJoinColumn[] pkColumnsAnn,
			JoinColumn[] joinColumnsAnn) {
		final int joinColumnCount = pkColumnsAnn != null ? pkColumnsAnn.length : joinColumnsAnn.length;
		if ( joinColumnCount == 0 ) {
			return createDefaultJoinColumn( propertyHolder );
		}
		else {
			final AnnotatedJoinColumn[] annotatedJoinColumns = new AnnotatedJoinColumn[joinColumnCount];
			for (int colIndex = 0; colIndex < joinColumnCount; colIndex++) {
				PrimaryKeyJoinColumn pkJoinAnn = pkColumnsAnn != null ? pkColumnsAnn[colIndex] : null;
				JoinColumn joinAnn = joinColumnsAnn != null ? joinColumnsAnn[colIndex] : null;
				annotatedJoinColumns[colIndex] = buildJoinColumn(
						pkJoinAnn,
						joinAnn,
						persistentClass.getIdentifier(),
						secondaryTables,
						propertyHolder,
						context
				);
			}
			return annotatedJoinColumns;
		}
	}

	private void bindJoinToPersistentClass(Join join, AnnotatedJoinColumn[] annotatedJoinColumns, MetadataBuildingContext buildingContext) {
		DependantValue key = new DependantValue( buildingContext, join.getTable(), persistentClass.getIdentifier() );
		join.setKey( key );
		setFKNameIfDefined( join );
		key.setCascadeDeleteEnabled( false );
		TableBinder.bindFk( persistentClass, null, annotatedJoinColumns, key, false, buildingContext );
		key.sortProperties();
		join.createPrimaryKey();
		join.createForeignKey();
		persistentClass.addJoin( join );
	}

	private void setFKNameIfDefined(Join join) {
		// just awful..
		org.hibernate.annotations.Table matchingTable = findMatchingComplementaryTableAnnotation( join );
		final SimpleValue key = (SimpleValue) join.getKey();
		if ( matchingTable != null && !isEmptyAnnotationValue( matchingTable.foreignKey().name() ) ) {
			key.setForeignKeyName( matchingTable.foreignKey().name() );
		}
		else {
			SecondaryTable jpaSecondaryTable = findMatchingSecondaryTable( join );
			if ( jpaSecondaryTable != null ) {
				final boolean noConstraintByDefault = context.getBuildingOptions().isNoConstraintByDefault();
				if ( jpaSecondaryTable.foreignKey().value() == ConstraintMode.NO_CONSTRAINT
						|| jpaSecondaryTable.foreignKey().value() == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) {
					key.disableForeignKey();
				}
				else {
					key.setForeignKeyName( nullIfEmpty( jpaSecondaryTable.foreignKey().name() ) );
					key.setForeignKeyDefinition( nullIfEmpty( jpaSecondaryTable.foreignKey().foreignKeyDefinition() ) );
				}
			}
		}
	}

	private SecondaryTable findMatchingSecondaryTable(Join join) {
		final String nameToMatch = join.getTable().getQuotedName();
		SecondaryTable secondaryTable = annotatedClass.getAnnotation( SecondaryTable.class );
		if ( secondaryTable != null && nameToMatch.equals( secondaryTable.name() ) ) {
			return secondaryTable;
		}
		SecondaryTables secondaryTables = annotatedClass.getAnnotation( SecondaryTables.class );
		if ( secondaryTables != null ) {
			for ( SecondaryTable secondaryTablesEntry : secondaryTables.value() ) {
				if ( secondaryTablesEntry != null && nameToMatch.equals( secondaryTablesEntry.name() ) ) {
					return secondaryTablesEntry;
				}
			}
		}
		return null;
	}

	private org.hibernate.annotations.Table findMatchingComplementaryTableAnnotation(Join join) {
		String tableName = join.getTable().getQuotedName();
		org.hibernate.annotations.Table table = annotatedClass.getAnnotation( org.hibernate.annotations.Table.class );
		org.hibernate.annotations.Table matchingTable = null;
		if ( table != null && tableName.equals( table.appliesTo() ) ) {
			matchingTable = table;
		}
		else {
			Tables tables = annotatedClass.getAnnotation( Tables.class );
			if ( tables != null ) {
				for (org.hibernate.annotations.Table current : tables.value()) {
					if ( tableName.equals( current.appliesTo() ) ) {
						matchingTable = current;
						break;
					}
				}
			}
		}
		return matchingTable;
	}

	//Used for @*ToMany @JoinTable
	public Join addJoin(JoinTable joinTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		return addJoin( null, joinTable, holder, noDelayInPkColumnCreation );
	}

	public Join addJoin(SecondaryTable secondaryTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		return addJoin( secondaryTable, null, holder, noDelayInPkColumnCreation );
	}

	private Join addJoin(
			SecondaryTable secondaryTable,
			JoinTable joinTable,
			PropertyHolder propertyHolder,
			boolean noDelayInPkColumnCreation) {
		// A non-null propertyHolder means than we process the Pk creation without delay
		Join join = new Join();
		join.setPersistentClass( persistentClass );

		final String schema;
		final String catalog;
		final Object joinColumns;
		final List<UniqueConstraintHolder> uniqueConstraintHolders;

		final QualifiedTableName logicalName;
		if ( secondaryTable != null ) {
			schema = secondaryTable.schema();
			catalog = secondaryTable.catalog();
			logicalName = new QualifiedTableName(
				Identifier.toIdentifier( catalog ),
				Identifier.toIdentifier( schema ),
					context.getMetadataCollector()
					.getDatabase()
					.getJdbcEnvironment()
					.getIdentifierHelper()
					.toIdentifier( secondaryTable.name() )
			);
			joinColumns = secondaryTable.pkJoinColumns();
			uniqueConstraintHolders = TableBinder.buildUniqueConstraintHolders( secondaryTable.uniqueConstraints() );
		}
		else if ( joinTable != null ) {
			schema = joinTable.schema();
			catalog = joinTable.catalog();
			logicalName = new QualifiedTableName(
				Identifier.toIdentifier( catalog ),
				Identifier.toIdentifier( schema ),
				context.getMetadataCollector()
						.getDatabase()
						.getJdbcEnvironment()
						.getIdentifierHelper()
						.toIdentifier( joinTable.name() )
			);
			joinColumns = joinTable.joinColumns();
			uniqueConstraintHolders = TableBinder.buildUniqueConstraintHolders( joinTable.uniqueConstraints() );
		}
		else {
			throw new AssertionFailure( "Both JoinTable and SecondaryTable are null" );
		}

		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				logicalName.getTableName(),
				false,
				uniqueConstraintHolders,
				null,
				null,
				context,
				null,
				null
		);

		final InFlightMetadataCollector.EntityTableXref tableXref
				= context.getMetadataCollector().getEntityTableXref( persistentClass.getEntityName() );
		assert tableXref != null : "Could not locate EntityTableXref for entity [" + persistentClass.getEntityName() + "]";
		tableXref.addSecondaryTable( logicalName, join );

		if ( secondaryTable != null ) {
			TableBinder.addIndexes( table, secondaryTable.indexes(), context );
		}

			//no check constraints available on joins
		join.setTable( table );

		//somehow keep joins() for later.
		//Has to do the work later because it needs persistentClass id!
		LOG.debugf( "Adding secondary table to entity %s -> %s",
				persistentClass.getEntityName(), join.getTable().getName() );
		org.hibernate.annotations.Table matchingTable = findMatchingComplementaryTableAnnotation( join );
		if ( matchingTable != null ) {
			join.setSequentialSelect( FetchMode.JOIN != matchingTable.fetch() );
			join.setInverse( matchingTable.inverse() );
			join.setOptional( matchingTable.optional() );
			String insertSql = matchingTable.sqlInsert().sql();
			if ( !isEmptyAnnotationValue(insertSql) ) {
				join.setCustomSQLInsert(
						insertSql.trim(),
						matchingTable.sqlInsert().callable(),
						fromExternalName( matchingTable.sqlInsert().check().toString().toLowerCase(Locale.ROOT) )
				);
			}
			String updateSql = matchingTable.sqlUpdate().sql();
			if ( !isEmptyAnnotationValue(updateSql) ) {
				join.setCustomSQLUpdate(
						updateSql.trim(),
						matchingTable.sqlUpdate().callable(),
						fromExternalName( matchingTable.sqlUpdate().check().toString().toLowerCase(Locale.ROOT) )
				);
			}
			String deleteSql = matchingTable.sqlDelete().sql();
			if ( !isEmptyAnnotationValue(deleteSql) ) {
				join.setCustomSQLDelete(
						deleteSql.trim(),
						matchingTable.sqlDelete().callable(),
						fromExternalName( matchingTable.sqlDelete().check().toString().toLowerCase(Locale.ROOT) )
				);
			}
		}
		else {
			//default
			join.setSequentialSelect( false );
			join.setInverse( false );
			join.setOptional( true ); //perhaps not quite per-spec, but a Good Thing anyway
		}

		if ( noDelayInPkColumnCreation ) {
			createPrimaryColumnsToSecondaryTable( joinColumns, propertyHolder, join );
		}
		else {
			final String quotedName = table.getQuotedName();
			if ( secondaryTable != null ) {
				secondaryTablesFromAnnotation.put( quotedName, join );
				secondaryTableFromAnnotationJoins.put( quotedName, joinColumns );
			}
			else {
				secondaryTableJoins.put( quotedName, joinColumns );
			}
			secondaryTables.put( quotedName, join );
		}

		return join;
	}

	public java.util.Map<String, Join> getSecondaryTables() {
		return secondaryTables;
	}

	public static String getCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		org.hibernate.cache.spi.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	public void addFilter(Filter filter) {
		filters.add(filter);
	}

	public boolean isIgnoreIdAnnotations() {
		return ignoreIdAnnotations;
	}

	public void setIgnoreIdAnnotations(boolean ignoreIdAnnotations) {
		this.ignoreIdAnnotations = ignoreIdAnnotations;
	}

	public void processComplementaryTableDefinitions(jakarta.persistence.Table table) {
		if ( table != null ) {
			TableBinder.addIndexes( persistentClass.getTable(), table.indexes(), context );
		}
	}

	public void processComplementaryTableDefinitions(org.hibernate.annotations.Table table) {
		//comment and index are processed here
		if ( table == null ) return;
		String appliedTable = table.appliesTo();
		Table hibTable = null;
		for ( Table pcTable : persistentClass.getTableClosure() ) {
			if ( pcTable.getQuotedName().equals( appliedTable ) ) {
				//we are in the correct table to find columns
				hibTable = pcTable;
				break;
			}
		}
		if ( hibTable == null ) {
			//maybe a join/secondary table
			for ( Join join : secondaryTables.values() ) {
				if ( join.getTable().getQuotedName().equals( appliedTable ) ) {
					hibTable = join.getTable();
					break;
				}
			}
		}
		if ( hibTable == null ) {
			throw new AnnotationException(
					"@org.hibernate.annotations.Table references an unknown table: " + appliedTable
			);
		}
		if ( !isEmptyAnnotationValue( table.comment() ) ) {
			hibTable.setComment( table.comment() );
		}
		if ( !isEmptyAnnotationValue( table.checkConstraint() ) ) {
			hibTable.addCheckConstraint( table.checkConstraint() );
		}
		TableBinder.addIndexes( hibTable, table.indexes(), context );
	}

	public void processComplementaryTableDefinitions(Tables tables) {
		if ( tables == null ) return;
		for (org.hibernate.annotations.Table table : tables.value()) {
			processComplementaryTableDefinitions( table );
		}
	}

	public AccessType getPropertyAccessType() {
		return propertyAccessType;
	}

	public void setPropertyAccessType(AccessType propertyAccessor) {
		this.propertyAccessType = getExplicitAccessType( annotatedClass );
		// only set the access type if there is no explicit access type for this class
		if( this.propertyAccessType == null ) {
			this.propertyAccessType = propertyAccessor;
		}
	}

	public AccessType getPropertyAccessor(XAnnotatedElement element) {
		AccessType accessType = getExplicitAccessType( element );
		if ( accessType == null ) {
			accessType = propertyAccessType;
		}
		return accessType;
	}

	public AccessType getExplicitAccessType(XAnnotatedElement element) {
		AccessType accessType = null;
		Access access = element.getAnnotation( Access.class );
		if ( access != null ) {
			accessType = AccessType.getAccessStrategy( access.value() );
		}
		return accessType;
	}
}
