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
import javax.persistence.Access;
import javax.persistence.Cacheable;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.SharedCacheMode;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
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
import org.hibernate.annotations.Tuplizer;
import org.hibernate.annotations.Tuplizers;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.NamingStrategyHelper;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.ObjectNameSource;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.Value;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.BinderHelper.toAliasEntityMap;
import static org.hibernate.cfg.BinderHelper.toAliasTableMap;


/**
 * Stateful holder and processor for binding Entity information
 *
 * @author Emmanuel Bernard
 */
public class EntityBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, EntityBinder.class.getName());
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
	private boolean explicitHibernateEntityAnnotation;
	private OptimisticLockType optimisticLockType;
	private PolymorphismType polymorphismType;
	private boolean selectBeforeUpdate;
	private int batchSize;
	private boolean lazy;
	private XClass proxyClass;
	private String where;
	// todo : we should defer to InFlightMetadataCollector.EntityTableXref for secondary table tracking;
	//		atm we use both from here; HBM binding solely uses InFlightMetadataCollector.EntityTableXref
	private java.util.Map<String, Join> secondaryTables = new HashMap<>();
	private java.util.Map<String, Object> secondaryTableJoins = new HashMap<>();
	private List<Filter> filters = new ArrayList<>();
	private InheritanceState inheritanceState;
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
			Entity ejb3Ann,
			org.hibernate.annotations.Entity hibAnn,
			XClass annotatedClass,
			PersistentClass persistentClass,
			MetadataBuildingContext context) {
		this.context = context;
		this.persistentClass = persistentClass;
		this.annotatedClass = annotatedClass;
		bindEjb3Annotation( ejb3Ann );
		bindHibernateAnnotation( hibAnn );
	}

	/**
	 * For the most part, this is a simple delegation to {@link PersistentClass#isPropertyDefinedInHierarchy},
	 * after verifying that PersistentClass is indeed set here.
	 *
	 * @param name The name of the property to check
	 *
	 * @return {@code true} if a property by that given name does already exist in the super hierarchy.
	 */
	@SuppressWarnings("SimplifiableIfStatement")
	public boolean isPropertyDefinedInSuperHierarchy(String name) {
		// Yes, yes... persistentClass can be null because EntityBinder can be used
		// to bind components as well, of course...
		if ( persistentClass == null ) {
			return false;
		}
		return persistentClass.isPropertyDefinedInSuperHierarchy( name );
	}

	@SuppressWarnings("SimplifiableConditionalExpression")
	private void bindHibernateAnnotation(org.hibernate.annotations.Entity hibAnn) {
		{
			final DynamicInsert dynamicInsertAnn = annotatedClass.getAnnotation( DynamicInsert.class );
			this.dynamicInsert = dynamicInsertAnn == null
					? ( hibAnn == null ? false : hibAnn.dynamicInsert() )
					: dynamicInsertAnn.value();
		}

		{
			final DynamicUpdate dynamicUpdateAnn = annotatedClass.getAnnotation( DynamicUpdate.class );
			this.dynamicUpdate = dynamicUpdateAnn == null
					? ( hibAnn == null ? false : hibAnn.dynamicUpdate() )
					: dynamicUpdateAnn.value();
		}

		{
			final SelectBeforeUpdate selectBeforeUpdateAnn = annotatedClass.getAnnotation( SelectBeforeUpdate.class );
			this.selectBeforeUpdate = selectBeforeUpdateAnn == null
					? ( hibAnn == null ? false : hibAnn.selectBeforeUpdate() )
					: selectBeforeUpdateAnn.value();
		}

		{
			final OptimisticLocking optimisticLockingAnn = annotatedClass.getAnnotation( OptimisticLocking.class );
			this.optimisticLockType = optimisticLockingAnn == null
					? ( hibAnn == null ? OptimisticLockType.VERSION : hibAnn.optimisticLock() )
					: optimisticLockingAnn.type();
		}

		{
			final Polymorphism polymorphismAnn = annotatedClass.getAnnotation( Polymorphism.class );
			this.polymorphismType = polymorphismAnn == null
					? ( hibAnn == null ? PolymorphismType.IMPLICIT : hibAnn.polymorphism() )
					: polymorphismAnn.type();
		}

		if ( hibAnn != null ) {
			// used later in bind for logging
			explicitHibernateEntityAnnotation = true;
			//persister handled in bind
		}
	}

	private void bindEjb3Annotation(Entity ejb3Ann) {
		if ( ejb3Ann == null ) throw new AssertionFailure( "@Entity should always be not null" );
		if ( BinderHelper.isEmptyAnnotationValue( ejb3Ann.name() ) ) {
			name = StringHelper.unqualify( annotatedClass.getName() );
		}
		else {
			name = ejb3Ann.name();
		}
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
			boolean mutable = true;
			//priority on @Immutable, then @Entity.mutable()
			if ( annotatedClass.isAnnotationPresent( Immutable.class ) ) {
				mutable = false;
			}
			else {
				org.hibernate.annotations.Entity entityAnn =
						annotatedClass.getAnnotation( org.hibernate.annotations.Entity.class );
				if ( entityAnn != null ) {
					mutable = entityAnn.mutable();
				}
			}
			rootClass.setMutable( mutable );
			rootClass.setExplicitPolymorphism( isExplicitPolymorphism( polymorphismType ) );

			if ( StringHelper.isNotEmpty( where ) ) {
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
			if (explicitHibernateEntityAnnotation) {
				LOG.entityAnnotationOnNonRoot(annotatedClass.getName());
			}
			if (annotatedClass.isAnnotationPresent(Immutable.class)) {
				LOG.immutableAnnotationOnNonRoot(annotatedClass.getName());
			}
		}

		persistentClass.setCached( isCached );

		persistentClass.setOptimisticLockStyle( getVersioning( optimisticLockType ) );
		persistentClass.setSelectBeforeUpdate( selectBeforeUpdate );

		//set persister if needed
		Persister persisterAnn = annotatedClass.getAnnotation( Persister.class );
		Class persister = null;
		if ( persisterAnn != null ) {
			persister = persisterAnn.impl();
		}
		else {
			org.hibernate.annotations.Entity entityAnn = annotatedClass.getAnnotation( org.hibernate.annotations.Entity.class );
			if ( entityAnn != null && !BinderHelper.isEmptyAnnotationValue( entityAnn.persister() ) ) {
				try {
					persister = context.getBootstrapContext().getClassLoaderAccess().classForName( entityAnn.persister() );
				}
				catch (ClassLoadingException e) {
					throw new AnnotationException( "Could not find persister class: " + entityAnn.persister(), e );
				}
			}
		}
		if ( persister != null ) {
			persistentClass.setEntityPersisterClass( persister );
		}

		persistentClass.setBatchSize( batchSize );

		//SQL overriding
		SQLInsert sqlInsert = annotatedClass.getAnnotation( SQLInsert.class );
		SQLUpdate sqlUpdate = annotatedClass.getAnnotation( SQLUpdate.class );
		SQLDelete sqlDelete = annotatedClass.getAnnotation( SQLDelete.class );
		SQLDeleteAll sqlDeleteAll = annotatedClass.getAnnotation( SQLDeleteAll.class );
		Loader loader = annotatedClass.getAnnotation( Loader.class );

		if ( sqlInsert != null ) {
			persistentClass.setCustomSQLInsert( sqlInsert.sql().trim(), sqlInsert.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlInsert.check().toString().toLowerCase(Locale.ROOT) )
			);

		}
		if ( sqlUpdate != null ) {
			persistentClass.setCustomSQLUpdate( sqlUpdate.sql(), sqlUpdate.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlUpdate.check().toString().toLowerCase(Locale.ROOT) )
			);
		}
		if ( sqlDelete != null ) {
			persistentClass.setCustomSQLDelete( sqlDelete.sql(), sqlDelete.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlDelete.check().toString().toLowerCase(Locale.ROOT) )
			);
		}
		if ( sqlDeleteAll != null ) {
			persistentClass.setCustomSQLDelete( sqlDeleteAll.sql(), sqlDeleteAll.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlDeleteAll.check().toString().toLowerCase(Locale.ROOT) )
			);
		}
		if ( loader != null ) {
			persistentClass.setLoaderName( loader.namedQuery() );
		}

		final JdbcEnvironment jdbcEnvironment = context.getMetadataCollector().getDatabase().getJdbcEnvironment();
		if ( annotatedClass.isAnnotationPresent( Synchronize.class )) {
			Synchronize synchronizedWith = annotatedClass.getAnnotation(Synchronize.class);

			String [] tables = synchronizedWith.value();
			for (String table : tables) {
				persistentClass.addSynchronizedTable(
						context.getBuildingOptions().getPhysicalNamingStrategy().toPhysicalTableName(
								jdbcEnvironment.getIdentifierHelper().toIdentifier( table ),
								jdbcEnvironment
						).render( jdbcEnvironment.getDialect() )
				);
			}
		}

		if ( annotatedClass.isAnnotationPresent(Subselect.class )) {
			Subselect subselect = annotatedClass.getAnnotation(Subselect.class);
			this.subselect = subselect.value();
		}

		//tuplizers
		if ( annotatedClass.isAnnotationPresent( Tuplizers.class ) ) {
			for (Tuplizer tuplizer : annotatedClass.getAnnotation( Tuplizers.class ).value()) {
				EntityMode mode = EntityMode.parse( tuplizer.entityMode() );
				//todo tuplizer.entityModeType
				persistentClass.addTuplizer( mode, tuplizer.impl().getName() );
			}
		}
		if ( annotatedClass.isAnnotationPresent( Tuplizer.class ) ) {
			Tuplizer tuplizer = annotatedClass.getAnnotation( Tuplizer.class );
			EntityMode mode = EntityMode.parse( tuplizer.entityMode() );
			//todo tuplizer.entityModeType
			persistentClass.addTuplizer( mode, tuplizer.impl().getName() );
		}

		for ( Filter filter : filters ) {
			String filterName = filter.name();
			String cond = filter.condition();
			if ( BinderHelper.isEmptyAnnotationValue( cond ) ) {
				FilterDefinition definition = context.getMetadataCollector().getFilterDefinition( filterName );
				cond = definition == null ? null : definition.getDefaultFilterCondition();
				if ( StringHelper.isEmpty( cond ) ) {
					throw new AnnotationException(
							"no filter condition found for filter " + filterName + " in " + this.name
					);
				}
			}
			persistentClass.addFilter(filterName, cond, filter.deduceAliasInjectionPoints(),
					toAliasTableMap(filter.aliases()), toAliasEntityMap(filter.aliases()));
		}
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
		if ( StringHelper.isEmpty( discriminatorValue ) ) {
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

	@SuppressWarnings({ "unchecked" })
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
		final Cache explicitCacheAnn = clazzToProcess.getAnnotation( Cache.class );
		final Cacheable explicitCacheableAnn = clazzToProcess.getAnnotation( Cacheable.class );

		isCached = false;
		cacheConcurrentStrategy = null;
		cacheRegion = null;
		cacheLazyProperty = true;

		if ( persistentClass instanceof RootClass ) {
			Cache effectiveCacheAnn = explicitCacheAnn;

			if ( explicitCacheAnn != null ) {
				// preserve legacy behavior of circumventing SharedCacheMode when Hibernate's @Cache is used.
				isCached = true;
			}
			else {
				effectiveCacheAnn = buildCacheMock( clazzToProcess.getName(), context );

				switch ( sharedCacheMode ) {
					case ALL: {
						// all entities should be cached
						isCached = true;
						break;
					}
					case ENABLE_SELECTIVE: {
						if ( explicitCacheableAnn != null && explicitCacheableAnn.value() ) {
							isCached = true;
						}
						break;
					}
					case DISABLE_SELECTIVE: {
						if ( explicitCacheableAnn == null || explicitCacheableAnn.value() ) {
							isCached = true;
						}
						break;
					}
					default: {
						// treat both NONE and UNSPECIFIED the same
						isCached = false;
						break;
					}
				}
			}

			cacheConcurrentStrategy = resolveCacheConcurrencyStrategy( effectiveCacheAnn.usage() );
			cacheRegion = effectiveCacheAnn.region();
			switch ( effectiveCacheAnn.include().toLowerCase( Locale.ROOT ) ) {
				case "all": {
					cacheLazyProperty = true;
					break;
				}
				case "non-lazy": {
					cacheLazyProperty = false;
					break;
				}
				default: {
					throw new AnnotationException(
							"Unknown @Cache.include value [" + effectiveCacheAnn.include() + "] : "
									+ annotatedClass.getName()
					);
				}
			}
		}
		else {
			if ( explicitCacheAnn != null ) {
				LOG.cacheOrCacheableAnnotationOnNonRoot(
						persistentClass.getClassName() == null
								? annotatedClass.getName()
								: persistentClass.getClassName()
				);
			}
			else if ( explicitCacheableAnn == null && persistentClass.getSuperclass() != null ) {
				// we should inherit our super's caching config
				isCached = persistentClass.getSuperclass().isCached();
			}
			else {
				switch ( sharedCacheMode ) {
					case ALL: {
						// all entities should be cached
						isCached = true;
						break;
					}
					case ENABLE_SELECTIVE: {
						// only entities with @Cacheable(true) should be cached
						if ( explicitCacheableAnn != null && explicitCacheableAnn.value() ) {
							isCached = true;
						}
						break;
					}
					case DISABLE_SELECTIVE: {
						if ( explicitCacheableAnn == null || !explicitCacheableAnn.value() ) {
							isCached = true;
						}
						break;
					}
					default: {
						// treat both NONE and UNSPECIFIED the same
						isCached = false;
						break;
					}
				}
			}
		}

		naturalIdCacheRegion = null;

		final NaturalIdCache naturalIdCacheAnn = clazzToProcess.getAnnotation( NaturalIdCache.class );
		if ( naturalIdCacheAnn != null ) {
			if ( BinderHelper.isEmptyAnnotationValue( naturalIdCacheAnn.region() ) ) {
				if ( explicitCacheAnn != null && StringHelper.isNotEmpty( explicitCacheAnn.region() ) ) {
					naturalIdCacheRegion = explicitCacheAnn.region() + NATURAL_ID_CACHE_SUFFIX;
				}
				else {
					naturalIdCacheRegion = clazzToProcess.getName() + NATURAL_ID_CACHE_SUFFIX;
				}
			}
			else {
				naturalIdCacheRegion = naturalIdCacheAnn.region();
			}
		}
	}

	private static String resolveCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		final org.hibernate.cache.spi.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	private static Cache buildCacheMock(String region, MetadataBuildingContext context) {
		return new LocalCacheAnnotationStub( region, determineCacheConcurrencyStrategy( context ) );
	}

	@SuppressWarnings({ "ClassExplicitlyAnnotation" })
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
		return CacheConcurrencyStrategy.fromAccessType(
				context.getBuildingOptions().getImplicitCacheAccessType()
		);
	}

	private static class EntityTableObjectNameSource implements ObjectNameSource {
		private final String explicitName;
		private final String logicalName;

		private EntityTableObjectNameSource(String explicitName, String entityName) {
			this.explicitName = explicitName;
			this.logicalName = StringHelper.isNotEmpty( explicitName )
					? explicitName
					: StringHelper.unqualify( entityName );
		}

		public String getExplicitName() {
			return explicitName;
		}

		public String getLogicalName() {
			return logicalName;
		}
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
		if ( !SingleTableSubclass.class.isInstance( persistentClass ) ) {
			throw new AssertionFailure(
					"Was expecting a discriminated subclass [" + SingleTableSubclass.class.getName() +
							"] but found [" + persistentClass.getClass().getName() + "] for entity [" +
							persistentClass.getEntityName() + "]"
			);
		}

		context.getMetadataCollector().addEntityTableXref(
				persistentClass.getEntityName(),
				context.getMetadataCollector().getDatabase().toIdentifier(
						context.getMetadataCollector().getLogicalTableName(
								superTableXref.getPrimaryTable()
						)
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
		EntityTableNamingStrategyHelper namingStrategyHelper = new EntityTableNamingStrategyHelper(
				persistentClass.getClassName(),
				persistentClass.getEntityName(),
				name
		);

		final Identifier logicalName;
		if ( StringHelper.isNotEmpty( tableName ) ) {
			logicalName = namingStrategyHelper.handleExplicitName( tableName, context );
		}
		else {
			logicalName = namingStrategyHelper.determineImplicitName( context );
		}

		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				logicalName,
				persistentClass.isAbstract(),
				uniqueConstraints,
				null,
				constraints,
				context,
				this.subselect,
				denormalizedSuperTableXref
		);
		final RowId rowId = annotatedClass.getAnnotation( RowId.class );
		if ( rowId != null ) {
			table.setRowId( rowId.value() );
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
		/*
		 * Those operations has to be done after the id definition of the persistence class.
		 * ie after the properties parsing
		 */
		Iterator joins = secondaryTables.values().iterator();
		Iterator joinColumns = secondaryTableJoins.values().iterator();

		while ( joins.hasNext() ) {
			Object uncastedColumn = joinColumns.next();
			Join join = (Join) joins.next();
			createPrimaryColumnsToSecondaryTable( uncastedColumn, propertyHolder, join );
		}
	}

	private void createPrimaryColumnsToSecondaryTable(Object uncastedColumn, PropertyHolder propertyHolder, Join join) {
		Ejb3JoinColumn[] ejb3JoinColumns;
		PrimaryKeyJoinColumn[] pkColumnsAnn = null;
		JoinColumn[] joinColumnsAnn = null;
		if ( uncastedColumn instanceof PrimaryKeyJoinColumn[] ) {
			pkColumnsAnn = (PrimaryKeyJoinColumn[]) uncastedColumn;
		}
		if ( uncastedColumn instanceof JoinColumn[] ) {
			joinColumnsAnn = (JoinColumn[]) uncastedColumn;
		}
		if ( pkColumnsAnn == null && joinColumnsAnn == null ) {
			ejb3JoinColumns = new Ejb3JoinColumn[1];
			ejb3JoinColumns[0] = Ejb3JoinColumn.buildJoinColumn(
					null,
					null,
					persistentClass.getIdentifier(),
					secondaryTables,
					propertyHolder,
					context
			);
		}
		else {
			int nbrOfJoinColumns = pkColumnsAnn != null ?
					pkColumnsAnn.length :
					joinColumnsAnn.length;
			if ( nbrOfJoinColumns == 0 ) {
				ejb3JoinColumns = new Ejb3JoinColumn[1];
				ejb3JoinColumns[0] = Ejb3JoinColumn.buildJoinColumn(
						null,
						null,
						persistentClass.getIdentifier(),
						secondaryTables,
						propertyHolder,
						context
				);
			}
			else {
				ejb3JoinColumns = new Ejb3JoinColumn[nbrOfJoinColumns];
				if ( pkColumnsAnn != null ) {
					for (int colIndex = 0; colIndex < nbrOfJoinColumns; colIndex++) {
						ejb3JoinColumns[colIndex] = Ejb3JoinColumn.buildJoinColumn(
								pkColumnsAnn[colIndex],
								null,
								persistentClass.getIdentifier(),
								secondaryTables,
								propertyHolder,
								context
						);
					}
				}
				else {
					for (int colIndex = 0; colIndex < nbrOfJoinColumns; colIndex++) {
						ejb3JoinColumns[colIndex] = Ejb3JoinColumn.buildJoinColumn(
								null,
								joinColumnsAnn[colIndex],
								persistentClass.getIdentifier(),
								secondaryTables,
								propertyHolder,
								context
						);
					}
				}
			}
		}

		for (Ejb3JoinColumn joinColumn : ejb3JoinColumns) {
			joinColumn.forceNotNull();
		}
		bindJoinToPersistentClass( join, ejb3JoinColumns, context );
	}

	private void bindJoinToPersistentClass(Join join, Ejb3JoinColumn[] ejb3JoinColumns, MetadataBuildingContext buildingContext) {
		SimpleValue key = new DependantValue( buildingContext, join.getTable(), persistentClass.getIdentifier() );
		join.setKey( key );
		setFKNameIfDefined( join );
		key.setCascadeDeleteEnabled( false );
		TableBinder.bindFk( persistentClass, null, ejb3JoinColumns, key, false, buildingContext );
		join.createPrimaryKey();
		join.createForeignKey();
		persistentClass.addJoin( join );
	}

	private void setFKNameIfDefined(Join join) {
		// just awful..

		org.hibernate.annotations.Table matchingTable = findMatchingComplimentTableAnnotation( join );
		if ( matchingTable != null && !BinderHelper.isEmptyAnnotationValue( matchingTable.foreignKey().name() ) ) {
			( (SimpleValue) join.getKey() ).setForeignKeyName( matchingTable.foreignKey().name() );
		}
		else {
			javax.persistence.SecondaryTable jpaSecondaryTable = findMatchingSecondaryTable( join );
			if ( jpaSecondaryTable != null ) {
				if ( jpaSecondaryTable.foreignKey().value() == ConstraintMode.NO_CONSTRAINT ) {
					( (SimpleValue) join.getKey() ).setForeignKeyName( "none" );
				}
				else {
					( (SimpleValue) join.getKey() ).setForeignKeyName( StringHelper.nullIfEmpty( jpaSecondaryTable.foreignKey().name() ) );
					( (SimpleValue) join.getKey() ).setForeignKeyDefinition( StringHelper.nullIfEmpty( jpaSecondaryTable.foreignKey().foreignKeyDefinition() ) );
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

	private org.hibernate.annotations.Table findMatchingComplimentTableAnnotation(Join join) {
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

	public void firstLevelSecondaryTablesBinding(
			SecondaryTable secTable, SecondaryTables secTables
	) {
		if ( secTables != null ) {
			//loop through it
			for (SecondaryTable tab : secTables.value()) {
				addJoin( tab, null, null, false );
			}
		}
		else {
			if ( secTable != null ) addJoin( secTable, null, null, false );
		}
	}

	//Used for @*ToMany @JoinTable
	public Join addJoin(JoinTable joinTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		return addJoin( null, joinTable, holder, noDelayInPkColumnCreation );
	}

	private static class SecondaryTableNamingStrategyHelper implements NamingStrategyHelper {
		@Override
		public Identifier determineImplicitName(MetadataBuildingContext buildingContext) {
			// should maybe throw an exception here
			return null;
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

	private static SecondaryTableNamingStrategyHelper SEC_TBL_NS_HELPER = new SecondaryTableNamingStrategyHelper();

	private Join addJoin(
			SecondaryTable secondaryTable,
			JoinTable joinTable,
			PropertyHolder propertyHolder,
			boolean noDelayInPkColumnCreation) {
		// A non null propertyHolder means than we process the Pk creation without delay
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

		final InFlightMetadataCollector.EntityTableXref tableXref = context.getMetadataCollector().getEntityTableXref( persistentClass.getEntityName() );
		assert tableXref != null : "Could not locate EntityTableXref for entity [" + persistentClass.getEntityName() + "]";
		tableXref.addSecondaryTable( logicalName, join );

		if ( secondaryTable != null ) {
			TableBinder.addIndexes( table, secondaryTable.indexes(), context );
		}

			//no check constraints available on joins
		join.setTable( table );

		//somehow keep joins() for later.
		//Has to do the work later because it needs persistentClass id!
		LOG.debugf( "Adding secondary table to entity %s -> %s", persistentClass.getEntityName(), join.getTable().getName() );
		org.hibernate.annotations.Table matchingTable = findMatchingComplimentTableAnnotation( join );
		if ( matchingTable != null ) {
			join.setSequentialSelect( FetchMode.JOIN != matchingTable.fetch() );
			join.setInverse( matchingTable.inverse() );
			join.setOptional( matchingTable.optional() );
			if ( !BinderHelper.isEmptyAnnotationValue( matchingTable.sqlInsert().sql() ) ) {
				join.setCustomSQLInsert( matchingTable.sqlInsert().sql().trim(),
						matchingTable.sqlInsert().callable(),
						ExecuteUpdateResultCheckStyle.fromExternalName(
								matchingTable.sqlInsert().check().toString().toLowerCase(Locale.ROOT)
						)
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( matchingTable.sqlUpdate().sql() ) ) {
				join.setCustomSQLUpdate( matchingTable.sqlUpdate().sql().trim(),
						matchingTable.sqlUpdate().callable(),
						ExecuteUpdateResultCheckStyle.fromExternalName(
								matchingTable.sqlUpdate().check().toString().toLowerCase(Locale.ROOT)
						)
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( matchingTable.sqlDelete().sql() ) ) {
				join.setCustomSQLDelete( matchingTable.sqlDelete().sql().trim(),
						matchingTable.sqlDelete().callable(),
						ExecuteUpdateResultCheckStyle.fromExternalName(
								matchingTable.sqlDelete().check().toString().toLowerCase(Locale.ROOT)
						)
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
			secondaryTables.put( table.getQuotedName(), join );
			secondaryTableJoins.put( table.getQuotedName(), joinColumns );
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

	public void setInheritanceState(InheritanceState inheritanceState) {
		this.inheritanceState = inheritanceState;
	}

	public boolean isIgnoreIdAnnotations() {
		return ignoreIdAnnotations;
	}

	public void setIgnoreIdAnnotations(boolean ignoreIdAnnotations) {
		this.ignoreIdAnnotations = ignoreIdAnnotations;
	}
	public void processComplementaryTableDefinitions(javax.persistence.Table table) {
		if ( table == null ) return;
		TableBinder.addIndexes( persistentClass.getTable(), table.indexes(), context );
	}
	public void processComplementaryTableDefinitions(org.hibernate.annotations.Table table) {
		//comment and index are processed here
		if ( table == null ) return;
		String appliedTable = table.appliesTo();
		Iterator tables = persistentClass.getTableClosureIterator();
		Table hibTable = null;
		while ( tables.hasNext() ) {
			Table pcTable = (Table) tables.next();
			if ( pcTable.getQuotedName().equals( appliedTable ) ) {
				//we are in the correct table to find columns
				hibTable = pcTable;
				break;
			}
			hibTable = null;
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
		if ( !BinderHelper.isEmptyAnnotationValue( table.comment() ) ) hibTable.setComment( table.comment() );
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

		AccessType hibernateAccessType = null;
		AccessType jpaAccessType = null;

		org.hibernate.annotations.AccessType accessTypeAnnotation = element.getAnnotation( org.hibernate.annotations.AccessType.class );
		if ( accessTypeAnnotation != null ) {
			hibernateAccessType = AccessType.getAccessStrategy( accessTypeAnnotation.value() );
		}

		Access access = element.getAnnotation( Access.class );
		if ( access != null ) {
			jpaAccessType = AccessType.getAccessStrategy( access.value() );
		}

		if ( hibernateAccessType != null && jpaAccessType != null && hibernateAccessType != jpaAccessType ) {
			throw new MappingException(
					"Found @Access and @AccessType with conflicting values on a property in class " + annotatedClass.toString()
			);
		}

		if ( hibernateAccessType != null ) {
			accessType = hibernateAccessType;
		}
		else if ( jpaAccessType != null ) {
			accessType = jpaAccessType;
		}

		return accessType;
	}
}
