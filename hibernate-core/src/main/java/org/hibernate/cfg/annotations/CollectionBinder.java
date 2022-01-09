/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.mapping.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.FilterJoinTables;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.BootLogging;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.InFlightMetadataCollector.CollectionTypeRegistrationDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotatedClassType;
import org.hibernate.cfg.AnnotatedColumn;
import org.hibernate.cfg.AnnotatedJoinColumn;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.CollectionPropertyHolder;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.cfg.IndexColumn;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyHolderBuilder;
import org.hibernate.cfg.PropertyInferredData;
import org.hibernate.cfg.PropertyPreloadedData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.EmbeddableInstantiator;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserCollectionType;

import org.jboss.logging.Logger;

import jakarta.persistence.Access;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import static jakarta.persistence.AccessType.PROPERTY;
import static org.hibernate.cfg.BinderHelper.toAliasEntityMap;
import static org.hibernate.cfg.BinderHelper.toAliasTableMap;

/**
 * Base class for binding different types of collections to Hibernate configuration objects.
 *
 * @author inger
 * @author Emmanuel Bernard
 */
@SuppressWarnings({"unchecked", "WeakerAccess", "deprecation"})
public abstract class CollectionBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, CollectionBinder.class.getName());

	private static final List<Class<?>> INFERRED_CLASS_PRIORITY = List.of(
			List.class,
			java.util.SortedSet.class,
			java.util.Set.class,
			java.util.SortedMap.class,
			Map.class,
			java.util.Collection.class
	);

	private final MetadataBuildingContext buildingContext;
	private final Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver;
	private final boolean isSortedCollection;

	protected Collection collection;
	protected String propertyName;
	PropertyHolder propertyHolder;
	private int batchSize;
	private String mappedBy;
	private XClass collectionType;
	private XClass targetEntity;
	private AnnotatedJoinColumn[] inverseJoinColumns;
	private String cascadeStrategy;
	private String cacheConcurrencyStrategy;
	private String cacheRegionName;
	private boolean oneToMany;
	protected IndexColumn indexColumn;
	protected boolean cascadeDeleteEnabled;
	protected String mapKeyPropertyName;
	private boolean insertable = true;
	private boolean updatable = true;
	private AnnotatedJoinColumn[] fkJoinColumns;
	private boolean isExplicitAssociationTable;
	private AnnotatedColumn[] elementColumns;
	private boolean isEmbedded;
	private XProperty property;
	private boolean ignoreNotFound;
	private TableBinder tableBinder;
	private AnnotatedColumn[] mapKeyColumns;
	private AnnotatedJoinColumn[] mapKeyManyToManyColumns;
	protected HashMap<String, IdentifierGeneratorDefinition> localGenerators;
	protected Map<XClass, InheritanceState> inheritanceStatePerClass;
	private XClass declaringClass;
	private boolean declaringClassSet;
	private AccessType accessType;
	private boolean hibernateExtensionMapping;

	private jakarta.persistence.OrderBy jpaOrderBy;
	private OrderBy sqlOrderBy;
	private SortNatural naturalSort;
	private SortComparator comparatorSort;

	private String explicitType;
	private final Properties explicitTypeParameters = new Properties();

	protected CollectionBinder(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			boolean isSortedCollection,
			MetadataBuildingContext buildingContext) {
		this.customTypeBeanResolver = customTypeBeanResolver;
		this.isSortedCollection = isSortedCollection;
		this.buildingContext = buildingContext;
	}

	protected MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	public Supplier<ManagedBean<? extends UserCollectionType>> getCustomTypeBeanResolver() {
		return customTypeBeanResolver;
	}

	public boolean isMap() {
		return false;
	}

	protected void setIsHibernateExtensionMapping(boolean hibernateExtensionMapping) {
		this.hibernateExtensionMapping = hibernateExtensionMapping;
	}

	protected boolean isHibernateExtensionMapping() {
		return hibernateExtensionMapping;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setInheritanceStatePerClass(Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public void setCascadeStrategy(String cascadeStrategy) {
		this.cascadeStrategy = cascadeStrategy;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public void setInverseJoinColumns(AnnotatedJoinColumn[] inverseJoinColumns) {
		this.inverseJoinColumns = inverseJoinColumns;
	}

	public void setJoinColumns(AnnotatedJoinColumn[] joinColumns) {
		this.joinColumns = joinColumns;
	}

	private AnnotatedJoinColumn[] joinColumns;

	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
	}

	public void setBatchSize(BatchSize batchSize) {
		this.batchSize = batchSize == null ? -1 : batchSize.size();
	}

	public void setJpaOrderBy(jakarta.persistence.OrderBy jpaOrderBy) {
		this.jpaOrderBy = jpaOrderBy;
	}

	public void setSqlOrderBy(OrderBy sqlOrderBy) {
		this.sqlOrderBy = sqlOrderBy;
	}

	public void setNaturalSort(SortNatural naturalSort) {
		this.naturalSort = naturalSort;
	}

	public void setComparatorSort(SortComparator comparatorSort) {
		this.comparatorSort = comparatorSort;
	}

	/**
	 * collection binder factory
	 */
	public static CollectionBinder getCollectionBinder(
			XProperty property,
			boolean isHibernateExtensionMapping,
			MetadataBuildingContext buildingContext) {
		final CollectionType typeAnnotation = HCANNHelper.findAnnotation( property, CollectionType.class );

		final CollectionBinder binder;
		if ( typeAnnotation != null ) {
			binder = createBinderFromCustomTypeAnnotation( property, typeAnnotation, buildingContext );

			// todo (6.0) - technically, these should no longer be needed
			binder.explicitType = typeAnnotation.type().getName();
			for ( Parameter param : typeAnnotation.parameters() ) {
				binder.explicitTypeParameters.setProperty( param.name(), param.value() );
			}
		}
		else {
			final CollectionClassification classification = determineCollectionClassification( property, buildingContext );
			final CollectionTypeRegistrationDescriptor typeRegistration = buildingContext
					.getMetadataCollector()
					.findCollectionTypeRegistration( classification );
			if ( typeRegistration != null ) {
				binder = createBinderFromTypeRegistration( property, classification, typeRegistration, buildingContext );
			}
			else {
				binder = createBinderFromProperty( property, buildingContext );
			}
		}

		binder.setIsHibernateExtensionMapping( isHibernateExtensionMapping );

		return binder;
	}

	private static CollectionBinder createBinderFromTypeRegistration(
			XProperty property,
			CollectionClassification classification,
			CollectionTypeRegistrationDescriptor typeRegistration,
			MetadataBuildingContext buildingContext) {
		return createBinder(
				property,
				() -> createCustomType(
						property.getDeclaringClass().getName() + "#" + property.getName(),
						typeRegistration.getImplementation(),
						typeRegistration.getParameters(),
						buildingContext
				),
				classification,
				buildingContext
		);
	}

	private static ManagedBean<? extends UserCollectionType> createCustomType(
			String role,
			Class<? extends UserCollectionType> implementation,
			Properties parameters,
			MetadataBuildingContext buildingContext) {
		final StandardServiceRegistry serviceRegistry = buildingContext.getBuildingOptions().getServiceRegistry();
		final ManagedBeanRegistry beanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
		if ( CollectionHelper.isNotEmpty( parameters ) ) {
			return beanRegistry.getBean( implementation );
		}
		else {
			// defined parameters...
			if ( ParameterizedType.class.isAssignableFrom( implementation ) ) {
				// because there are config parameters and the type is configurable, we need
				// a separate bean instance which means uniquely naming it
				final ManagedBean<? extends UserCollectionType> typeBean = beanRegistry.getBean( role, implementation );
				final UserCollectionType type = typeBean.getBeanInstance();
				( (ParameterizedType) type ).setParameterValues( parameters );
				return typeBean;
			}
			else {
				// log a "warning"
				BootLogging.LOGGER.debugf(
						"Custom collection-type (`%s`) assigned to attribute (`%s`) does not implement `%s`, but its `@CollectionType` defined parameters",
						implementation.getName(),
						role,
						ParameterizedType.class.getName()
				);

				// but still return the bean - we can again use the no-config bean instance
				return beanRegistry.getBean( implementation );
			}
		}
	}

	private static CollectionBinder createBinderFromProperty(
			XProperty property,
			MetadataBuildingContext buildingContext) {
		final CollectionClassification classification = determineCollectionClassification( property, buildingContext );
		return createBinder( property, null, classification, buildingContext );
	}

	private static CollectionBinder createBinderFromCustomTypeAnnotation(
			XProperty property,
			CollectionType typeAnnotation,
			MetadataBuildingContext buildingContext) {
		determineSemanticJavaType( property );

		final ManagedBean<? extends UserCollectionType> customTypeBean = resolveCustomType( property, typeAnnotation, buildingContext );
		return createBinder(
				property,
				() -> customTypeBean,
				customTypeBean.getBeanInstance().getClassification(),
				buildingContext
		);
	}

	public static ManagedBean<? extends UserCollectionType> resolveCustomType(
			XProperty property,
			CollectionType typeAnnotation,
			MetadataBuildingContext buildingContext) {
		final ManagedBeanRegistry beanRegistry = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		final Class<? extends UserCollectionType> typeImpl = typeAnnotation.type();
		if ( typeAnnotation.parameters().length == 0 ) {
			// no parameters - we can re-use a no-config bean instance
			return beanRegistry.getBean( typeImpl );
		}
		else {
			// defined parameters...
			final String attributeKey = property.getDeclaringClass().getName() + "#" + property.getName();

			if ( ParameterizedType.class.isAssignableFrom( typeImpl ) ) {
				// because there are config parameters and the type is configurable, we need
				// a separate bean instance which means uniquely naming it
				final ManagedBean<? extends UserCollectionType> typeBean = beanRegistry.getBean( attributeKey, typeImpl );
				final UserCollectionType type = typeBean.getBeanInstance();
				( (ParameterizedType) type ).setParameterValues( extractParameters( typeAnnotation ) );
				return typeBean;
			}
			else {
				// log a "warning"
				BootLogging.LOGGER.debugf(
						"Custom collection-type (`%s`) assigned to attribute (`%s`) does not implement `%s`, but its `@CollectionType` defined parameters",
						typeImpl.getName(),
						attributeKey,
						ParameterizedType.class.getName()
				);

				// but still return the bean - we can again use the no-config bean instance
				return beanRegistry.getBean( typeImpl );
			}
		}
	}

	private static Properties extractParameters(CollectionType typeAnnotation) {
		final Parameter[] parameterAnnotations = typeAnnotation.parameters();
		final Properties configParams = new Properties( parameterAnnotations.length );
		for ( Parameter parameterAnnotation : parameterAnnotations ) {
			configParams.put( parameterAnnotation.name(), parameterAnnotation.value() );
		}
		return configParams;
	}

	private static CollectionBinder createBinder(
			XProperty property,
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanAccess,
			CollectionClassification classification,
			MetadataBuildingContext buildingContext) {
		switch ( classification ) {
			case ARRAY: {
				if ( property.getElementClass().isPrimitive() ) {
					return new PrimitiveArrayBinder( customTypeBeanAccess, buildingContext );
				}
				return new ArrayBinder( customTypeBeanAccess, buildingContext );
			}
			case BAG: {
				return new BagBinder( customTypeBeanAccess, buildingContext );
			}
			case ID_BAG: {
				return new IdBagBinder( customTypeBeanAccess, buildingContext );
			}
			case LIST: {
				return new ListBinder( customTypeBeanAccess, buildingContext );
			}
			case MAP:
			case ORDERED_MAP: {
				return new MapBinder( customTypeBeanAccess, false, buildingContext );
			}
			case SORTED_MAP: {
				return new MapBinder( customTypeBeanAccess, true, buildingContext );
			}
			case SET:
			case ORDERED_SET: {
				return new SetBinder( customTypeBeanAccess, false, buildingContext );
			}
			case SORTED_SET: {
				return new SetBinder( customTypeBeanAccess, true, buildingContext );
			}
		}

		final XClass declaringClass = property.getDeclaringClass();

		throw new AnnotationException(
				String.format(
						Locale.ROOT,
						"Unable to determine proper CollectionBinder (`%s) : %s.%s",
						classification,
						declaringClass.getName(),
						property.getName()
				)
		);
	}

	private static CollectionClassification determineCollectionClassification(
			XProperty property,
			MetadataBuildingContext buildingContext) {
		if ( property.isArray() ) {
			return CollectionClassification.ARRAY;
		}

		final Bag bagAnnotation = HCANNHelper.findAnnotation( property, Bag.class );
		if ( bagAnnotation != null ) {
			final Class<?> collectionJavaType = property.getCollectionClass();
			if ( java.util.List.class.equals( collectionJavaType ) || java.util.Collection.class.equals( collectionJavaType ) ) {
				return CollectionClassification.BAG;
			}
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@Bag annotation encountered on an attribute `%s#%s` of type `%s`; only `%s` and `%s` are supported",
							property.getDeclaringClass().getName(),
							property.getName(),
							collectionJavaType.getName(),
							java.util.List.class.getName(),
							java.util.Collection.class.getName()
					)
			);
		}

		return determineCollectionClassification(
				determineSemanticJavaType( property ),
				property,
				buildingContext
		);
	}

	private static CollectionClassification determineCollectionClassification(
			Class<?> semanticJavaType,
			XProperty property,
			MetadataBuildingContext buildingContext) {
		if ( semanticJavaType.isArray() ) {
			return CollectionClassification.ARRAY;
		}
		else if ( property.isAnnotationPresent( CollectionId.class )
				|| property.isAnnotationPresent( CollectionIdJdbcType.class )
				|| property.isAnnotationPresent( CollectionIdJdbcTypeCode.class )
				|| property.isAnnotationPresent( CollectionIdJavaType.class ) ) {
			// explicitly an ID_BAG
			return CollectionClassification.ID_BAG;
		}
		else if ( java.util.List.class.isAssignableFrom( semanticJavaType ) ) {
			if ( property.isAnnotationPresent( OrderColumn.class )
					|| property.isAnnotationPresent( org.hibernate.annotations.IndexColumn.class )
					|| property.isAnnotationPresent( ListIndexBase.class )
					|| property.isAnnotationPresent( ListIndexJdbcType.class )
					|| property.isAnnotationPresent( ListIndexJdbcTypeCode.class )
					|| property.isAnnotationPresent( ListIndexJavaType.class ) ) {
				// it is implicitly a LIST because of presence of explicit List index config
				return CollectionClassification.LIST;
			}
			// otherwise, return the implicit classification for List attributes
			return buildingContext.getBuildingOptions().getMappingDefaults().getImplicitListClassification();
		}
		else if ( java.util.SortedSet.class.isAssignableFrom( semanticJavaType ) ) {
			return CollectionClassification.SORTED_SET;
		}
		else if ( java.util.Set.class.isAssignableFrom( semanticJavaType ) ) {
			return CollectionClassification.SET;
		}
		else if ( java.util.SortedMap.class.isAssignableFrom( semanticJavaType ) ) {
			return CollectionClassification.SORTED_MAP;
		}
		else if ( java.util.Map.class.isAssignableFrom( semanticJavaType ) ) {
			return CollectionClassification.MAP;
		}
		else if ( java.util.Collection.class.isAssignableFrom( semanticJavaType ) ) {
			if ( property.isAnnotationPresent( CollectionId.class ) ) {
				return CollectionClassification.ID_BAG;
			}
			else {
				return CollectionClassification.BAG;
			}
		}
		else {
			return null;
		}
	}

	private static Class<?> determineSemanticJavaType(XProperty property) {
		final Class<?> returnedJavaType = property.getCollectionClass();
		if ( returnedJavaType == null ) {
			throw new AnnotationException(
					String.format(
							Locale.ROOT,
							"Illegal attempt to map a non collection as a @OneToMany, @ManyToMany or @CollectionOfElements: %s.%s",
							property.getDeclaringClass().getName(),
							property.getName()
					)
			);
		}

		return inferCollectionClassFromSubclass( returnedJavaType );
	}

	private static Class<?> inferCollectionClassFromSubclass(Class<?> clazz) {
		for ( Class<?> priorityClass : INFERRED_CLASS_PRIORITY ) {
			if ( priorityClass.isAssignableFrom( clazz ) ) {
				return priorityClass;
			}
		}

		return null;
	}

	public void setMappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
	}

	public void setTableBinder(TableBinder tableBinder) {
		this.tableBinder = tableBinder;
	}

	public void setCollectionType(XClass collectionType) {
		// NOTE: really really badly named.  This is actually NOT the collection-type, but rather the collection-element-type!
		this.collectionType = collectionType;
	}

	public void setTargetEntity(XClass targetEntity) {
		this.targetEntity = targetEntity;
	}

	protected abstract Collection createCollection(PersistentClass persistentClass);

	public Collection getCollection() {
		return collection;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setDeclaringClass(XClass declaringClass) {
		this.declaringClass = declaringClass;
		this.declaringClassSet = true;
	}

	public void bind() {
		this.collection = createCollection( propertyHolder.getPersistentClass() );
		String role = StringHelper.qualify( propertyHolder.getPath(), propertyName );
		LOG.debugf( "Collection role: %s", role );
		collection.setRole( role );
		collection.setMappedByProperty( mappedBy );

		if ( property.isAnnotationPresent( MapKeyColumn.class )
			&& mapKeyPropertyName != null ) {
			throw new AnnotationException(
					"Cannot mix @jakarta.persistence.MapKey and @MapKeyColumn or @org.hibernate.annotations.MapKey "
							+ "on the same collection: " + StringHelper.qualify(
							propertyHolder.getPath(), propertyName
					)
			);
		}

		// set explicit type information
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		if ( explicitType != null ) {
			final TypeDefinition typeDef = metadataCollector.getTypeDefinition( explicitType );
			if ( typeDef == null ) {
				collection.setTypeName( explicitType );
				collection.setTypeParameters( explicitTypeParameters );
			}
			else {
				collection.setTypeName( typeDef.getTypeImplementorClass().getName() );
				collection.setTypeParameters( typeDef.getParameters() );
			}
		}

		//set laziness
		defineFetchingStrategy();
		collection.setBatchSize( batchSize );

		collection.setMutable( !property.isAnnotationPresent( Immutable.class ) );

		//work on association
		boolean isMappedBy = !BinderHelper.isEmptyAnnotationValue( mappedBy );

		final OptimisticLock lockAnn = property.getAnnotation( OptimisticLock.class );
		final boolean includeInOptimisticLockChecks = ( lockAnn != null )
				? ! lockAnn.excluded()
				: ! isMappedBy;
		collection.setOptimisticLocked( includeInOptimisticLockChecks );

		Persister persisterAnn = property.getAnnotation( Persister.class );
		if ( persisterAnn != null ) {
			//noinspection rawtypes
			collection.setCollectionPersisterClass( (Class) persisterAnn.impl() );
		}

		applySortingAndOrdering( collection );

		//set cache
		if ( StringHelper.isNotEmpty( cacheConcurrencyStrategy ) ) {
			collection.setCacheConcurrencyStrategy( cacheConcurrencyStrategy );
			collection.setCacheRegionName( cacheRegionName );
		}

		//SQL overriding
		SQLInsert sqlInsert = property.getAnnotation( SQLInsert.class );
		SQLUpdate sqlUpdate = property.getAnnotation( SQLUpdate.class );
		SQLDelete sqlDelete = property.getAnnotation( SQLDelete.class );
		SQLDeleteAll sqlDeleteAll = property.getAnnotation( SQLDeleteAll.class );
		Loader loader = property.getAnnotation( Loader.class );
		if ( sqlInsert != null ) {
			collection.setCustomSQLInsert( sqlInsert.sql().trim(), sqlInsert.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlInsert.check().toString().toLowerCase(Locale.ROOT) )
			);

		}
		if ( sqlUpdate != null ) {
			collection.setCustomSQLUpdate( sqlUpdate.sql(), sqlUpdate.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlUpdate.check().toString().toLowerCase(Locale.ROOT) )
			);
		}
		if ( sqlDelete != null ) {
			collection.setCustomSQLDelete( sqlDelete.sql(), sqlDelete.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlDelete.check().toString().toLowerCase(Locale.ROOT) )
			);
		}
		if ( sqlDeleteAll != null ) {
			collection.setCustomSQLDeleteAll( sqlDeleteAll.sql(), sqlDeleteAll.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlDeleteAll.check().toString().toLowerCase(Locale.ROOT) )
			);
		}
		if ( loader != null ) {
			collection.setLoaderName( loader.namedQuery() );
		}

		if (isMappedBy
				&& (property.isAnnotationPresent( JoinColumn.class )
					|| property.isAnnotationPresent( JoinColumns.class )
					|| propertyHolder.getJoinTable( property ) != null ) ) {
			String message = "Associations marked as mappedBy must not define database mappings like @JoinTable or @JoinColumn: ";
			message += StringHelper.qualify( propertyHolder.getPath(), propertyName );
			throw new AnnotationException( message );
		}

		if (!isMappedBy
				&& oneToMany
				&& property.isAnnotationPresent( OnDelete.class )
				&& !property.isAnnotationPresent( JoinColumn.class )) {
			String message = "Unidirectional one-to-many associations annotated with @OnDelete must define @JoinColumn: ";
			message += StringHelper.qualify( propertyHolder.getPath(), propertyName );
			throw new AnnotationException( message );
		}

		collection.setInverse( isMappedBy );

		//many to many may need some second pass information
		if ( !oneToMany && isMappedBy ) {
			metadataCollector.addMappedBy( getCollectionType().getName(), mappedBy, propertyName );
		}
		//TODO reduce tableBinder != null and oneToMany
		XClass collectionType = getCollectionType();
		if ( inheritanceStatePerClass == null) {
			throw new AssertionFailure( "inheritanceStatePerClass not set" );
		}
		SecondPass sp = getSecondPass(
				fkJoinColumns,
				joinColumns,
				inverseJoinColumns,
				elementColumns,
				mapKeyColumns,
				mapKeyManyToManyColumns,
				isEmbedded,
				property,
				collectionType,
				ignoreNotFound,
				oneToMany,
				tableBinder,
				buildingContext
		);
		if ( collectionType.isAnnotationPresent( Embeddable.class )
				|| property.isAnnotationPresent( ElementCollection.class ) //JPA 2
				) {
			// do it right away, otherwise @ManyToOne on composite element call addSecondPass
			// and raise a ConcurrentModificationException
			//sp.doSecondPass( CollectionHelper.EMPTY_MAP );
			metadataCollector.addSecondPass( sp, !isMappedBy );
		}
		else {
			metadataCollector.addSecondPass( sp, !isMappedBy );
		}

		metadataCollector.addCollectionBinding( collection );

		//property building
		PropertyBinder binder = new PropertyBinder();
		binder.setName( propertyName );
		binder.setValue( collection );
		binder.setCascade( cascadeStrategy );
		if ( cascadeStrategy != null && cascadeStrategy.contains( "delete-orphan" ) ) {
			collection.setOrphanDelete( true );
		}
		binder.setLazy( collection.isLazy() );
		final LazyGroup lazyGroupAnnotation = property.getAnnotation( LazyGroup.class );
		if ( lazyGroupAnnotation != null ) {
			binder.setLazyGroup( lazyGroupAnnotation.value() );
		}
		binder.setAccessType( accessType );
		binder.setProperty( property );
		binder.setInsertable( insertable );
		binder.setUpdatable( updatable );
		Property prop = binder.makeProperty();
		//we don't care about the join stuffs because the column is on the association table.
		if (! declaringClassSet) {
			throw new AssertionFailure( "DeclaringClass is not set in CollectionBinder while binding" );
		}
		propertyHolder.addProperty( prop, declaringClass );
	}

	private void applySortingAndOrdering(Collection collection) {
		final boolean hadExplicitSort;
		final Class<? extends Comparator<?>> comparatorClass;

		if ( naturalSort != null ) {
			if ( comparatorSort != null ) {
				throw buildIllegalSortCombination();
			}
			hadExplicitSort = true;
			comparatorClass = null;
		}
		else if ( comparatorSort != null ) {
			hadExplicitSort = true;
			comparatorClass = comparatorSort.value();
		}
		else {
			hadExplicitSort = false;
			comparatorClass = null;
		}

		boolean hadOrderBy = false;
		if ( jpaOrderBy != null || sqlOrderBy != null ) {
			if ( jpaOrderBy != null && sqlOrderBy != null ) {
				throw buildIllegalOrderCombination();
			}

			hadOrderBy = true;

			// we can only apply the sql-based order by up front.  The jpa order by has to wait for second pass
			if ( sqlOrderBy != null ) {
				collection.setOrderBy( sqlOrderBy.clause() );
			}
		}

		final boolean isSorted = isSortedCollection || hadExplicitSort;

		if ( isSorted && hadOrderBy ) {
			throw buildIllegalOrderAndSortCombination();
		}

		collection.setSorted( isSorted );

		if ( comparatorClass != null ) {
			try {
				collection.setComparator( comparatorClass.newInstance() );
			}
			catch (Exception e) {
				throw new AnnotationException(
						String.format(
								"Could not instantiate comparator class [%s] for %s",
								comparatorClass.getName(),
								safeCollectionRole()
						)
				);
			}
		}
	}

	private AnnotationException buildIllegalOrderCombination() {
		return new AnnotationException(
				String.format(
						Locale.ROOT,
						"Illegal combination of ordering and sorting annotations (`%s`) - only one of `@%s` and `@%s` may be used",
						jakarta.persistence.OrderBy.class.getName(),
						OrderBy.class.getName(),
						safeCollectionRole()
				)
		);
	}

	private AnnotationException buildIllegalOrderAndSortCombination() {
		throw new AnnotationException(
				String.format(
						Locale.ROOT,
						"Illegal combination of ordering and sorting annotations (`%s`) - only one of `@%s`, `@%s`, `@%s` and `@%s` can be used",
						safeCollectionRole(),
						jakarta.persistence.OrderBy.class.getName(),
						OrderBy.class.getName(),
						SortComparator.class.getName(),
						SortNatural.class.getName()
				)
		);
	}

	private AnnotationException buildIllegalSortCombination() {
		return new AnnotationException(
				String.format(
						"Illegal combination of sorting annotations (`%s`) - only one of `@%s` and `@%s` can be used",
						safeCollectionRole(),
						SortNatural.class.getName(),
						SortComparator.class.getName()
				)
		);
	}

	private void defineFetchingStrategy() {
		LazyCollection lazy = property.getAnnotation( LazyCollection.class );
		Fetch fetch = property.getAnnotation( Fetch.class );
		OneToMany oneToMany = property.getAnnotation( OneToMany.class );
		ManyToMany manyToMany = property.getAnnotation( ManyToMany.class );
		ElementCollection elementCollection = property.getAnnotation( ElementCollection.class );
		ManyToAny manyToAny = property.getAnnotation( ManyToAny.class );
		FetchType fetchType;
		if ( oneToMany != null ) {
			fetchType = oneToMany.fetch();
		}
		else if ( manyToMany != null ) {
			fetchType = manyToMany.fetch();
		}
		else if ( elementCollection != null ) {
			fetchType = elementCollection.fetch();
		}
		else if ( manyToAny != null ) {
			fetchType = FetchType.LAZY;
		}
		else {
			throw new AssertionFailure(
					"Define fetch strategy on a property not annotated with @ManyToOne nor @OneToMany nor @ElementCollection"
			);
		}
		if ( lazy != null ) {
			collection.setLazy( !( lazy.value() == LazyCollectionOption.FALSE ) );
			collection.setExtraLazy( lazy.value() == LazyCollectionOption.EXTRA );
		}
		else {
			collection.setLazy( fetchType == FetchType.LAZY );
			collection.setExtraLazy( false );
		}
		if ( fetch != null ) {
			if ( fetch.value() == org.hibernate.annotations.FetchMode.JOIN ) {
				collection.setFetchMode( FetchMode.JOIN );
				collection.setLazy( false );
			}
			else if ( fetch.value() == org.hibernate.annotations.FetchMode.SELECT ) {
				collection.setFetchMode( FetchMode.SELECT );
			}
			else if ( fetch.value() == org.hibernate.annotations.FetchMode.SUBSELECT ) {
				collection.setFetchMode( FetchMode.SELECT );
				collection.setSubselectLoadable( true );
				collection.getOwner().setSubselectLoadableCollections( true );
			}
			else {
				throw new AssertionFailure( "Unknown FetchMode: " + fetch.value() );
			}
		}
		else {
			collection.setFetchMode( AnnotationBinder.getFetchMode( fetchType ) );
		}
	}

	private XClass getCollectionType() {
		if ( AnnotationBinder.isDefault( targetEntity, buildingContext ) ) {
			if ( collectionType != null ) {
				return collectionType;
			}
			else {
				String errorMsg = "Collection has neither generic type or OneToMany.targetEntity() defined: "
						+ safeCollectionRole();
				throw new AnnotationException( errorMsg );
			}
		}
		else {
			return targetEntity;
		}
	}

	public SecondPass getSecondPass(
			final AnnotatedJoinColumn[] fkJoinColumns,
			final AnnotatedJoinColumn[] keyColumns,
			final AnnotatedJoinColumn[] inverseColumns,
			final AnnotatedColumn[] elementColumns,
			final AnnotatedColumn[] mapKeyColumns,
			final AnnotatedJoinColumn[] mapKeyManyToManyColumns,
			final boolean isEmbedded,
			final XProperty property,
			final XClass collType,
			final boolean ignoreNotFound,
			final boolean unique,
			final TableBinder assocTableBinder,
			final MetadataBuildingContext buildingContext) {
		return new CollectionSecondPass( buildingContext, collection ) {
			@SuppressWarnings("rawtypes")
			@Override
			public void secondPass(Map persistentClasses, Map inheritedMetas) throws MappingException {
				bindStarToManySecondPass(
						persistentClasses,
						collType,
						fkJoinColumns,
						keyColumns,
						inverseColumns,
						elementColumns,
						isEmbedded,
						property,
						unique,
						assocTableBinder,
						ignoreNotFound,
						buildingContext
				);
			}
		};
	}

	/**
	 * return true if it's a Fk, false if it's an association table
	 */
	protected boolean bindStarToManySecondPass(
			Map<String, PersistentClass> persistentClasses,
			XClass collType,
			AnnotatedJoinColumn[] fkJoinColumns,
			AnnotatedJoinColumn[] keyColumns,
			AnnotatedJoinColumn[] inverseColumns,
			AnnotatedColumn[] elementColumns,
			boolean isEmbedded,
			XProperty property,
			boolean unique,
			TableBinder associationTableBinder,
			boolean ignoreNotFound,
			MetadataBuildingContext buildingContext) {
		PersistentClass persistentClass = persistentClasses.get( collType.getName() );
		boolean reversePropertyInJoin = false;
		if ( persistentClass != null && StringHelper.isNotEmpty( this.mappedBy ) ) {
			try {
				reversePropertyInJoin = 0 != persistentClass.getJoinNumber(
						persistentClass.getRecursiveProperty( this.mappedBy )
				);
			}
			catch (MappingException e) {
				throw new AnnotationException(
						"mappedBy reference an unknown target entity property: " +
								collType + "." + this.mappedBy +
								" in " +
								collection.getOwnerEntityName() +
								"." +
								property.getName()
				);
			}
		}
		if ( persistentClass != null
				&& !reversePropertyInJoin
				&& oneToMany
				&& !this.isExplicitAssociationTable
				&& ( joinColumns[0].isImplicit() && !BinderHelper.isEmptyAnnotationValue( this.mappedBy ) //implicit @JoinColumn
				|| !fkJoinColumns[0].isImplicit() ) //this is an explicit @JoinColumn
				) {
			//this is a Foreign key
			bindOneToManySecondPass(
					getCollection(),
					persistentClasses,
					fkJoinColumns,
					collType,
					cascadeDeleteEnabled,
					ignoreNotFound,
					buildingContext,
					inheritanceStatePerClass
			);
			return true;
		}
		else {
			//this is an association table
			bindManyToManySecondPass(
					this.collection,
					persistentClasses,
					keyColumns,
					inverseColumns,
					elementColumns,
					isEmbedded, collType,
					ignoreNotFound, unique,
					cascadeDeleteEnabled,
					associationTableBinder,
					property,
					propertyHolder,
					buildingContext
			);
			return false;
		}
	}

	protected void bindOneToManySecondPass(
			Collection collection,
			Map<String, PersistentClass> persistentClasses,
			AnnotatedJoinColumn[] fkJoinColumns,
			XClass collectionType,
			boolean cascadeDeleteEnabled,
			boolean ignoreNotFound,
			MetadataBuildingContext buildingContext,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding a OneToMany: %s.%s through a foreign key", propertyHolder.getEntityName(), propertyName );
		}
		if ( buildingContext == null ) {
			throw new AssertionFailure(
					"CollectionSecondPass for oneToMany should not be called with null mappings"
			);
		}
		org.hibernate.mapping.OneToMany oneToMany = new org.hibernate.mapping.OneToMany( buildingContext, collection.getOwner() );
		collection.setElement( oneToMany );
		oneToMany.setReferencedEntityName( collectionType.getName() );
		oneToMany.setIgnoreNotFound( ignoreNotFound );

		String assocClass = oneToMany.getReferencedEntityName();
		PersistentClass associatedClass = persistentClasses.get( assocClass );
		if ( jpaOrderBy != null ) {
			final String orderByFragment = buildOrderByClauseFromHql(
					jpaOrderBy.value(),
					associatedClass,
					collection.getRole()
			);
			if ( StringHelper.isNotEmpty( orderByFragment ) ) {
				collection.setOrderBy( orderByFragment );
			}
		}
		Map<String, Join> joins = buildingContext.getMetadataCollector().getJoins( assocClass );
		if ( associatedClass == null ) {
			throw new MappingException(
					String.format("Association [%s] for entity [%s] references unmapped class [%s]",
							propertyName, propertyHolder.getClassName(), assocClass)
			);
		}
		oneToMany.setAssociatedClass( associatedClass );
		for (AnnotatedJoinColumn column : fkJoinColumns) {
			column.setPersistentClass( associatedClass, joins, inheritanceStatePerClass );
			column.setJoins( joins );
			collection.setCollectionTable( column.getTable() );
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Mapping collection: %s -> %s", collection.getRole(), collection.getCollectionTable().getName() );
		}
		bindFilters( false );
		bindCollectionSecondPass( collection, null, fkJoinColumns, cascadeDeleteEnabled, property, propertyHolder, buildingContext );
		if ( !collection.isInverse()
				&& !collection.getKey().isNullable() ) {
			// for non-inverse one-to-many, with a not-null fk, add a backref!
			String entityName = oneToMany.getReferencedEntityName();
			PersistentClass referenced = buildingContext.getMetadataCollector().getEntityBinding( entityName );
			Backref prop = new Backref();
			prop.setName( '_' + fkJoinColumns[0].getPropertyName() + '_' + fkJoinColumns[0].getLogicalColumnName() + "Backref" );
			prop.setUpdateable( false );
			prop.setSelectable( false );
			prop.setCollectionRole( collection.getRole() );
			prop.setEntityName( collection.getOwner().getEntityName() );
			prop.setValue( collection.getKey() );
			referenced.addProperty( prop );
		}
	}


	private void bindFilters(boolean hasAssociationTable) {
		Filter simpleFilter = property.getAnnotation( Filter.class );
		//set filtering
		//test incompatible choices
		//if ( StringHelper.isNotEmpty( where ) ) collection.setWhere( where );
		if ( simpleFilter != null ) {
			if ( hasAssociationTable ) {
				collection.addManyToManyFilter(simpleFilter.name(), getCondition(simpleFilter), simpleFilter.deduceAliasInjectionPoints(),
						toAliasTableMap(simpleFilter.aliases()), toAliasEntityMap(simpleFilter.aliases()));
			}
			else {
				collection.addFilter(simpleFilter.name(), getCondition(simpleFilter), simpleFilter.deduceAliasInjectionPoints(),
						toAliasTableMap(simpleFilter.aliases()), toAliasEntityMap(simpleFilter.aliases()));
			}
		}
		Filters filters = property.getAnnotation( Filters.class );
		if ( filters != null ) {
			for (Filter filter : filters.value()) {
				if ( hasAssociationTable ) {
					collection.addManyToManyFilter( filter.name(), getCondition(filter), filter.deduceAliasInjectionPoints(),
							toAliasTableMap(filter.aliases()), toAliasEntityMap(filter.aliases()));
				}
				else {
					collection.addFilter(filter.name(), getCondition(filter), filter.deduceAliasInjectionPoints(),
							toAliasTableMap(filter.aliases()), toAliasEntityMap(filter.aliases()));
				}
			}
		}
		FilterJoinTable simpleFilterJoinTable = property.getAnnotation( FilterJoinTable.class );
		if ( simpleFilterJoinTable != null ) {
			if ( hasAssociationTable ) {
				collection.addFilter(simpleFilterJoinTable.name(), simpleFilterJoinTable.condition(),
						simpleFilterJoinTable.deduceAliasInjectionPoints(),
						toAliasTableMap(simpleFilterJoinTable.aliases()), toAliasEntityMap(simpleFilterJoinTable.aliases()));
					}
			else {
				throw new AnnotationException(
						"Illegal use of @FilterJoinTable on an association without join table: "
								+ StringHelper.qualify( propertyHolder.getPath(), propertyName )
				);
			}
		}
		FilterJoinTables filterJoinTables = property.getAnnotation( FilterJoinTables.class );
		if ( filterJoinTables != null ) {
			for (FilterJoinTable filter : filterJoinTables.value()) {
				if ( hasAssociationTable ) {
					collection.addFilter(filter.name(), filter.condition(),
							filter.deduceAliasInjectionPoints(),
							toAliasTableMap(filter.aliases()), toAliasEntityMap(filter.aliases()));
				}
				else {
					throw new AnnotationException(
							"Illegal use of @FilterJoinTable on an association without join table: "
									+ StringHelper.qualify( propertyHolder.getPath(), propertyName )
					);
				}
			}
		}

		final boolean useEntityWhereClauseForCollections = ConfigurationHelper.getBoolean(
				AvailableSettings.USE_ENTITY_WHERE_CLAUSE_FOR_COLLECTIONS,
				buildingContext
						.getBuildingOptions()
						.getServiceRegistry()
						.getService( ConfigurationService.class )
						.getSettings(),
				true
		);

		// There are 2 possible sources of "where" clauses that apply to the associated entity table:
		// 1) from the associated entity mapping; i.e., @Entity @Where(clause="...")
		//    (ignored if useEntityWhereClauseForCollections == false)
		// 2) from the collection mapping;
		//    for one-to-many, e.g., @OneToMany @JoinColumn @Where(clause="...") public Set<Rating> getRatings();
		//    for many-to-many e.g., @ManyToMany @Where(clause="...") public Set<Rating> getRatings();
		String whereOnClassClause = null;
		if ( useEntityWhereClauseForCollections && property.getElementClass() != null ) {
			Where whereOnClass = property.getElementClass().getAnnotation( Where.class );
			if ( whereOnClass != null ) {
				whereOnClassClause = whereOnClass.clause();
			}
		}
		Where whereOnCollection = property.getAnnotation( Where.class );
		String whereOnCollectionClause = null;
		if ( whereOnCollection != null ) {
			whereOnCollectionClause = whereOnCollection.clause();
		}
		final String whereClause = StringHelper.getNonEmptyOrConjunctionIfBothNonEmpty(
				whereOnClassClause,
				whereOnCollectionClause
		);
		if ( hasAssociationTable ) {
			// A many-to-many association has an association (join) table
			// Collection#setManytoManyWhere is used to set the "where" clause that applies to
			// to the many-to-many associated entity table (not the join table).
			collection.setManyToManyWhere( whereClause );
		}
		else {
			// A one-to-many association does not have an association (join) table.
			// Collection#setWhere is used to set the "where" clause that applies to the collection table
			// (which is the associated entity table for a one-to-many association).
			collection.setWhere( whereClause );
		}

		WhereJoinTable whereJoinTable = property.getAnnotation( WhereJoinTable.class );
		String whereJoinTableClause = whereJoinTable == null ? null : whereJoinTable.clause();
		if ( StringHelper.isNotEmpty( whereJoinTableClause ) ) {
			if ( hasAssociationTable ) {
				// This is a many-to-many association.
				// Collection#setWhere is used to set the "where" clause that applies to the collection table
				// (which is the join table for a many-to-many association).
				collection.setWhere( whereJoinTableClause );
			}
			else {
				throw new AnnotationException(
						"Illegal use of @WhereJoinTable on an association without join table: "
								+ StringHelper.qualify( propertyHolder.getPath(), propertyName )
				);
			}
		}
//		This cannot happen in annotations since the second fetch is hardcoded to join
//		if ( ( ! collection.getManyToManyFilterMap().isEmpty() || collection.getManyToManyWhere() != null ) &&
//		        collection.getFetchMode() == FetchMode.JOIN &&
//		        collection.getElement().getFetchMode() != FetchMode.JOIN ) {
//			throw new MappingException(
//			        "association with join table  defining filter or where without join fetching " +
//			        "not valid within collection using join fetching [" + collection.getRole() + "]"
//				);
//		}
	}

	private String getCondition(Filter filter) {
		//set filtering
		String name = filter.name();
		String cond = filter.condition();
		return getCondition( cond, name );
	}

	private String getCondition(String cond, String name) {
		if ( BinderHelper.isEmptyAnnotationValue( cond ) ) {
			cond = buildingContext.getMetadataCollector().getFilterDefinition( name ).getDefaultFilterCondition();
			if ( StringHelper.isEmpty( cond ) ) {
				throw new AnnotationException(
						"no filter condition found for filter " + name + " in "
								+ StringHelper.qualify( propertyHolder.getPath(), propertyName )
				);
			}
		}
		return cond;
	}

	public void setCache(Cache cacheAnn) {
		if ( cacheAnn != null ) {
			cacheRegionName = BinderHelper.isEmptyAnnotationValue( cacheAnn.region() ) ? null : cacheAnn.region();
			cacheConcurrencyStrategy = EntityBinder.getCacheConcurrencyStrategy( cacheAnn.usage() );
		}
		else {
			cacheConcurrencyStrategy = null;
			cacheRegionName = null;
		}
	}

	public void setOneToMany(boolean oneToMany) {
		this.oneToMany = oneToMany;
	}

	public void setIndexColumn(IndexColumn indexColumn) {
		this.indexColumn = indexColumn;
	}

	public void setMapKey(MapKey key) {
		if ( key != null ) {
			mapKeyPropertyName = key.name();
		}
	}

	private static String buildOrderByClauseFromHql(String orderByFragment, PersistentClass associatedClass, String role) {
		if ( orderByFragment != null ) {
			if ( orderByFragment.length() == 0 ) {
				//order by id
				return buildOrderById( associatedClass, " asc" );
			}
			else if ( "desc".equals( orderByFragment ) ) {
				return buildOrderById( associatedClass, " desc" );
			}
		}
		return orderByFragment;
	}

	private static String buildOrderById(PersistentClass associatedClass, String order) {
		final StringBuilder sb = new StringBuilder();
		final Iterator<Selectable> columnIterator = associatedClass.getIdentifier().getColumnIterator();
		while ( columnIterator.hasNext() ) {
			final Selectable selectable = columnIterator.next();
			sb.append( selectable.getText() );
			sb.append( order );
			sb.append( ", " );
		}
		sb.setLength( sb.length() - 2 );
		return sb.toString();
	}

	public static String adjustUserSuppliedValueCollectionOrderingFragment(String orderByFragment) {
		if ( orderByFragment != null ) {
			orderByFragment = orderByFragment.trim();
			if ( orderByFragment.length() == 0 || orderByFragment.equalsIgnoreCase( "asc" ) ) {
				// This indicates something like either:
				//		`@OrderBy()`
				//		`@OrderBy("asc")
				//
				// JPA says this should indicate an ascending natural ordering of the elements - id for
				//		entity associations or the value(s) for "element collections"
				return "$element$ asc";
			}
			else if ( orderByFragment.equalsIgnoreCase( "desc" ) ) {
				// This indicates:
				//		`@OrderBy("desc")`
				//
				// JPA says this should indicate a descending natural ordering of the elements - id for
				//		entity associations or the value(s) for "element collections"
				return "$element$ desc";
			}
		}

		return orderByFragment;
	}

	private static SimpleValue buildCollectionKey(
			Collection collValue,
			AnnotatedJoinColumn[] joinColumns,
			boolean cascadeDeleteEnabled,
			boolean noConstraintByDefault,
			XProperty property,
			PropertyHolder propertyHolder,
			MetadataBuildingContext buildingContext) {
		//binding key reference using column
		KeyValue keyVal;
		//give a chance to override the referenced property name
		//has to do that here because the referencedProperty creation happens in a FKSecondPass for Many to one yuk!
		if ( joinColumns.length > 0 && StringHelper.isNotEmpty( joinColumns[0].getMappedBy() ) ) {
			String entityName = joinColumns[0].getManyToManyOwnerSideEntityName() != null ?
					"inverse__" + joinColumns[0].getManyToManyOwnerSideEntityName() :
					joinColumns[0].getPropertyHolder().getEntityName();
			String propRef = buildingContext.getMetadataCollector().getPropertyReferencedAssociation(
					entityName,
					joinColumns[0].getMappedBy()
			);
			if ( propRef != null ) {
				collValue.setReferencedPropertyName( propRef );
				buildingContext.getMetadataCollector().addPropertyReference( collValue.getOwnerEntityName(), propRef );
			}
		}
		String propRef = collValue.getReferencedPropertyName();
		if ( propRef == null ) {
			keyVal = collValue.getOwner().getIdentifier();
		}
		else {
			keyVal = (KeyValue) collValue.getOwner()
					.getReferencedProperty( propRef )
					.getValue();
		}
		DependantValue key = new DependantValue( buildingContext, collValue.getCollectionTable(), keyVal );
		key.setTypeName( null );
		AnnotatedColumn.checkPropertyConsistency( joinColumns, collValue.getOwnerEntityName() );
		key.setNullable( joinColumns.length == 0 || joinColumns[0].isNullable() );
		key.setUpdateable( joinColumns.length == 0 || joinColumns[0].isUpdatable() );
		key.setCascadeDeleteEnabled( cascadeDeleteEnabled );
		collValue.setKey( key );
		if ( property != null ) {
			final ForeignKey fk = property.getAnnotation( ForeignKey.class );
			if ( fk != null && !BinderHelper.isEmptyAnnotationValue( fk.name() ) ) {
				key.setForeignKeyName( fk.name() );
			}
			else {
				final CollectionTable collectionTableAnn = property.getAnnotation( CollectionTable.class );
				if ( collectionTableAnn != null ) {
					if ( collectionTableAnn.foreignKey().value() == ConstraintMode.NO_CONSTRAINT
							|| collectionTableAnn.foreignKey().value() == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) {
						key.setForeignKeyName( "none" );
					}
					else {
						key.setForeignKeyName( StringHelper.nullIfEmpty( collectionTableAnn.foreignKey().name() ) );
						key.setForeignKeyDefinition( StringHelper.nullIfEmpty( collectionTableAnn.foreignKey().foreignKeyDefinition() ) );
						if ( key.getForeignKeyName() == null &&
							key.getForeignKeyDefinition() == null &&
							collectionTableAnn.joinColumns().length == 1 ) {
							JoinColumn joinColumn = collectionTableAnn.joinColumns()[0];
							key.setForeignKeyName( StringHelper.nullIfEmpty( joinColumn.foreignKey().name() ) );
							key.setForeignKeyDefinition( StringHelper.nullIfEmpty( joinColumn.foreignKey().foreignKeyDefinition() ) );
						}
					}
				}
				else {
					final JoinTable joinTableAnn = property.getAnnotation( JoinTable.class );
					if ( joinTableAnn != null ) {
						String foreignKeyName = joinTableAnn.foreignKey().name();
						String foreignKeyDefinition = joinTableAnn.foreignKey().foreignKeyDefinition();
						ConstraintMode foreignKeyValue = joinTableAnn.foreignKey().value();
						if ( joinTableAnn.joinColumns().length != 0 ) {
							final JoinColumn joinColumnAnn = joinTableAnn.joinColumns()[0];
							if ( foreignKeyName != null && foreignKeyName.isEmpty() ) {
								foreignKeyName = joinColumnAnn.foreignKey().name();
								foreignKeyDefinition = joinColumnAnn.foreignKey().foreignKeyDefinition();
							}
							if ( foreignKeyValue != ConstraintMode.NO_CONSTRAINT ) {
								foreignKeyValue = joinColumnAnn.foreignKey().value();
							}
						}
						if ( foreignKeyValue == ConstraintMode.NO_CONSTRAINT
								|| foreignKeyValue == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) {
							key.setForeignKeyName( "none" );
						}
						else {
							key.setForeignKeyName( StringHelper.nullIfEmpty( foreignKeyName ) );
							key.setForeignKeyDefinition( StringHelper.nullIfEmpty( foreignKeyDefinition ) );
						}
					}
					else {
						final jakarta.persistence.ForeignKey fkOverride = propertyHolder.getOverriddenForeignKey(
								StringHelper.qualify( propertyHolder.getPath(), property.getName() )
						);
						if ( fkOverride != null && ( fkOverride.value() == ConstraintMode.NO_CONSTRAINT ||
								fkOverride.value() == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) ) {
							key.setForeignKeyName( "none" );
						}
						else if ( fkOverride != null ) {
							key.setForeignKeyName( StringHelper.nullIfEmpty( fkOverride.name() ) );
							key.setForeignKeyDefinition( StringHelper.nullIfEmpty( fkOverride.foreignKeyDefinition() ) );
						}
						else {
							final OneToMany oneToManyAnn = property.getAnnotation( OneToMany.class );
							final OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
							if ( oneToManyAnn != null && !oneToManyAnn.mappedBy().isEmpty()
									&& ( onDeleteAnn == null || onDeleteAnn.action() != OnDeleteAction.CASCADE ) ) {
								// foreign key should be up to @ManyToOne side
								// @OnDelete generate "on delete cascade" foreign key
								key.setForeignKeyName( "none" );
							}
							else {
								final JoinColumn joinColumnAnn = property.getAnnotation( JoinColumn.class );
								if ( joinColumnAnn != null ) {
									if ( joinColumnAnn.foreignKey().value() == ConstraintMode.NO_CONSTRAINT
											|| joinColumnAnn.foreignKey().value() == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) {
										key.setForeignKeyName( "none" );
									}
									else {
										key.setForeignKeyName( StringHelper.nullIfEmpty( joinColumnAnn.foreignKey().name() ) );
										key.setForeignKeyDefinition( StringHelper.nullIfEmpty( joinColumnAnn.foreignKey().foreignKeyDefinition() ) );
									}
								}
							}
						}
					}
				}
			}
		}

		return key;
	}

	private void bindManyToManySecondPass(
			Collection collValue,
			Map<String, PersistentClass> persistentClasses,
			AnnotatedJoinColumn[] joinColumns,
			AnnotatedJoinColumn[] inverseJoinColumns,
			AnnotatedColumn[] elementColumns,
			boolean isEmbedded,
			XClass collType,
			boolean ignoreNotFound, boolean unique,
			boolean cascadeDeleteEnabled,
			TableBinder associationTableBinder,
			XProperty property,
			PropertyHolder parentPropertyHolder,
			MetadataBuildingContext buildingContext) throws MappingException {
		if ( property == null ) {
			throw new IllegalArgumentException( "null was passed for argument property" );
		}

		final PersistentClass collectionEntity = persistentClasses.get( collType.getName() );
		final String hqlOrderBy = extractHqlOrderBy( jpaOrderBy );

		boolean isCollectionOfEntities = collectionEntity != null;
		ManyToAny anyAnn = property.getAnnotation( ManyToAny.class );
		if ( LOG.isDebugEnabled() ) {
			String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
			if ( isCollectionOfEntities && unique ) {
				LOG.debugf("Binding a OneToMany: %s through an association table", path);
			}
			else if (isCollectionOfEntities) {
				LOG.debugf("Binding a ManyToMany: %s", path);
			}
			else if (anyAnn != null) {
				LOG.debugf("Binding a ManyToAny: %s", path);
			}
			else {
				LOG.debugf("Binding a collection of element: %s", path);
			}
		}
		//check for user error
		if ( !isCollectionOfEntities ) {
			if ( property.isAnnotationPresent( ManyToMany.class ) || property.isAnnotationPresent( OneToMany.class ) ) {
				String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
				throw new AnnotationException(
						"Use of @OneToMany or @ManyToMany targeting an unmapped class: " + path + "[" + collType + "]"
				);
			}
			else if ( anyAnn != null ) {
				if ( parentPropertyHolder.getJoinTable( property ) == null ) {
					String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
					throw new AnnotationException(
							"@JoinTable is mandatory when @ManyToAny is used: " + path
					);
				}
			}
			else {
				JoinTable joinTableAnn = parentPropertyHolder.getJoinTable( property );
				if ( joinTableAnn != null && joinTableAnn.inverseJoinColumns().length > 0 ) {
					String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
					throw new AnnotationException(
							"Use of @JoinTable.inverseJoinColumns targeting an unmapped class: " + path + "[" + collType + "]"
					);
				}
			}
		}

		boolean mappedBy = !BinderHelper.isEmptyAnnotationValue( joinColumns[0].getMappedBy() );
		if ( mappedBy ) {
			if ( !isCollectionOfEntities ) {
				throw new AnnotationException(
						"Collection of elements must not have mappedBy or association reference an unmapped entity: " +
								collValue.getOwnerEntityName() +
								"." +
								joinColumns[0].getPropertyName()
				);
			}
			Property otherSideProperty;
			try {
				otherSideProperty = collectionEntity.getRecursiveProperty( joinColumns[0].getMappedBy() );
			}
			catch (MappingException e) {
				throw new AnnotationException(
						"mappedBy references an unknown target entity property: "
								+ collType + "." + joinColumns[0].getMappedBy() + " in "
								+ collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName()
				);
			}
			Table table;
			if ( otherSideProperty.getValue() instanceof Collection ) {
				//this is a collection on the other side
				table = ( (Collection) otherSideProperty.getValue() ).getCollectionTable();
			}
			else {
				//This is a ToOne with a @JoinTable or a regular property
				table = otherSideProperty.getValue().getTable();
			}
			collValue.setCollectionTable( table );
			String entityName = collectionEntity.getEntityName();
			for (AnnotatedJoinColumn column : joinColumns) {
				//column.setDefaultColumnHeader( joinColumns[0].getMappedBy() ); //seems not to be used, make sense
				column.setManyToManyOwnerSideEntityName( entityName );
			}
		}
		else {
			//TODO: only for implicit columns?
			//FIXME NamingStrategy
			for (AnnotatedJoinColumn column : joinColumns) {
				String mappedByProperty = buildingContext.getMetadataCollector().getFromMappedBy(
						collValue.getOwnerEntityName(), column.getPropertyName()
				);
				Table ownerTable = collValue.getOwner().getTable();
				column.setMappedBy(
						collValue.getOwner().getEntityName(),
						collValue.getOwner().getJpaEntityName(),
						buildingContext.getMetadataCollector().getLogicalTableName( ownerTable ),
						mappedByProperty
				);
//				String header = ( mappedByProperty == null ) ? mappings.getLogicalTableName( ownerTable ) : mappedByProperty;
//				column.setDefaultColumnHeader( header );
			}
			if ( StringHelper.isEmpty( associationTableBinder.getName() ) ) {
				//default value
				associationTableBinder.setDefaultName(
						collValue.getOwner().getClassName(),
						collValue.getOwner().getEntityName(),
						collValue.getOwner().getJpaEntityName(),
						buildingContext.getMetadataCollector().getLogicalTableName( collValue.getOwner().getTable() ),
						collectionEntity != null ? collectionEntity.getClassName() : null,
						collectionEntity != null ? collectionEntity.getEntityName() : null,
						collectionEntity != null ? collectionEntity.getJpaEntityName() : null,
						collectionEntity != null ? buildingContext.getMetadataCollector().getLogicalTableName(
								collectionEntity.getTable()
						) : null,
						joinColumns[0].getPropertyName()
				);
			}
			associationTableBinder.setJPA2ElementCollection( !isCollectionOfEntities && property.isAnnotationPresent( ElementCollection.class ));
			collValue.setCollectionTable( associationTableBinder.bind() );
		}
		bindFilters( isCollectionOfEntities );
		bindCollectionSecondPass( collValue, collectionEntity, joinColumns, cascadeDeleteEnabled, property, propertyHolder, buildingContext );

		ManyToOne element = null;
		if ( isCollectionOfEntities ) {
			element = new ManyToOne( buildingContext,  collValue.getCollectionTable() );
			collValue.setElement( element );
			element.setReferencedEntityName( collType.getName() );
			//element.setFetchMode( fetchMode );
			//element.setLazy( fetchMode != FetchMode.JOIN );
			//make the second join non lazy
			element.setFetchMode( FetchMode.JOIN );
			element.setLazy( false );
			element.setIgnoreNotFound( ignoreNotFound );
			// as per 11.1.38 of JPA 2.0 spec, default to primary key if no column is specified by @OrderBy.
			if ( hqlOrderBy != null ) {
				collValue.setManyToManyOrdering(
						buildOrderByClauseFromHql( hqlOrderBy, collectionEntity, collValue.getRole() )
				);
			}

			final ForeignKey fk = property.getAnnotation( ForeignKey.class );
			if ( fk != null && !BinderHelper.isEmptyAnnotationValue( fk.name() ) ) {
				element.setForeignKeyName( fk.name() );
			}
			else {
				final JoinTable joinTableAnn = property.getAnnotation( JoinTable.class );
				if ( joinTableAnn != null ) {
					String foreignKeyName = joinTableAnn.inverseForeignKey().name();
					String foreignKeyDefinition = joinTableAnn.inverseForeignKey().foreignKeyDefinition();
					ConstraintMode foreignKeyValue = joinTableAnn.inverseForeignKey().value();
					if ( joinTableAnn.inverseJoinColumns().length != 0 ) {
						final JoinColumn joinColumnAnn = joinTableAnn.inverseJoinColumns()[0];
						if ( foreignKeyName != null && foreignKeyName.isEmpty() ) {
							foreignKeyName = joinColumnAnn.foreignKey().name();
							foreignKeyDefinition = joinColumnAnn.foreignKey().foreignKeyDefinition();
						}
						if ( foreignKeyValue != ConstraintMode.NO_CONSTRAINT ) {
							foreignKeyValue = joinColumnAnn.foreignKey().value();
						}
					}
					if ( joinTableAnn.inverseForeignKey().value() == ConstraintMode.NO_CONSTRAINT
							|| joinTableAnn.inverseForeignKey().value() == ConstraintMode.PROVIDER_DEFAULT && buildingContext.getBuildingOptions().isNoConstraintByDefault() ) {
						element.setForeignKeyName( "none" );
					}
					else {
						element.setForeignKeyName( StringHelper.nullIfEmpty( foreignKeyName ) );
						element.setForeignKeyDefinition( StringHelper.nullIfEmpty( foreignKeyDefinition ) );
					}
				}
			}
		}
		else if ( anyAnn != null ) {
			//@ManyToAny
			//Make sure that collTyp is never used during the @ManyToAny branch: it will be set to void.class
			final PropertyData inferredData = new PropertyInferredData(
					null,
					property,
					"unsupported",
					buildingContext.getBootstrapContext().getReflectionManager()
			);

			final jakarta.persistence.Column discriminatorColumnAnn = inferredData.getProperty().getAnnotation( jakarta.persistence.Column.class );
			final Formula discriminatorFormulaAnn = inferredData.getProperty().getAnnotation( Formula.class );

			//override the table
			for (AnnotatedColumn column : inverseJoinColumns) {
				column.setTable( collValue.getCollectionTable() );
			}

			final Any any = BinderHelper.buildAnyValue(
					discriminatorColumnAnn,
					discriminatorFormulaAnn,
					inverseJoinColumns,
					inferredData,
					cascadeDeleteEnabled,
					anyAnn.fetch() == FetchType.LAZY,
					Nullability.NO_CONSTRAINT,
					propertyHolder,
					new EntityBinder(),
					true,
					buildingContext
			);
			collValue.setElement( any );
		}
		else {
			XClass elementClass;
			AnnotatedClassType classType;

			CollectionPropertyHolder holder;
			if ( BinderHelper.PRIMITIVE_NAMES.contains( collType.getName() ) ) {
				classType = AnnotatedClassType.NONE;
				elementClass = null;

				holder = PropertyHolderBuilder.buildPropertyHolder(
						collValue,
						collValue.getRole(),
						null,
						property,
						parentPropertyHolder,
						buildingContext
				);
			}
			else {
				elementClass = collType;
				classType = buildingContext.getMetadataCollector().getClassType( elementClass );

				holder = PropertyHolderBuilder.buildPropertyHolder(
						collValue,
						collValue.getRole(),
						elementClass,
						property,
						parentPropertyHolder,
						buildingContext
				);

				// 'parentPropertyHolder' is the PropertyHolder for the owner of the collection
				// 'holder' is the CollectionPropertyHolder.
				// 'property' is the collection XProperty
				parentPropertyHolder.startingProperty( property );

				//force in case of attribute override
				boolean attributeOverride = property.isAnnotationPresent( AttributeOverride.class )
						|| property.isAnnotationPresent( AttributeOverrides.class );
				// todo : force in the case of Convert annotation(s) with embedded paths (beyond key/value prefixes)?
				if ( isEmbedded || attributeOverride ) {
					classType = AnnotatedClassType.EMBEDDABLE;
				}
			}

			if ( AnnotatedClassType.EMBEDDABLE.equals( classType ) ) {
				holder.prepare( property );

				EntityBinder entityBinder = new EntityBinder();
				PersistentClass owner = collValue.getOwner();

				final AccessType baseAccessType;
				final Access accessAnn = property.getAnnotation( Access.class );
				if ( accessAnn != null ) {
					// the attribute is locally annotated with `@Access`, use that
					baseAccessType = accessAnn.value() == PROPERTY
							? AccessType.PROPERTY
							: AccessType.FIELD;
				}
				else if ( owner.getIdentifierProperty() != null ) {
					// use the access for the owning entity's id attribute, if one
					baseAccessType = owner.getIdentifierProperty().getPropertyAccessorName().equals( "property" )
							? AccessType.PROPERTY
							: AccessType.FIELD;
				}
				else if ( owner.getIdentifierMapper() != null && owner.getIdentifierMapper().getPropertySpan() > 0 ) {
					// use the access for the owning entity's "id mapper", if one
					Property prop = owner.getIdentifierMapper().getPropertyIterator().next();
					baseAccessType = prop.getPropertyAccessorName().equals( "property" )
							? AccessType.PROPERTY
							: AccessType.FIELD;
				}
				else {
					// otherwise...
					throw new AssertionFailure( "Unable to guess collection property accessor name" );
				}

				PropertyData inferredData;
				if ( isMap() ) {
					//"value" is the JPA 2 prefix for map values (used to be "element")
					if ( isHibernateExtensionMapping() ) {
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "element", elementClass );
					}
					else {
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "value", elementClass );
					}
				}
				else {
					if ( isHibernateExtensionMapping() ) {
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "element", elementClass );
					}
					else {
						//"collection&&element" is not a valid property name => placeholder
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "collection&&element", elementClass );
					}
				}

				//TODO be smart with isNullable
				boolean isNullable = true;
				Component component = AnnotationBinder.fillComponent(
						holder,
						inferredData,
						baseAccessType,
						isNullable,
						entityBinder,
						false,
						false,
						true,
						resolveCustomInstantiator( property, elementClass, buildingContext ),
						buildingContext,
						inheritanceStatePerClass
				);

				collValue.setElement( component );

				if ( StringHelper.isNotEmpty( hqlOrderBy ) ) {
					String orderBy = adjustUserSuppliedValueCollectionOrderingFragment( hqlOrderBy );
					if ( orderBy != null ) {
						collValue.setOrderBy( orderBy );
					}
				}
			}
			else {
				holder.prepare( property );

				final BasicValueBinder elementBinder = new BasicValueBinder( BasicValueBinder.Kind.COLLECTION_ELEMENT, buildingContext );
				elementBinder.setReturnedClassName( collType.getName() );
				if ( elementColumns == null || elementColumns.length == 0 ) {
					elementColumns = new AnnotatedColumn[1];
					AnnotatedColumn column = new AnnotatedColumn();
					column.setImplicit( false );
					//not following the spec but more clean
					column.setNullable( true );
					column.setLogicalColumnName( Collection.DEFAULT_ELEMENT_COLUMN_NAME );
					//TODO create an EMPTY_JOINS collection
					column.setJoins( new HashMap<>() );
					column.setBuildingContext( buildingContext );
					column.bind();
					elementColumns[0] = column;
				}
				//override the table
				for (AnnotatedColumn column : elementColumns) {
					column.setTable( collValue.getCollectionTable() );
				}
				elementBinder.setColumns( elementColumns );
				elementBinder.setType(
						property,
						elementClass,
						collValue.getOwnerEntityName(),
						holder.resolveElementAttributeConverterDescriptor( property, elementClass )
				);
				elementBinder.setPersistentClassName( propertyHolder.getEntityName() );
				elementBinder.setAccessType( accessType );
				collValue.setElement( elementBinder.make() );
				String orderBy = adjustUserSuppliedValueCollectionOrderingFragment( hqlOrderBy );
				if ( orderBy != null ) {
					collValue.setOrderBy( orderBy );
				}
			}
		}

		checkFilterConditions( collValue );

		//FIXME: do optional = false
		if ( isCollectionOfEntities ) {
			bindManytoManyInverseFk( collectionEntity, inverseJoinColumns, element, unique, buildingContext );
		}

	}

	private Class<? extends EmbeddableInstantiator> resolveCustomInstantiator(
			XProperty property,
			XClass propertyClass,
			MetadataBuildingContext context) {
		final org.hibernate.annotations.EmbeddableInstantiator propertyAnnotation = property.getAnnotation( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( propertyAnnotation != null ) {
			return propertyAnnotation.value();
		}

		final org.hibernate.annotations.EmbeddableInstantiator classAnnotation = propertyClass.getAnnotation( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( classAnnotation != null ) {
			return classAnnotation.value();
		}

		final Class<?> embeddableClass = context.getBootstrapContext().getReflectionManager().toClass( propertyClass );
		if ( embeddableClass != null ) {
			return context.getMetadataCollector().findRegisteredEmbeddableInstantiator( embeddableClass );
		}

		return null;
	}

	private String extractHqlOrderBy(jakarta.persistence.OrderBy jpaOrderBy) {
		if ( jpaOrderBy != null ) {
			return jpaOrderBy.value(); // Null not possible. In case of empty expression, apply default ordering.
		}
		return null; // @OrderBy not found.
	}

	private static void checkFilterConditions(Collection collValue) {
		//for now it can't happen, but sometime soon...
		if ( ( collValue.getFilters().size() != 0 || StringHelper.isNotEmpty( collValue.getWhere() ) ) &&
				collValue.getFetchMode() == FetchMode.JOIN &&
				!( collValue.getElement() instanceof SimpleValue ) && //SimpleValue (CollectionOfElements) are always SELECT but it does not matter
				collValue.getElement().getFetchMode() != FetchMode.JOIN ) {
			throw new MappingException(
					"@ManyToMany or @ElementCollection defining filter or where without join fetching "
							+ "not valid within collection using join fetching[" + collValue.getRole() + "]"
			);
		}
	}

	private static void bindCollectionSecondPass(
			Collection collValue,
			PersistentClass collectionEntity,
			AnnotatedJoinColumn[] joinColumns,
			boolean cascadeDeleteEnabled,
			XProperty property,
			PropertyHolder propertyHolder,
			MetadataBuildingContext buildingContext) {
		try {
			BinderHelper.createSyntheticPropertyReference(
					joinColumns,
					collValue.getOwner(),
					collectionEntity,
					collValue,
					false,
					buildingContext
			);
		}
		catch (AnnotationException ex) {
			throw new AnnotationException( "Unable to map collection " + collValue.getOwner().getClassName() + "." + property.getName(), ex );
		}
		SimpleValue key = buildCollectionKey( collValue, joinColumns, cascadeDeleteEnabled,
				buildingContext.getBuildingOptions().isNoConstraintByDefault(), property, propertyHolder, buildingContext );
		if ( property.isAnnotationPresent( ElementCollection.class ) && joinColumns.length > 0 ) {
			joinColumns[0].setJPA2ElementCollection( true );
		}
		TableBinder.bindFk( collValue.getOwner(), collectionEntity, joinColumns, key, false, buildingContext );
	}

	public void setCascadeDeleteEnabled(boolean onDeleteCascade) {
		this.cascadeDeleteEnabled = onDeleteCascade;
	}

	private String safeCollectionRole() {
		if ( propertyHolder != null ) {
			return propertyHolder.getEntityName() + "." + propertyName;
		}
		else {
			return "";
		}
	}


	/**
	 * bind the inverse FK of a ManyToMany
	 * If we are in a mappedBy case, read the columns from the associated
	 * collection element
	 * Otherwise delegates to the usual algorithm
	 */
	public static void bindManytoManyInverseFk(
			PersistentClass referencedEntity,
			AnnotatedJoinColumn[] columns,
			SimpleValue value,
			boolean unique,
			MetadataBuildingContext buildingContext) {
		final String mappedBy = columns[0].getMappedBy();
		if ( StringHelper.isNotEmpty( mappedBy ) ) {
			final Property property = referencedEntity.getRecursiveProperty( mappedBy );
			Iterator<Selectable> mappedByColumns;
			if ( property.getValue() instanceof Collection ) {
				mappedByColumns = ( (Collection) property.getValue() ).getKey().getColumnIterator();
			}
			else {
				//find the appropriate reference key, can be in a join
				Iterator<Join> joinsIt = referencedEntity.getJoinIterator();
				KeyValue key = null;
				while ( joinsIt.hasNext() ) {
					Join join = (Join) joinsIt.next();
					if ( join.containsProperty( property ) ) {
						key = join.getKey();
						break;
					}
				}
				if ( key == null ) {
					key = property.getPersistentClass().getIdentifier();
				}
				mappedByColumns = key.getColumnIterator();
			}
			while ( mappedByColumns.hasNext() ) {
				Column column = (Column) mappedByColumns.next();
				columns[0].linkValueUsingAColumnCopy( column, value );
			}
			String referencedPropertyName =
					buildingContext.getMetadataCollector().getPropertyReferencedAssociation(
							"inverse__" + referencedEntity.getEntityName(), mappedBy
					);
			if ( referencedPropertyName != null ) {
				//TODO always a many to one?
				( (ManyToOne) value ).setReferencedPropertyName( referencedPropertyName );
				buildingContext.getMetadataCollector().addUniquePropertyReference(
						referencedEntity.getEntityName(),
						referencedPropertyName
				);
			}
			( (ManyToOne) value ).setReferenceToPrimaryKey( referencedPropertyName == null );
			value.createForeignKey();
		}
		else {
			BinderHelper.createSyntheticPropertyReference( columns, referencedEntity, null, value, true, buildingContext );
			TableBinder.bindFk( referencedEntity, null, columns, value, unique, buildingContext );
		}
	}

	public void setFkJoinColumns(AnnotatedJoinColumn[] annotatedJoinColumns) {
		this.fkJoinColumns = annotatedJoinColumns;
	}

	public void setExplicitAssociationTable(boolean explicitAssocTable) {
		this.isExplicitAssociationTable = explicitAssocTable;
	}

	public void setElementColumns(AnnotatedColumn[] elementColumns) {
		this.elementColumns = elementColumns;
	}

	public void setEmbedded(boolean annotationPresent) {
		this.isEmbedded = annotationPresent;
	}

	public void setProperty(XProperty property) {
		this.property = property;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

	public void setMapKeyColumns(AnnotatedColumn[] mapKeyColumns) {
		this.mapKeyColumns = mapKeyColumns;
	}

	public void setMapKeyManyToManyColumns(AnnotatedJoinColumn[] mapJoinColumns) {
		this.mapKeyManyToManyColumns = mapJoinColumns;
	}

	public void setLocalGenerators(HashMap<String, IdentifierGeneratorDefinition> localGenerators) {
		this.localGenerators = localGenerators;
	}
}
