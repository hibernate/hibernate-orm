/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.spi;

import java.io.InvalidObjectException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.internal.ArrayTupleType;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.QueryParameterJavaObjectType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ParameterizedTypeImpl;

import jakarta.persistence.TemporalType;

import static org.hibernate.id.uuid.LocalObjectUuidHelper.generateLocalObjectUuid;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.type.PrimitiveWrappers.canonicalize;
import static org.hibernate.query.sqm.internal.TypecheckUtil.isNumberArray;

/**
 * Each instance defines a set of {@linkplain Type types} available in a given
 * persistence unit, and isolates them from other configurations.
 * <p>
 * Note that each instance of {@code Type} is inherently "scoped" to a
 * {@code TypeConfiguration}. We always obtain a reference to a {@code Type}
 * via the {@code TypeConfiguration} associated with the current persistence
 * unit.
 * <p>
 * On the other hand, a {@code Type} does not inherently have access to its
 * parent {@code TypeConfiguration} since extensions may contribute instances
 * of {@code Type}, via {@link org.hibernate.boot.model.TypeContributions},
 * for example, and the instantiation of such instances occurs outside the
 * control of Hibernate.
 * <p>
 * In particular, a custom {@link org.hibernate.boot.model.TypeContributor}
 * may contribute types to a {@code TypeConfiguration}.
 * <p>
 * If a {@code Type} requires access to the parent {@code TypeConfiguration},
 * it should implement {@link TypeConfigurationAware}.
 *
 * @author Steve Ebersole
 *
 * @since 5.3
 *
 * @see org.hibernate.boot.model.TypeContributor
 * @see org.hibernate.boot.model.TypeContributions
 */
@Incubating
public class TypeConfiguration implements SessionFactoryObserver, Serializable {
//	private static final CoreMessageLogger LOG = messageLogger( Scope.class );

	private final String uuid = generateLocalObjectUuid();

	private final Scope scope;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// things available during both boot and runtime lifecycle phases
	private final transient JavaTypeRegistry javaTypeRegistry;
	private final transient JdbcTypeRegistry jdbcTypeRegistry;
	private final transient DdlTypeRegistry ddlTypeRegistry;
	private final transient BasicTypeRegistry basicTypeRegistry;

	private final transient Map<Integer, Set<String>> jdbcToHibernateTypeContributionMap = new HashMap<>();

	public TypeConfiguration() {
		scope = new Scope( this );
		javaTypeRegistry = new JavaTypeRegistry( this );
		jdbcTypeRegistry = new JdbcTypeRegistry( this );
		ddlTypeRegistry = new DdlTypeRegistry( this );
		basicTypeRegistry = new BasicTypeRegistry( this );
		StandardBasicTypes.prime( this );
	}

	public String getUuid() {
		return uuid;
	}

	public BasicTypeRegistry getBasicTypeRegistry() {
		return basicTypeRegistry;
	}

	public JavaTypeRegistry getJavaTypeRegistry() {
		return javaTypeRegistry;
	}

	public JdbcTypeRegistry getJdbcTypeRegistry() {
		return jdbcTypeRegistry;
	}

	public DdlTypeRegistry getDdlTypeRegistry() {
		return ddlTypeRegistry;
	}

	public JdbcTypeIndicators getCurrentBaseSqlTypeIndicators() {
		return scope;
	}

	public Map<Integer, Set<String>> getJdbcToHibernateTypeContributionMap() {
		return jdbcToHibernateTypeContributionMap;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Scoping

	/**
	 * Obtain the {@link MetadataBuildingContext} currently scoping this {@code TypeConfiguration}.
	 *
	 * @apiNote Throws an exception if the {@code TypeConfiguration} is no longer scoped to the
	 *          {@link MetadataBuildingContext}. See {@link Scope} for more details regarding the
	 *          stages a {@code TypeConfiguration} passes through.
	 *
	 * @return The {@link MetadataBuildingContext}
	 *
	 * @deprecated This operation is not very typesafe, and we're migrating away from its use
	 */
	@Deprecated(since = "6.2")
	public MetadataBuildingContext getMetadataBuildingContext() {
		return scope.getMetadataBuildingContext();
	}

	/**
	 * Scope this {@code TypeConfiguration} to the given {@link MetadataBuildingContext}.
	 *
	 * @implNote The given factory is not yet fully-initialized!
	 *
	 * @param metadataBuildingContext a {@link MetadataBuildingContext}
	 */
	public void scope(MetadataBuildingContext metadataBuildingContext) {
//		LOG.tracef( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );
	}

	/**
	 * Scope this {@code TypeConfiguration} to the given {@link SessionFactory}.
	 *
	 * @implNote The given factory is not yet fully-initialized!
	 *
	 * @param sessionFactory a {@link SessionFactory} that is in a very fragile state
	 */
	public void scope(SessionFactoryImplementor sessionFactory) {
//		LOG.tracef( "Scoping TypeConfiguration [%s] to SessionFactoryImplementor [%s]", this, sessionFactory );

		if ( scope.getMetadataBuildingContext() == null ) {
			throw new IllegalStateException( "MetadataBuildingContext not known" );
		}

		scope.setSessionFactory( sessionFactory );
		sessionFactory.addObserver( this );
	}

	/**
	 * Obtain the {@link SessionFactory} currently scoping this {@code TypeConfiguration}.
	 *
	 * @apiNote Throws an exception if the {@code TypeConfiguration} is not yet scoped to
	 *          a factory. See {@link Scope} for more details regarding the stages a
	 *          {@code TypeConfiguration} passes through (this is a "runtime stage").
	 *
	 * @return The {@link SessionFactory} to which this {@code TypeConfiguration} is scoped
	 *
	 * @throws HibernateException if the {@code TypeConfiguration} is not currently scoped
	 *                            to a {@link SessionFactory} (in a "runtime stage").
	 *
	 * @deprecated This operation is not very typesafe, and we're migrating away from its use
	 */
	@Deprecated(since = "6.2")
	public SessionFactoryImplementor getSessionFactory() {
		return scope.getSessionFactory();
	}

	/**
	 * Obtain the {@link ServiceRegistry} scoped to this {@code TypeConfiguration}.
	 *
	 * @apiNote The current {@link Scope} will determine from where the {@link ServiceRegistry}
	 *          is obtained.
	 *
	 * @return The {@link ServiceRegistry} for the current scope
	 *
	 * @deprecated This simply isn't a very sensible place to hang the {@link ServiceRegistry}
	 */
	@Deprecated(since = "6.2")
	public ServiceRegistry getServiceRegistry() {
		return scope.getServiceRegistry();
	}

	/**
	 * Obtain the {@link JpaCompliance} setting.
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public JpaCompliance getJpaCompliance() {
		return scope.getJpaCompliance();
	}

	/**
	 * Workaround for an issue faced in {@link org.hibernate.type.EntityType#getReturnedClass()}.
	 */
	@Internal
	public Class<?> entityClassForEntityName(String entityName) {
		return scope.entityClassForEntityName(entityName);
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		// Instead of allowing scope#setSessionFactory to influence this, we use the SessionFactoryObserver callback
		// to handle this, allowing any SessionFactory constructor code to be able to continue to have access to the
		// MetadataBuildingContext through TypeConfiguration until this callback is fired.
//		LOG.tracef( "Handling #sessionFactoryCreated from [%s] for TypeConfiguration", factory );
		scope.setMetadataBuildingContext( null );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
//		LOG.tracef( "Handling #sessionFactoryClosed from [%s] for TypeConfiguration", factory );
		scope.unsetSessionFactory( factory );
		// todo (6.0) : finish this
		//		release Database, descriptor Maps, etc... things that are only
		// 		valid while the TypeConfiguration is scoped to SessionFactory
	}

	public void addBasicTypeRegistrationContributions(List<BasicTypeRegistration> contributions) {
		for ( var basicTypeRegistration : contributions ) {
			addBasicTypeRegistration( basicTypeRegistration, basicTypeRegistration.getBasicType() );
		}
	}

	private <T> void addBasicTypeRegistration(BasicTypeRegistration basicTypeRegistration, BasicType<T> basicType) {
		basicTypeRegistry.register(
				basicType,
				basicTypeRegistration.getRegistrationKeys()
		);

		javaTypeRegistry.resolveDescriptor(
				basicType.getJavaType(),
				basicType::getJavaTypeDescriptor
		);

		jdbcToHibernateTypeContributionMap.computeIfAbsent(
				basicType.getJdbcType().getDefaultSqlTypeCode(),
				k -> new HashSet<>()
		).add( basicType.getName() );
	}

	/**
	 * Understands the following target type names for the {@code cast()} function:
	 * <ul>
	 * <li>{@code String}
	 * <li>{@code Character}
	 * <li>{@code Byte}, {@code Short}, {@code Integer}, {@code Long}
	 * <li>{@code Float}, {@code Double}
	 * <li>{@code Time}, {@code Date}, {@code Timestamp}
	 * <li>{@code LocalDate}, {@code LocalTime}, {@code LocalDateTime}
	 * <li>{@code BigInteger}
	 * <li>{@code BigDecimal}
	 * <li>{@code Binary}
	 * <li>{@code Boolean}
	 *     (fragile, not aware of encoding to character via
	 *     {@link org.hibernate.type.CharBooleanConverter})
	 * </ul>
	 * <p>
	 * The type names are not case-sensitive.
	 */
	public BasicType<?> resolveCastTargetType(String name) {
		switch ( name.toLowerCase() ) {
			case "string": return getBasicTypeForJavaType( String.class );
			case "character": return getBasicTypeForJavaType( Character.class );
			case "byte": return getBasicTypeForJavaType( Byte.class );
			case "short": return getBasicTypeForJavaType( Short.class );
			case "integer": return getBasicTypeForJavaType( Integer.class );
			case "long": return getBasicTypeForJavaType( Long.class );
			case "float": return getBasicTypeForJavaType( Float.class );
			case "double": return getBasicTypeForJavaType( Double.class );
			case "time": return getBasicTypeForJavaType( Time.class );
			case "date": return getBasicTypeForJavaType( java.sql.Date.class );
			case "timestamp": return getBasicTypeForJavaType( Timestamp.class );
			case "localtime": return getBasicTypeForJavaType( LocalTime.class );
			case "localdate": return getBasicTypeForJavaType( LocalDate.class );
			case "localdatetime": return getBasicTypeForJavaType( LocalDateTime.class );
			case "offsetdatetime": return getBasicTypeForJavaType( OffsetDateTime.class );
			case "zoneddatetime": return getBasicTypeForJavaType( ZonedDateTime.class );
			case "biginteger": return getBasicTypeForJavaType( BigInteger.class );
			case "bigdecimal": return getBasicTypeForJavaType( BigDecimal.class );
			case "duration": return getBasicTypeForJavaType( Duration.class );
			case "instant": return getBasicTypeForJavaType( Instant.class );
			case "binary": return getBasicTypeForJavaType( byte[].class );
			//this one is very fragile ... works well for BIT or BOOLEAN columns only
			//works OK, I suppose, for integer columns, but not at all for char columns
			case "boolean": return getBasicTypeForJavaType( Boolean.class );
			case "truefalse": return basicTypeRegistry.getRegisteredType( StandardBasicTypes.TRUE_FALSE.getName() );
			case "yesno": return basicTypeRegistry.getRegisteredType( StandardBasicTypes.YES_NO.getName() );
			case "numericboolean": return basicTypeRegistry.getRegisteredType( StandardBasicTypes.NUMERIC_BOOLEAN.getName() );
			case "json": return basicTypeRegistry.resolve( Object.class, SqlTypes.JSON );
			case "xml": return basicTypeRegistry.resolve( Object.class, SqlTypes.SQLXML );
			//really not sure about this one - it works well for casting from binary
			//to UUID, but people will want to use it to cast from varchar, and that
			//won't work at all without some special casing in the Dialects
//			case "uuid": return getBasicTypeForJavaType( UUID.class );
			default: {
				final BasicType<?> registeredBasicType = basicTypeRegistry.getRegisteredType( name );
				if ( registeredBasicType != null ) {
					return registeredBasicType;
				}

				try {
					final Class<?> javaTypeClass = scope.getClassLoaderService().classForName( name );
					final var jtd = javaTypeRegistry.resolveDescriptor( javaTypeClass );
					final var jdbcType = jtd.getRecommendedJdbcType( getCurrentBaseSqlTypeIndicators() );
					return basicTypeRegistry.resolve( jtd, jdbcType );
				}
				catch ( Exception ignore ) {
				}

				throw new HibernateException( "unrecognized cast target type: " + name );
			}
		}
	}

	/**
	 * Encapsulation of lifecycle concerns of a {@link TypeConfiguration}:
	 * <ol>
	 *     <li>
	 *         "Boot" is where the {@link TypeConfiguration} is first built as
	 *         {@linkplain org.hibernate.boot.model the boot model} of the domain
	 *         model is converted into {@linkplain org.hibernate.metamodel.model
	 *         the runtime model}. During this phase,
	 *         {@link #getMetadataBuildingContext()} is accessible but
	 *         {@link #getSessionFactory} throws an exception.
	 *     </li>
	 *     <li>
	 *         "Runtime" is where the runtime model is accessible. During this
	 *         phase, {@link #getSessionFactory()} is accessible but
	 *         {@link #getMetadataBuildingContext()} throws an exception.
	 *     </li>
	 *     <li>
	 *        "Sunset" happens after the {@link SessionFactory} has been closed.
	 *        Both {@link #getSessionFactory()} and {@link #getMetadataBuildingContext()}
	 *        throw exceptions.
	 *     </li>
	 * </ol>
	 * <p>
	 * On the other hand, the {@linkplain #getServiceRegistry() service registry}
	 * is available during both "Boot" and "Runtime" phases.
	 * <p>
	 * Each stage or phase is considered a scope for the {@link TypeConfiguration}.
	 */
	private static class Scope implements JdbcTypeIndicators, Serializable {
		private final TypeConfiguration typeConfiguration;

		private transient MetadataBuildingContext metadataBuildingContext;
		private transient SessionFactoryImplementor sessionFactory;

		private boolean allowExtensionsInCdi;
		private String sessionFactoryName;
		private String sessionFactoryUuid;

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public boolean isPreferJavaTimeJdbcTypesEnabled() {
			return sessionFactory == null
					? metadataBuildingContext.isPreferJavaTimeJdbcTypesEnabled()
					: sessionFactory.getSessionFactoryOptions().isPreferJavaTimeJdbcTypesEnabled();
		}

		@Override
		public boolean isPreferNativeEnumTypesEnabled() {
			return sessionFactory == null
					? metadataBuildingContext.isPreferNativeEnumTypesEnabled()
					: sessionFactory.getSessionFactoryOptions().isPreferNativeEnumTypesEnabled();
		}

		@Override
		public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
			return sessionFactory == null
					? metadataBuildingContext.getBuildingOptions().getDefaultTimeZoneStorage()
					: sessionFactory.getSessionFactoryOptions().getDefaultTimeZoneStorageStrategy();
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return sessionFactory == null
					? metadataBuildingContext.getPreferredSqlTypeCodeForBoolean()
					: sessionFactory.getSessionFactoryOptions().getPreferredSqlTypeCodeForBoolean();
		}

		@Override
		public int getPreferredSqlTypeCodeForDuration() {
			return sessionFactory == null
					? metadataBuildingContext.getPreferredSqlTypeCodeForDuration()
					: sessionFactory.getSessionFactoryOptions().getPreferredSqlTypeCodeForDuration();
		}

		@Override
		public int getPreferredSqlTypeCodeForUuid() {
			return sessionFactory == null
					? metadataBuildingContext.getPreferredSqlTypeCodeForUuid()
					: sessionFactory.getSessionFactoryOptions().getPreferredSqlTypeCodeForUuid();
		}

		@Override
		public int getPreferredSqlTypeCodeForInstant() {
			return sessionFactory == null
					? metadataBuildingContext.getPreferredSqlTypeCodeForInstant()
					: sessionFactory.getSessionFactoryOptions().getPreferredSqlTypeCodeForInstant();
		}

		@Override
		public int getPreferredSqlTypeCodeForArray() {
			return sessionFactory == null
					? metadataBuildingContext.getPreferredSqlTypeCodeForArray()
					: sessionFactory.getSessionFactoryOptions().getPreferredSqlTypeCodeForArray();
		}

		@Override
		public Dialect getDialect() {
			return sessionFactory == null
					? metadataBuildingContext.getMetadataCollector().getDatabase().getDialect()
					: sessionFactory.getJdbcServices().getDialect();
		}

		@Override
		public boolean preferJdbcDatetimeTypes() {
			return sessionFactory != null
				&& sessionFactory.getSessionFactoryOptions().isPreferJdbcDatetimeTypesInNativeQueriesEnabled();
		}

		@Override
		public boolean isXmlFormatMapperLegacyFormatEnabled() {
			if ( metadataBuildingContext != null ) {
				return metadataBuildingContext.getBuildingOptions().isXmlFormatMapperLegacyFormatEnabled();
			}
			else if ( sessionFactory != null ) {
				return sessionFactory.getSessionFactoryOptions().isXmlFormatMapperLegacyFormatEnabled();
			}
			else {
				return false;
			}
		}

		public ClassLoaderService getClassLoaderService() {
			return sessionFactory == null
					? metadataBuildingContext.getBootstrapContext().getClassLoaderService()
					: sessionFactory.getClassLoaderService();
		}

		public ManagedBeanRegistry getManagedBeanRegistry() {
			return sessionFactory == null
					? metadataBuildingContext.getBootstrapContext().getManagedBeanRegistry()
					: sessionFactory.getManagedBeanRegistry();
		}

		private Scope(TypeConfiguration typeConfiguration) {
			this.typeConfiguration = typeConfiguration;
		}

		private MetadataBuildingContext getMetadataBuildingContext() {
			if ( metadataBuildingContext == null ) {
				throw new HibernateException( "TypeConfiguration is not currently scoped to MetadataBuildingContext" );
			}
			return metadataBuildingContext;
		}

		private ServiceRegistry getServiceRegistry() {
			if ( metadataBuildingContext != null ) {
				return metadataBuildingContext.getBootstrapContext().getServiceRegistry();
			}
			else if ( sessionFactory != null ) {
				return sessionFactory.getServiceRegistry();
			}
			else {
				throw new AssertionFailure( "No service registry available" );
			}
		}

		private JpaCompliance getJpaCompliance() {
			if ( metadataBuildingContext != null ) {
				return metadataBuildingContext.getBootstrapContext().getJpaCompliance();
			}
			else if ( sessionFactory != null ) {
				return sessionFactory.getSessionFactoryOptions().getJpaCompliance();
			}
			return null;
		}

		private void setMetadataBuildingContext(MetadataBuildingContext context) {
			metadataBuildingContext = context;
			if ( context != null ) {
				allowExtensionsInCdi = context.getBuildingOptions().isAllowExtensionsInCdi();
			}
		}

		private SessionFactoryImplementor getSessionFactory() {
			if ( sessionFactory == null ) {
				if ( sessionFactoryName == null && sessionFactoryUuid == null ) {
					throw new HibernateException( "TypeConfiguration was not yet scoped to SessionFactory" );
				}
				sessionFactory =
						SessionFactoryRegistry.INSTANCE
								.findSessionFactory( sessionFactoryUuid, sessionFactoryName );
				if ( sessionFactory == null ) {
					throw new HibernateException(
							"Could not find a SessionFactory [uuid=" + sessionFactoryUuid + ",name=" + sessionFactoryName + "]"
					);
				}
			}
			return sessionFactory;
		}

		/**
		 * Used by {@link TypeConfiguration} scoping.
		 *
		 * @param factory The {@link SessionFactory} to which the {@link TypeConfiguration} is being bound
		 */
		private void setSessionFactory(SessionFactoryImplementor factory) {
			if ( sessionFactory != null ) {
//				LOG.scopingTypesToSessionFactoryAfterAlreadyScoped( sessionFactory, factory );
				throw new IllegalStateException( "TypeConfiguration was already scoped to SessionFactory: "
													+ sessionFactory.getUuid() );
			}
			else {
				sessionFactoryUuid = factory.getUuid();
				sessionFactoryName = getFactoryName( factory );
			}
			sessionFactory = factory;
		}

		private static String getFactoryName(SessionFactoryImplementor factory) {
			final String factoryName = factory.getSessionFactoryOptions().getSessionFactoryName();
			if ( factoryName == null ) {
				final var cfgXmlAccessService =
						factory.getServiceRegistry()
								.requireService( CfgXmlAccessService.class );
				final var aggregatedConfig = cfgXmlAccessService.getAggregatedConfig();
				return aggregatedConfig == null ? null : aggregatedConfig.getSessionFactoryName();
			}
			else {
				return factoryName;
			}
		}

		private void unsetSessionFactory(SessionFactory factory) {
//			LOG.tracef( "Un-scoping TypeConfiguration [%s] from SessionFactory [%s]", this, factory );
			sessionFactory = null;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Custom serialization hook

		@Serial
		private Object readResolve() throws InvalidObjectException {
			if ( sessionFactory == null ) {
				if ( sessionFactoryName != null || sessionFactoryUuid != null ) {
					sessionFactory =
							SessionFactoryRegistry.INSTANCE
									.findSessionFactory( sessionFactoryUuid, sessionFactoryName );
					if ( sessionFactory == null ) {
						throw new HibernateException( "Could not find a SessionFactory [uuid="
								+ sessionFactoryUuid + ",name=" + sessionFactoryName + "]"
						);
					}
				}
			}

			return this;
		}

		private Class<?> entityClassForEntityName(String entityName) {
			return sessionFactory == null
					? metadataBuildingContext.getMetadataCollector().getEntityBinding( entityName ).getMappedClass()
					: sessionFactory.getMappingMetamodel().findEntityDescriptor( entityName ).getMappedClass();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final ConcurrentMap<ArrayCacheKey, ArrayTupleType> arrayTuples = new ConcurrentHashMap<>();

	public SqmBindableType<?> resolveTupleType(List<? extends SqmTypedNode<?>> typedNodes) {
		final var components = new SqmBindableType<?>[typedNodes.size()];
		for ( int i = 0; i < typedNodes.size(); i++ ) {
			final var tupleElement = typedNodes.get(i);
			final var sqmExpressible = tupleElement.getNodeType();
			// keep null value for Named Parameters
			if ( tupleElement instanceof SqmParameter<?> && sqmExpressible == null ) {
				components[i] = QueryParameterJavaObjectType.INSTANCE;
			}
			else {
				components[i] = sqmExpressible != null
						? sqmExpressible
						: castNonNull( getBasicTypeForJavaType( Object.class ) );
			}
		}
		return arrayTuples.computeIfAbsent( new ArrayCacheKey( components ),
				key -> new ArrayTupleType( key.components ) );
	}

	private static class ArrayCacheKey {
		final SqmBindableType<?>[] components;

		public ArrayCacheKey(SqmBindableType<?>[] components) {
			this.components = components;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof ArrayCacheKey key
				&& Arrays.equals( components, key.components );
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode( components );
		}
	}

	/**
	 * @see QueryHelper#highestPrecedenceType2
	 */
	public @Nullable SqmBindableType<?> resolveArithmeticType(
			@Nullable SqmBindableType<?> firstType,
			@Nullable SqmBindableType<?> secondType,
			BinaryArithmeticOperator operator) {
		return resolveArithmeticType( firstType, secondType );
	}

	/**
	 * Determine the result type of an arithmetic operation as defined by the
	 * rules in section 6.5.8.1, taking converters into account.
	 *
	 * @see QueryHelper#highestPrecedenceType2
	 */
	public @Nullable SqmBindableType<?> resolveArithmeticType(
			@Nullable SqmBindableType<?> firstType,
			@Nullable SqmBindableType<?> secondType) {

		if ( getSqlTemporalType( firstType ) != null ) {
			if ( secondType==null || getSqlTemporalType( secondType ) != null ) {
				// special case for subtraction of two dates
				// or timestamps resulting in a duration
				return getBasicTypeRegistry().getRegisteredType( Duration.class );
			}
			else {
				// must be postfix addition/subtraction of
				// a duration to/from a date or timestamp
				return firstType;
			}
		}
		else if ( isDuration( secondType ) ) {
			// if firstType is not known, and operator is
			// addition/subtraction, then this can be
			// either addition/subtraction of duration
			// to/from temporal or addition/subtraction of
			// durations in this case we shall return null;
			// otherwise, it's either addition/subtraction of durations
			// or prefix scalar multiplication of a duration
//			return secondType;
			return firstType == null ? null : secondType;
		}
		else if ( firstType==null && getSqlTemporalType( secondType ) != null ) {
			// subtraction of a date or timestamp from a
			// parameter (which doesn't have a type yet)
			return getBasicTypeRegistry().getRegisteredType( Duration.class );
		}

		if ( firstType != null && ( secondType == null
				|| !secondType.getRelationalJavaType().isWider( firstType.getRelationalJavaType() ) ) ) {
			return resolveArithmeticType( firstType );
		}
		return secondType != null ? resolveArithmeticType( secondType ) : null;
	}

	/**
	 * Determine the result type of a unary arithmetic operation,
	 * taking converters into account.
	 */
	public @Nullable SqmBindableType<?> resolveArithmeticType(SqmBindableType<?> expressible) {
		return isNumberArray( expressible )
				? expressible.getSqmType()
				// Use the relational java type to account for possible converters
				: getBasicTypeForJavaType( expressible.getRelationalJavaType().getJavaTypeClass() );
	}

	private static boolean matchesJavaType(SqmExpressible<?> type, Class<?> javaType) {
		assert javaType != null;
		return type != null && javaType.isAssignableFrom( type.getRelationalJavaType().getJavaTypeClass() );
	}


	private final ConcurrentHashMap<Type, BasicType<?>> basicTypeByJavaType = new ConcurrentHashMap<>();

	private static <J> BasicType<J> checkExisting(Class<J> javaClass, BasicType<?> existing) {
		if ( !isCompatible( javaClass, existing.getJavaType() ) ) {
			throw new IllegalStateException( "Type registration was corrupted for: " + javaClass.getName() );
		}
		@SuppressWarnings("unchecked") // safe, we just checked
		final var basicType = (BasicType<J>) existing;
		return basicType;
	}

	private static <J> boolean isCompatible(Class<J> javaClass, Class<?> existing) {
		return existing.isAssignableFrom( canonicalize( javaClass ) );
	}

	@Deprecated(since = "7.2", forRemoval = true) // no longer used
	public <J> @Nullable BasicType<J> getBasicTypeForGenericJavaType(Class<? super J> javaType, Type... typeArguments) {
		//noinspection unchecked
		return (BasicType<J>)
				getBasicTypeForJavaType( new ParameterizedTypeImpl( javaType, typeArguments, null ) );
	}

	public <J> @Nullable BasicType<J> getBasicTypeForJavaType(Class<J> javaClass) {
		final var existing = basicTypeByJavaType.get( javaClass );
		if ( existing != null ) {
			return checkExisting( javaClass, existing );
		}
		else {
			final var registeredType = basicTypeRegistry.getRegisteredType( javaClass );
			if ( registeredType != null ) {
				basicTypeByJavaType.put( javaClass, registeredType );
				return registeredType;
			}
			else {
				return null;
			}
		}
	}

	public @Nullable BasicType<?> getBasicTypeForJavaType(Type javaType) {
		final var existing = basicTypeByJavaType.get( javaType );
		if ( existing != null ) {
			return existing;
		}
		else {
			final var registeredType = basicTypeRegistry.getRegisteredType( javaType );
			if ( registeredType != null ) {
				basicTypeByJavaType.put( javaType, registeredType );
				return registeredType;
			}
			else {
				return null;
			}
		}
	}

	public <J> BasicType<J> standardBasicTypeForJavaType(Class<J> javaClass) {
		return javaClass == null ? null
				: standardBasicTypeForJavaType( javaClass,
						descriptor -> new BasicTypeImpl<>( descriptor,
								descriptor.getRecommendedJdbcType(
										getCurrentBaseSqlTypeIndicators() ) ) );
	}

	public BasicType<?> standardBasicTypeForJavaType(Type javaType) {
		return javaType == null ? null
				: standardBasicTypeForJavaType( javaType,
						descriptor -> new BasicTypeImpl<>( descriptor,
								descriptor.getRecommendedJdbcType(
										getCurrentBaseSqlTypeIndicators() ) ) );
	}

	@Deprecated(since = "7.2", forRemoval = true) // Can be private
	public <J> BasicType<J> standardBasicTypeForJavaType(
			Class<J> javaClass,
			Function<JavaType<J>, BasicType<J>> creator) {
		if ( javaClass == null ) {
			return null;
		}
		var existing = basicTypeByJavaType.get( javaClass );
		if ( existing != null ) {
			return checkExisting( javaClass, existing );
		}
		else {
			// See if one exists in the BasicTypeRegistry and use that one if so
			final var registeredType =
					basicTypeRegistry.getRegisteredType( javaClass );
			return registeredType == null
					? creator.apply( javaTypeRegistry.resolveDescriptor( javaClass ) )
					: registeredType;
		}
	}

	@Deprecated(since = "7.2", forRemoval = true) // Due to weird signature and unchecked cast
	public <J> BasicType<?> standardBasicTypeForJavaType(
			Type javaType,
			Function<JavaType<J>, BasicType<J>> creator) {
		if ( javaType == null ) {
			return null;
		}
		var existing = basicTypeByJavaType.get( javaType );
		if ( existing != null ) {
			return existing;
		}
		else {
			// See if one exists in the BasicTypeRegistry and use that one if so
			final var registeredType =
					basicTypeRegistry.getRegisteredType( javaType );
			//noinspection unchecked
			return registeredType == null
					? creator.apply( (JavaType<J>) // UNCHECKED CAST
							javaTypeRegistry.resolveDescriptor( javaType ) )
					: registeredType;
		}
	}

	@SuppressWarnings("deprecation")
	public @Nullable TemporalType getSqlTemporalType(@Nullable SqmExpressible<?> type) {
		return type == null ? null
			: getSqlTemporalType( type.getRelationalJavaType()
					.getRecommendedJdbcType( getCurrentBaseSqlTypeIndicators() ) );

	}

	@SuppressWarnings("deprecation")
	public static @Nullable TemporalType getSqlTemporalType(JdbcMapping jdbcMapping) {
		return getSqlTemporalType( jdbcMapping.getJdbcType() );
	}

	@SuppressWarnings("deprecation")
	public static @Nullable TemporalType getSqlTemporalType(JdbcMappingContainer jdbcMappings) {
		assert jdbcMappings.getJdbcTypeCount() == 1;
		return getSqlTemporalType( jdbcMappings.getSingleJdbcMapping().getJdbcType() );
	}

	@SuppressWarnings("deprecation")
	public static @Nullable TemporalType getSqlTemporalType(MappingModelExpressible<?> type) {
		if ( type instanceof BasicValuedMapping basicValuedMapping ) {
			return getSqlTemporalType( basicValuedMapping.getJdbcMapping().getJdbcType() );
		}
		else if ( type instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
			// Handle the special embeddables for emulated offset/timezone handling
			final var javaTypeClass = embeddableValuedModelPart.getJavaType().getJavaTypeClass();
			if ( javaTypeClass == OffsetDateTime.class
					|| javaTypeClass == ZonedDateTime.class ) {
				return TemporalType.TIMESTAMP;
			}
			else if ( javaTypeClass == OffsetTime.class ) {
				return TemporalType.TIME;
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public static @Nullable TemporalType getSqlTemporalType(JdbcType descriptor) {
		return getSqlTemporalType( descriptor.getDefaultSqlTypeCode() );
	}

	@SuppressWarnings("deprecation")
	protected static @Nullable TemporalType getSqlTemporalType(int jdbcTypeCode) {
		return switch ( jdbcTypeCode ) {
			case SqlTypes.TIMESTAMP, SqlTypes.TIMESTAMP_WITH_TIMEZONE, SqlTypes.TIMESTAMP_UTC
					-> TemporalType.TIMESTAMP;
			case SqlTypes.TIME, SqlTypes.TIME_WITH_TIMEZONE, SqlTypes.TIME_UTC
					-> TemporalType.TIME;
			case SqlTypes.DATE
					-> TemporalType.DATE;
			default -> null;
		};
	}

	public static @Nullable IntervalType getSqlIntervalType(JdbcMappingContainer jdbcMappings) {
		assert jdbcMappings.getJdbcTypeCount() == 1;
		return getSqlIntervalType( jdbcMappings.getSingleJdbcMapping().getJdbcType() );
	}

	public static @Nullable IntervalType getSqlIntervalType(JdbcType descriptor) {
		return getSqlIntervalType( descriptor.getDefaultSqlTypeCode() );
	}

	protected static @Nullable IntervalType getSqlIntervalType(int jdbcTypeCode) {
		return jdbcTypeCode == SqlTypes.INTERVAL_SECOND ? IntervalType.SECOND : null;
	}

	public static boolean isJdbcTemporalType(@Nullable SqmExpressible<?> type) {
		return matchesJavaType( type, Date.class );
	}

	public static boolean isDuration(@Nullable SqmExpressible<?> type) {
		return matchesJavaType( type, Duration.class );
	}

	@Internal @SuppressWarnings("unchecked")
	public <J> MutabilityPlan<J> createMutabilityPlan(Class<? extends MutabilityPlan<?>> planClass) {
		return !scope.allowExtensionsInCdi
				? (MutabilityPlan<J>) FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( planClass )
				: (MutabilityPlan<J>) scope.getManagedBeanRegistry().getBean( planClass ).getBeanInstance();
	}

	@Internal @Incubating // find a new home for this operation
	public final FormatMapper getJsonFormatMapper() {
		return getSessionFactory().getSessionFactoryOptions().getJsonFormatMapper();
	}

	@Internal @Incubating // find a new home for this operation
	public final FormatMapper getXmlFormatMapper() {
		return getSessionFactory().getSessionFactoryOptions().getXmlFormatMapper();
	}
}
