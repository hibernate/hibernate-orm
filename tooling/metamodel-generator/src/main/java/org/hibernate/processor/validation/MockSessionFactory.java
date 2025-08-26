/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.validation;

import jakarta.persistence.metamodel.Bindable;
import org.jspecify.annotations.Nullable;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.MappingException;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.boot.internal.DefaultCustomEntityDirtinessStrategy;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.internal.StandardEntityNotFoundDelegate;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.registry.selector.internal.StrategySelectorImpl;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.internal.DisabledCaching;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.MutableJpaComplianceImpl;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.AbstractPluralAttribute;
import org.hibernate.metamodel.model.domain.internal.BagAttributeImpl;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.BasicTypeImpl;
import org.hibernate.metamodel.model.domain.internal.EmbeddableTypeImpl;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;
import org.hibernate.metamodel.model.domain.internal.JpaMetamodelImpl;
import org.hibernate.metamodel.model.domain.internal.ListAttributeImpl;
import org.hibernate.metamodel.model.domain.internal.MapAttributeImpl;
import org.hibernate.metamodel.model.domain.internal.MappedSuperclassTypeImpl;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.metamodel.model.domain.internal.PluralAttributeBuilder;
import org.hibernate.metamodel.model.domain.internal.SetAttributeImpl;
import org.hibernate.metamodel.model.domain.internal.SingularAttributeImpl;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.hql.internal.StandardHqlTranslator;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.internal.NamedObjectRepositoryImpl;
import org.hibernate.query.internal.QueryInterpretationCacheDisabledImpl;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.sql.internal.SqlTranslationEngineImpl;
import org.hibernate.query.sql.spi.SqlTranslationEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.query.sqm.tree.domain.SqmPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmSingularPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddableDomainType;
import org.hibernate.stat.internal.StatisticsImpl;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.BagType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.ListType;
import org.hibernate.type.MapType;
import org.hibernate.type.SetType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

/**
 * @author Gavin King
 */
@SuppressWarnings({"nullness", "initialization"})
public abstract class MockSessionFactory
		implements SessionFactoryImplementor, SessionFactoryOptions, QueryEngine, FunctionContributions,
		MetadataBuildingOptions, MetadataBuildingContext, RuntimeModelCreationContext, BootstrapContext,
		JdbcTypeIndicators, RuntimeMetamodelsImplementor {

	private static final BasicTypeImpl<Object> OBJECT_BASIC_TYPE =
			new BasicTypeImpl<>(new UnknownBasicJavaType<>(Object.class), ObjectJdbcType.INSTANCE);

	private final TypeConfiguration typeConfiguration;

	private final Map<String,MockEntityPersister> entityPersistersByName = new HashMap<>();
	private final Map<String,MockCollectionPersister> collectionPersistersByName = new HashMap<>();

	private final StandardServiceRegistryImpl serviceRegistry;
	private final SqmFunctionRegistry functionRegistry;
	private final MappingMetamodelImpl metamodel;

	private final MetadataImplementor bootModel;
//	private final MetadataContext metadataContext;

	private final NodeBuilder nodeBuilder;
	private final SqlTranslationEngine sqlTranslationEngine;

	private final ClassLoaderServiceImpl classLoaderService;

	public MockSessionFactory() {
		classLoaderService = new ClassLoaderServiceImpl() {
			@Override
			@SuppressWarnings("unchecked")
			public Class<?> classForName(String className) {
				try {
					return super.classForName( className );
				}
				catch (ClassLoadingException e) {
					if ( isClassDefined( className ) ) {
						return Object[].class;
					}
					else {
						throw e;
					}
				}
			}
		};
		serviceRegistry = StandardServiceRegistryImpl.create(
				new BootstrapServiceRegistryImpl(
						true,
						classLoaderService,
						new StrategySelectorImpl( classLoaderService ),
						Collections::emptyList
				),
//				new BootstrapServiceRegistryBuilder().applyClassLoaderService( classLoaderService ).build(),
				singletonList(MockJdbcServicesInitiator.INSTANCE),
				emptyList(),
				emptyMap()
		);

		functionRegistry = new SqmFunctionRegistry();
		metamodel = new MockMappingMetamodelImpl();

		bootModel = new MetadataImpl(
				UUID.randomUUID(),
				this,
				emptyMap(),
				emptyList(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				emptyMap(),
				new Database(this, MockJdbcServicesInitiator.jdbcServices.getJdbcEnvironment()),
				this
		);

//		metadataContext = new MetadataContext(
//				metamodel.getJpaMetamodel(),
//				metamodel,
//				bootModel,
//				JpaStaticMetamodelPopulationSetting.DISABLED,
//				JpaMetamodelPopulationSetting.DISABLED,
//				this,
//				classLoaderService
//		);

		typeConfiguration = new TypeConfiguration();
		typeConfiguration.scope((MetadataBuildingContext) this);
		MockJdbcServicesInitiator.genericDialect.initializeFunctionRegistry(this);
		CommonFunctionFactory functionFactory = new CommonFunctionFactory(this);
		functionFactory.listagg(null);
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();
		functionFactory.windowFunctions();
		typeConfiguration.scope((SessionFactoryImplementor) this);

		nodeBuilder = new SqmCriteriaNodeBuilder("", "", this, this, this);

		sqlTranslationEngine = new SqlTranslationEngineImpl(this, typeConfiguration, emptyMap() );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	@Override
	public void addObserver(SessionFactoryObserver observer) {
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return this;
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return new PhysicalNamingStrategyStandardImpl();
	}

	@Override
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return new ImplicitNamingStrategyJpaCompliantImpl();
	}

	static CollectionType createCollectionType(String role, String name) {
		return switch ( name ) {
			case "Set", "SortedSet" ->
				//might actually be a bag!
				//TODO: look for @OrderColumn on the property
					new SetType( role, null );
			case "List", "SortedList" -> new ListType( role, null );
			case "Map", "SortedMap" -> new MapType( role, null );
			default -> new BagType( role, null );
		};
	}

	/**
	 * Lazily create a {@link MockEntityPersister}
	 */
	abstract MockEntityPersister createMockEntityPersister(String entityName);

	/**
	 * Lazily create a {@link MockCollectionPersister}
	 */
	abstract MockCollectionPersister createMockCollectionPersister(String role);

	abstract boolean isEntityDefined(String jpaEntityName);

	abstract String qualifyName(String jpaEntityName);

	abstract boolean isAttributeDefined(String entityName, String fieldName);

	abstract boolean isClassDefined(String qualifiedName);

	protected abstract boolean isEntity(String entityName);

	abstract boolean isEnum(String className);

	abstract boolean isEnumConstant(String className, String terminal);

	abstract Class<?> javaConstantType(String className, String fieldName);

	abstract boolean isFieldDefined(String qualifiedClassName, String fieldName);

	abstract boolean isConstructorDefined(String qualifiedClassName, List<Type> argumentTypes);

	abstract Type propertyType(String typeName, String propertyPath);

	protected abstract boolean isSubtype(String entityName, String subtypeEntityName);

	protected abstract String getSupertype(String entityName);

	private EntityPersister createEntityPersister(String entityName) {
		MockEntityPersister result = entityPersistersByName.get(entityName);
		if (result!=null) {
			return result;
		}
		result = createMockEntityPersister(entityName);
		entityPersistersByName.put(entityName, result);
		return result;
	}

	private CollectionPersister createCollectionPersister(String entityName) {
		MockCollectionPersister result = collectionPersistersByName.get(entityName);
		if (result!=null) {
			return result;
		}
		result = createMockCollectionPersister(entityName);
		collectionPersistersByName.put(entityName, result);
		return result;
	}

	List<MockEntityPersister> getMockEntityPersisters() {
		return entityPersistersByName.values()
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@Override
	public Type getIdentifierType(String className)
			throws MappingException {
		return createEntityPersister(className)
				.getIdentifierType();
	}

	public BasicType<?> getVersionType(String className)
			throws MappingException {
		return createEntityPersister(className)
				.getVersionType();
	}

	@Override
	public String getIdentifierPropertyName(String className)
			throws MappingException {
		return createEntityPersister(className)
				.getIdentifierPropertyName();
	}

	@Override
	public Type getReferencedPropertyType(String className, String propertyName)
			throws MappingException {
		return createEntityPersister(className)
				.getPropertyType(propertyName);
	}

	@Override
	public MappingMetamodelImplementor getMetamodel() {
		return metamodel;
	}

	@Override
	public StandardServiceRegistryImpl getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public JdbcServices getJdbcServices() {
		return MockJdbcServicesInitiator.jdbcServices;
//		return serviceRegistry.getService(JdbcServices.class);
	}

	@Override
	public String getName() {
		return "mock";
	}

	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		return this;
	}

	@Override
	public Set<String> getDefinedFilterNames() {
		return emptySet();
	}

	@Override
	public CacheImplementor getCache() {
		return new DisabledCaching(this);
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return new StandardEntityNotFoundDelegate();
	}

	@Override
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return new DefaultCustomEntityDirtinessStrategy();
	}

	@Override
	public CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver() {
		return null;
	}

	@Override
	public JavaType<Object> getTenantIdentifierJavaType() {
		return null;
	}

	@Override
	public boolean isPreferJavaTimeJdbcTypesEnabled() {
		return MetadataBuildingContext.super.isPreferJavaTimeJdbcTypesEnabled();
	}

	@Override
	public boolean isPreferNativeEnumTypesEnabled() {
		return MetadataBuildingContext.super.isPreferNativeEnumTypesEnabled();
	}

	@Override
	public boolean isXmlFormatMapperLegacyFormatEnabled() {
		return false;
	}

	@Override
	public ValueHandlingMode getCriteriaValueHandlingMode() {
		return ValueHandlingMode.BIND;
	}

	@Override
	public void close() {}

	@Override
	public RootGraphImplementor<?> findEntityGraphByName(String s) {
		throw new UnsupportedOperationException("operation not supported");
	}

	static Class<?> toPrimitiveClass(Class<?> type) {
		return switch ( type.getName() ) {
			case "java.lang.Boolean" -> boolean.class;
			case "java.lang.Character" -> char.class;
			case "java.lang.Integer" -> int.class;
			case "java.lang.Short" -> short.class;
			case "java.lang.Byte" -> byte.class;
			case "java.lang.Long" -> long.class;
			case "java.lang.Float" -> float.class;
			case "java.lang.Double" -> double.class;
			default -> Object.class;
		};
	}

	@Override
	public NativeQueryInterpreter getNativeQueryInterpreter() {
		return new NativeQueryInterpreterStandardImpl( this.getNativeJdbcParametersIgnored() );
	}

	@Override
	public QueryInterpretationCache getInterpretationCache() {
		return new QueryInterpretationCacheDisabledImpl( serviceRegistry );
	}

	@Override
	public StatisticsImplementor getStatistics() {
		return new StatisticsImpl(this);
	}

	@Override
	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return functionRegistry;
	}

	@Override
	public NodeBuilder getCriteriaBuilder() {
		return nodeBuilder;
	}

	@Override
	public void validateNamedQueries() {
	}

	@Override
	public NamedObjectRepository getNamedObjectRepository() {
		return new NamedObjectRepositoryImpl(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
	}

	@Override
	public HqlTranslator getHqlTranslator() {
		return new StandardHqlTranslator(nodeBuilder, new SqmCreationOptions() {});
	}

	@Override
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return new StandardSqmTranslatorFactory();
	}

	@Override
	public QueryEngine getQueryEngine() {
		return this;
	}

	@Override
	public SqlTranslationEngine getSqlTranslationEngine() {
		return sqlTranslationEngine;
	}

	@Override
	public JpaMetamodelImplementor getJpaMetamodel() {
		return metamodel.getJpaMetamodel();
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return metamodel;
	}

	@Override
	public RuntimeMetamodelsImplementor getRuntimeMetamodels() {
		return this;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	private static final SessionFactoryObserver[] NO_OBSERVERS = new SessionFactoryObserver[0];
	private static final EntityNameResolver[] NO_RESOLVERS = new EntityNameResolver[0];

	static MutableJpaCompliance jpaCompliance = new MutableJpaComplianceImpl(emptyMap());

	@Override
	public MutableJpaCompliance getJpaCompliance() {
		return jpaCompliance;
	}

	@Override
	public String getSessionFactoryName() {
		return "mock";
	}

	@Override
	public String getUuid() {
		return "mock";
	}

	@Override
	public SessionFactoryObserver[] getSessionFactoryObservers() {
		return NO_OBSERVERS;
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		return NO_RESOLVERS;
	}

	@Override
	public boolean isDelayBatchFetchLoaderCreationsEnabled() {
		return false;
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return null;
	}

	@Override
	public boolean allowImmutableEntityUpdate() {
		return true;
	}

	@Override
	public ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode() {
		return ImmutableEntityUpdateQueryHandlingMode.ALLOW;
	}

	@Override
	public void setCheckNullability(boolean enabled) {}

	private static class MockMappingDefaults implements MappingDefaults {
		@Override
		public String getImplicitSchemaName() {
			return null;
		}

		@Override
		public String getImplicitCatalogName() {
			return null;
		}

		@Override
		public boolean shouldImplicitlyQuoteIdentifiers() {
			return false;
		}

		@Override
		public String getImplicitIdColumnName() {
			return null;
		}

		@Override
		public String getImplicitTenantIdColumnName() {
			return null;
		}

		@Override
		public String getImplicitDiscriminatorColumnName() {
			return null;
		}

		@Override
		public String getImplicitPackageName() {
			return null;
		}

		@Override
		public boolean isAutoImportEnabled() {
			return false;
		}

		@Override
		public String getImplicitCascadeStyleName() {
			return null;
		}

		@Override
		public String getImplicitPropertyAccessorName() {
			return null;
		}

		@Override
		public boolean areEntitiesImplicitlyLazy() {
			return false;
		}

		@Override
		public boolean areCollectionsImplicitlyLazy() {
			return false;
		}

		@Override
		public AccessType getImplicitCacheAccessType() {
			return null;
		}

		@Override
		public CollectionClassification getImplicitListClassification() {
			return null;
		}
	}

	@Override
	public Dialect getDialect() {
		return MockJdbcServicesInitiator.genericDialect;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return SqlTypes.BOOLEAN;
	}

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return SqlTypes.NUMERIC;
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return SqlTypes.UUID;
	}

	@Override
	public int getPreferredSqlTypeCodeForInstant() {
		return SqlTypes.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return SqlTypes.ARRAY;
	}

	private static class MockJavaType<X> implements BasicJavaType<X> {
		private final String typeName;

		public MockJavaType(String typeName) {
			this.typeName = typeName;
		}

		@Override
		public <X1> X1 unwrap(X value, Class<X1> type, WrapperOptions options) {
			return null;
		}

		@Override
		public <X1> X wrap(X1 value, WrapperOptions options) {
			return null;
		}

		@Override
		public String getTypeName() {
			return typeName;
		}

		@Override
		public Class<X> getJavaTypeClass() {
			try {
				return (Class<X>) Class.forName( typeName );
			}
			catch (ClassNotFoundException e) {
				return null;
			}
		}
	}

	private class MockMappingMetamodelImpl extends MappingMetamodelImpl {
		public MockMappingMetamodelImpl() {
			super(typeConfiguration, serviceRegistry);
		}

		@Override
		public EntityPersister getEntityDescriptor(String entityName) {
			return createEntityPersister(entityName);
		}

		@Override
		public CollectionPersister getCollectionDescriptor(String role) {
			return createCollectionPersister(role);
		}

		@Override
		public CollectionPersister findCollectionDescriptor(String role) {
			return createCollectionPersister(role);
		}

		@Override
		public JpaMetamodelImplementor getJpaMetamodel() {
			return new MockJpaMetamodelImpl();
		}

		@Override
		public EntityPersister findEntityDescriptor(String entityName) {
			return createEntityPersister(entityName);
		}

		@Override
		public Set<String> getEnumTypesForValue(String enumValue) {
			return MockSessionFactory.this.getEnumTypesForValue(enumValue);
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return MockSessionFactory.this;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return this;
	}

	@Override
	public MetadataImplementor getBootModel() {
		return bootModel;
	}

	@Override
	public MappingMetamodelImplementor getDomainModel() {
		return metamodel;
	}

	@Override
	public SqmFunctionRegistry getFunctionRegistry() {
		return functionRegistry;
	}

	@Override
	public Map<String, Object> getSettings() {
		return emptyMap();
	}

	@Override
	public SqlStringGenerationContext getSqlStringGenerationContext() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return new MockMappingDefaults();
	}

	@Override
	public EffectiveMappingDefaults getEffectiveDefaults() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		return TimeZoneStorageStrategy.NATIVE;
	}

	private class MockJpaMetamodelImpl extends JpaMetamodelImpl {
		public MockJpaMetamodelImpl() {
			super(typeConfiguration, metamodel, serviceRegistry);
		}

		@Override
		public @Nullable EntityDomainType<?> findEntityType(@Nullable String jpaEntityName) {
			final String entityTypeName = qualifyName(jpaEntityName);
			if ( entityTypeName != null ) {
				return new MockEntityDomainType<>(new MockJavaType<>(entityTypeName), jpaEntityName);
			}
			else {
				return null;
			}
		}

		@Override
		public String qualifyImportableName(String queryName) {
			if (isClassDefined(queryName)) {
				return queryName;
			}
			else if (isEntityDefined(queryName)) {
				return qualifyName(queryName);
			}
			else {
				return null;
			}
		}

		@Override
		public @Nullable <X> ManagedDomainType<X> findManagedType(@Nullable String typeName) {
			// TODO: not every ManagedDomainType is an EntityDomainType!
			return typeName == null ? null : new MockEntityDomainType<>(new MockJavaType<>(typeName));
		}

		@Override
		public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
			return findManagedType( cls.getName() );
		}

		@Override
		public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
			return !cls.isArray() && !cls.isPrimitive() && isEntity(cls.getName())
					? new MockEntityDomainType<>(new MockJavaType<>(cls.getName()))
					: null;
		}

		@Override
		public EnumJavaType<?> getEnumType(String className) {
			if ( isEnum(className) ) {
				return new EnumJavaType<>( Enum.class ) {
					@Override
					public String getTypeName() {
						return className;
					}
				};
			}
			else {
				return null;
			}
		}

		@Override
		public JavaType<?> getJavaConstantType(String className, String fieldName) {
			return MockSessionFactory.this.getTypeConfiguration()
					.getJavaTypeRegistry()
					.getDescriptor( javaConstantType( className, fieldName ) );
		}

		@Override
		public <T> T getJavaConstant(String className, String fieldName) {
			return null;
		}

		@Override
		public <E extends Enum<E>> E enumValue(EnumJavaType<E> enumType, String enumValueName) {
			if ( !isEnumConstant( enumType.getTypeName(), enumValueName ) ) {
				throw new IllegalArgumentException( "No enum constant " + enumType.getTypeName() + "." + enumValueName );
			}
			return null;
		}

		@Override
		public @Nullable Set<String> getEnumTypesForValue(String enumValue) {
			return MockSessionFactory.this.getEnumTypesForValue(enumValue);
		}
	}

	@Nullable Set<String> getEnumTypesForValue(String value) {
		return emptySet();
	}

	class MockMappedDomainType<X> extends MappedSuperclassTypeImpl<X>{
		public MockMappedDomainType(String typeName) {
			super(typeName, false, true, false, null, null, metamodel.getJpaMetamodel());
		}

		@Override
		public SqmPersistentAttribute<X,?> findDeclaredAttribute(String name) {
			final String typeName = getTypeName();
			return isFieldDefined(typeName, name)
					? createAttribute(name, typeName, propertyType(typeName, name), this)
					: null;
		}
	}

	class MockEntityDomainType<X> extends EntityTypeImpl<X> {

		public MockEntityDomainType(JavaType<X> javaType) {
			this(javaType, getJpaEntityName(javaType.getTypeName()));
		}

		public MockEntityDomainType(JavaType<X> javaType, String jpaEntityName) {
			super(javaType.getTypeName(), jpaEntityName,
					false, true, false, javaType, null,
					metamodel.getJpaMetamodel());
		}

		@Override
		public SqmSingularPersistentAttribute<? super X, ?> findVersionAttribute() {
			final BasicType<?> type = getVersionType(getHibernateEntityName());
			if (type == null) {
				return null;
			}
			else {
				return new SingularAttributeImpl<>(
						MockEntityDomainType.this,
						EntityVersionMapping.VERSION_ROLE_NAME,
						AttributeClassification.BASIC,
						type,
						type.getRelationalJavaType(),
						null,
						false,
						true,
						false,
						false
				);
			}
		}

		@Override
		public boolean hasVersionAttribute() {
			return getVersionType(getHibernateEntityName()) != null;
		}

		@Override
		public SqmPathSource<?> getIdentifierDescriptor() {
			final Type type = getIdentifierType(getHibernateEntityName());
			if (type instanceof BasicDomainType<?> basicDomainType) {
				return new BasicSqmPathSource<>(
						EntityIdentifierMapping.ID_ROLE_NAME,
						null,
						basicDomainType,
						MockEntityDomainType.this.getExpressibleJavaType(),
						Bindable.BindableType.SINGULAR_ATTRIBUTE,
						false
				);
			}
			else if (type instanceof SqmEmbeddableDomainType<?> embeddableDomainType) {
				return new EmbeddedSqmPathSource<>(
						EntityIdentifierMapping.ID_ROLE_NAME,
						null,
						embeddableDomainType,
						Bindable.BindableType.SINGULAR_ATTRIBUTE,
						false
				);
			}
			else {
				return null;
			}
		}

		@Override
		public SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
			switch (name) {
				case EntityIdentifierMapping.ID_ROLE_NAME:
					return getIdentifierDescriptor();
				case EntityVersionMapping.VERSION_ROLE_NAME:
					return findVersionAttribute();
			}
			final SqmPathSource<?> source = super.findSubPathSource(name, includeSubtypes);
			if ( source != null ) {
				return source;
			}
			final String supertype = MockSessionFactory.this.getSupertype(getHibernateEntityName());
			final PersistentAttribute<? super Object, ?> superattribute
					= new MockMappedDomainType<>(supertype).findAttribute(name);
			if (superattribute != null) {
				return (SqmPathSource<?>) superattribute;
			}
			for (Map.Entry<String, MockEntityPersister> entry : entityPersistersByName.entrySet()) {
				final MockEntityPersister entityPersister = entry.getValue();
				if (!entityPersister.getEntityName().equals(getHibernateEntityName())
						&& isSubtype(entityPersister.getEntityName(), getHibernateEntityName())) {
					final MockEntityDomainType<Object> entityDomainType =
							new MockEntityDomainType<>(new MockJavaType<>(entityPersister.getEntityName()),
									entityPersister.getJpaEntityName());
					final PersistentAttribute<? super Object, ?> subattribute =
							entityDomainType.findAttribute(name);
					if (subattribute != null) {
						return (SqmPathSource<?>) subattribute;
					}
				}
			}
			return null;
		}

		@Override
		public SqmPersistentAttribute<? super X, ?> findAttribute(String name) {
			final var attribute = super.findAttribute(name);
			if (attribute != null) {
				return attribute;
			}
			else {
				final String supertype = MockSessionFactory.this.getSupertype( getHibernateEntityName() );
				return new MockMappedDomainType<>( supertype ).findAttribute( name );
			}
		}

		@Override
		public SqmPersistentAttribute<X,?> findDeclaredAttribute(String name) {
			final String entityName = getHibernateEntityName();
			return isAttributeDefined(entityName, name)
					? createAttribute(name, entityName, getReferencedPropertyType(entityName, name), this)
					: null;
		}
	}

	protected abstract String getJpaEntityName(String typeName);

	private <T> SqmPersistentAttribute<T,?> createAttribute(String name, String entityName, Type type, ManagedDomainType<T> owner) {
		if (type==null) {
			throw new UnsupportedOperationException(entityName + "." + name);
		}
		else if ( type.isCollectionType() ) {
			final CollectionType collectionType = (CollectionType) type;
			return createPluralAttribute(collectionType, entityName, name, owner);
		}
		else if ( type.isEntityType() ) {
			return new SingularAttributeImpl<>(
					owner,
					name,
					AttributeClassification.MANY_TO_ONE,
					new MockEntityDomainType<>(new MockJavaType<>(type.getName())),
					null,
					null,
					false,
					false,
					true,
					false
			);
		}
		else if ( type.isComponentType() ) {
			final CompositeType compositeType = (CompositeType) type;
			return new SingularAttributeImpl<>(
					owner,
					name,
					AttributeClassification.EMBEDDED,
					createEmbeddableDomainType(entityName, compositeType, owner),
					null,
					null,
					false,
					false,
					true,
					false
			);
		}
		else {
			return new SingularAttributeImpl<>(
					owner,
					name,
					AttributeClassification.BASIC,
					(SqmDomainType<?>) type,
					type instanceof JdbcMapping jdbcMapping
							? jdbcMapping.getJavaTypeDescriptor()
							: null,
					null,
					false,
					false,
					true,
					false
			);
		}
	}

	private SqmDomainType<?> getElementDomainType(String entityName, CollectionType collectionType, ManagedDomainType<?> owner) {
		final Type elementType = collectionType.getElementType(MockSessionFactory.this);
		return getDomainType(entityName, collectionType, owner, elementType);
	}

	private DomainType<?> getMapKeyDomainType(String entityName, CollectionType collectionType, ManagedDomainType<?> owner) {
		final Type keyType = getMappingMetamodel().getCollectionDescriptor( collectionType.getRole() ).getIndexType();
		return getDomainType(entityName, collectionType, owner, keyType);
	}

	private SqmDomainType<?> getDomainType(
			String entityName, CollectionType collectionType, ManagedDomainType<?> owner, Type elementType) {
		if ( elementType.isEntityType() ) {
			final String associatedEntityName = collectionType.getAssociatedEntityName(this);
			return new MockEntityDomainType<>(new MockJavaType<>(associatedEntityName));
		}
		else if ( elementType.isComponentType() ) {
			final CompositeType compositeType = (CompositeType) elementType;
			return createEmbeddableDomainType(entityName, compositeType, owner);
		}
		else if ( elementType instanceof SqmDomainType<?> domainType ) {
			return domainType;
		}
		else {
			return OBJECT_BASIC_TYPE;
		}
	}

	private <T> AbstractPluralAttribute<T,?,?> createPluralAttribute(
			CollectionType collectionType,
			String entityName,
			String name,
			ManagedDomainType<T> owner) {
		final Property property = new Property();
		property.setName(name);
		final JavaType<?> collectionJavaType =
				typeConfiguration.getJavaTypeRegistry()
						.getDescriptor(collectionType.getReturnedClass());
		final SqmDomainType<?> elementDomainType = getElementDomainType(entityName, collectionType, owner);
		final CollectionClassification classification = collectionType.getCollectionClassification();
		return switch ( classification ) {
			case LIST -> new ListAttributeImpl(
					new PluralAttributeBuilder<>(
							collectionJavaType,
							true,
							AttributeClassification.MANY_TO_MANY,
							classification,
							elementDomainType,
							typeConfiguration.getBasicTypeRegistry()
									.getRegisteredType( Integer.class ),
							owner,
							property,
							null
					)
			);
			case BAG, ID_BAG -> new BagAttributeImpl(
					new PluralAttributeBuilder<>(
							collectionJavaType,
							true,
							AttributeClassification.MANY_TO_MANY,
							classification,
							elementDomainType,
							null,
							owner,
							property,
							null
					)
			);
			case SET, SORTED_SET, ORDERED_SET -> new SetAttributeImpl(
					new PluralAttributeBuilder<>(
							collectionJavaType,
							true,
							AttributeClassification.MANY_TO_MANY,
							classification,
							elementDomainType,
							null,
							owner,
							property,
							null
					)
			);
			case MAP, SORTED_MAP, ORDERED_MAP -> new MapAttributeImpl(
					new PluralAttributeBuilder<>(
							collectionJavaType,
							true,
							AttributeClassification.MANY_TO_MANY,
							classification,
							elementDomainType,
							getMapKeyDomainType( entityName, collectionType, owner ),
							owner,
							property,
							null
					)
			);
			default -> null;
		};
	}

	private <T> EmbeddableTypeImpl<T> createEmbeddableDomainType(String entityName, CompositeType compositeType, ManagedDomainType<T> owner) {
		final JavaType<T> javaType = new UnknownBasicJavaType<>(null, compositeType.getReturnedClassName());
		return new EmbeddableTypeImpl<>( javaType, null, null, true, metamodel.getJpaMetamodel() ) {
			@Override
			public SqmPersistentAttribute<T, ?> findAttribute(String name) {
				return createAttribute(
						name,
						entityName,
						compositeType.getSubtypes()[compositeType.getPropertyIndex(name)],
						owner
				);
			}
		};
	}
}
