/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.InvalidObjectException;
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

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.uuid.LocalObjectUuidHelper;
import org.hibernate.internal.CoreMessageLogger;
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
import org.hibernate.resource.beans.spi.ManagedBean;
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
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ParameterizedTypeImpl;

import jakarta.persistence.TemporalType;

import static org.hibernate.internal.CoreLogging.messageLogger;
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
	private static final CoreMessageLogger log = messageLogger( Scope.class );

	private final String uuid = LocalObjectUuidHelper.generateLocalObjectUuid();

	private final Scope scope;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// things available during both boot and runtime lifecycle phases
	private final transient JavaTypeRegistry javaTypeRegistry;
	private final transient JdbcTypeRegistry jdbcTypeRegistry;
	private final transient DdlTypeRegistry ddlTypeRegistry;
	private final transient BasicTypeRegistry basicTypeRegistry;

	private final transient Map<Integer, Set<String>> jdbcToHibernateTypeContributionMap = new HashMap<>();

	public TypeConfiguration() {
		this.scope = new Scope( this );

		this.javaTypeRegistry = new JavaTypeRegistry( this );
		this.jdbcTypeRegistry = new JdbcTypeRegistry( this );
		this.ddlTypeRegistry = new DdlTypeRegistry( this );
		this.basicTypeRegistry = new BasicTypeRegistry( this );
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
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
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
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactoryImplementor [%s]", this, sessionFactory );

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
	 */
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
		log.tracef( "Handling #sessionFactoryCreated from [%s] for TypeConfiguration", factory );
		scope.setMetadataBuildingContext( null );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		log.tracef( "Handling #sessionFactoryClosed from [%s] for TypeConfiguration", factory );

		scope.unsetSessionFactory( factory );

		// todo (6.0) : finish this
		//		release Database, descriptor Maps, etc... things that are only
		// 		valid while the TypeConfiguration is scoped to SessionFactory
	}

	public void addBasicTypeRegistrationContributions(List<BasicTypeRegistration> contributions) {
		for ( BasicTypeRegistration basicTypeRegistration : contributions ) {
			BasicType<?> basicType = basicTypeRegistration.getBasicType();

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
					final Class<?> javaTypeClass =
							scope.getServiceRegistry().requireService( ClassLoaderService.class )
									.classForName( name );
					final JavaType<?> jtd = javaTypeRegistry.resolveDescriptor( javaTypeClass );
					final JdbcType jdbcType = jtd.getRecommendedJdbcType( getCurrentBaseSqlTypeIndicators() );
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
			return null;
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

		private void setMetadataBuildingContext(MetadataBuildingContext metadataBuildingContext) {
			this.metadataBuildingContext = metadataBuildingContext;
			if ( metadataBuildingContext != null ) {
				this.allowExtensionsInCdi = metadataBuildingContext.getBuildingOptions().isAllowExtensionsInCdi();
			}
		}

		private SessionFactoryImplementor getSessionFactory() {
			if ( sessionFactory == null ) {
				if ( sessionFactoryName == null && sessionFactoryUuid == null ) {
					throw new HibernateException( "TypeConfiguration was not yet scoped to SessionFactory" );
				}
				sessionFactory = SessionFactoryRegistry.INSTANCE.findSessionFactory(
						sessionFactoryUuid,
						sessionFactoryName
				);
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
			if ( this.sessionFactory != null ) {
				log.scopingTypesToSessionFactoryAfterAlreadyScoped( this.sessionFactory, factory );
			}
			else {
				this.sessionFactoryUuid = factory.getUuid();
				this.sessionFactoryName = getFactoryName( factory );
			}
			this.sessionFactory = factory;
		}

		private static String getFactoryName(SessionFactoryImplementor factory) {
			final String factoryName = factory.getSessionFactoryOptions().getSessionFactoryName();
			if ( factoryName == null ) {
				final CfgXmlAccessService cfgXmlAccessService = factory.getServiceRegistry()
						.requireService( CfgXmlAccessService.class );
				if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
					return cfgXmlAccessService.getAggregatedConfig().getSessionFactoryName();
				}
			}
			return factoryName;
		}

		private void unsetSessionFactory(SessionFactory factory) {
			log.debugf( "Un-scoping TypeConfiguration [%s] from SessionFactory [%s]", this, factory );
			this.sessionFactory = null;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Custom serialization hook

		private Object readResolve() throws InvalidObjectException {
			if ( sessionFactory == null ) {
				if ( sessionFactoryName != null || sessionFactoryUuid != null ) {
					sessionFactory = SessionFactoryRegistry.INSTANCE.findSessionFactory(
							sessionFactoryUuid,
							sessionFactoryName
					);

					if ( sessionFactory == null ) {
						throw new HibernateException(
								"Could not find a SessionFactory [uuid=" + sessionFactoryUuid + ",name=" + sessionFactoryName + "]"
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

	public SqmExpressible<?> resolveTupleType(List<? extends SqmTypedNode<?>> typedNodes) {
		final SqmExpressible<?>[] components = new SqmExpressible<?>[typedNodes.size()];
		for ( int i = 0; i < typedNodes.size(); i++ ) {
			SqmTypedNode<?> tupleElement = typedNodes.get(i);
			final SqmExpressible<?> sqmExpressible = tupleElement.getNodeType();
			// keep null value for Named Parameters
			if (tupleElement instanceof SqmParameter<?> && sqmExpressible == null) {
				components[i] = QueryParameterJavaObjectType.INSTANCE;
			}
			else {
				components[i] = sqmExpressible != null
						? sqmExpressible
						: getBasicTypeForJavaType( Object.class );
			}
		}
		return arrayTuples.computeIfAbsent(
				new ArrayCacheKey( components ),
				key -> new ArrayTupleType( key.components )
		);
	}

	private static class ArrayCacheKey {
		final SqmExpressible<?>[] components;

		public ArrayCacheKey(SqmExpressible<?>[] components) {
			this.components = components;
		}

		@Override
		public boolean equals(Object o) {
			return Arrays.equals( components, ((ArrayCacheKey) o).components );
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode( components );
		}
	}

	/**
	 * @see QueryHelper#highestPrecedenceType2
	 */
	public SqmExpressible<?> resolveArithmeticType(
			SqmExpressible<?> firstType,
			SqmExpressible<?> secondType,
			BinaryArithmeticOperator operator) {
		return resolveArithmeticType( firstType, secondType );
	}

	/**
	 * Determine the result type of an arithmetic operation as defined by the
	 * rules in section 6.5.8.1.
	 *
	 * @see QueryHelper#highestPrecedenceType2
	 */
	public SqmExpressible<?> resolveArithmeticType(
			SqmExpressible<?> firstType,
			SqmExpressible<?> secondType) {

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
				|| firstType.getRelationalJavaType().isWider( secondType.getRelationalJavaType() ) ) ) {
			return resolveBasicArithmeticType( firstType );
		}
		return secondType != null ? resolveBasicArithmeticType( secondType ) : null;
	}

	private BasicType<?> resolveBasicArithmeticType(SqmExpressible<?> expressible) {
		if ( isNumberArray( expressible ) ) {
			return (BasicType<?>) expressible.getSqmType();
		}
		// Use the relational java type to account for possible converters
		return getBasicTypeForJavaType( expressible.getRelationalJavaType().getJavaTypeClass() );
	}

	private static boolean matchesJavaType(SqmExpressible<?> type, Class<?> javaType) {
		assert javaType != null;
		return type != null && javaType.isAssignableFrom( type.getRelationalJavaType().getJavaTypeClass() );
	}


	private final ConcurrentHashMap<Type, BasicType<?>> basicTypeByJavaType = new ConcurrentHashMap<>();

	public <J> BasicType<J> getBasicTypeForGenericJavaType(Class<? super J> javaType, Type... typeArguments) {
		return getBasicTypeForJavaType( new ParameterizedTypeImpl( javaType, typeArguments, null ) );
	}

	public <J> BasicType<J> getBasicTypeForJavaType(Class<J> javaType) {
		return getBasicTypeForJavaType( (Type) javaType );
	}

	public <J> BasicType<J> getBasicTypeForJavaType(Type javaType) {
		final BasicType<?> existing = basicTypeByJavaType.get( javaType );
		if ( existing != null ) {
			//noinspection unchecked
			return (BasicType<J>) existing;
		}

		final BasicType<J> registeredType = basicTypeRegistry.getRegisteredType( javaType );
		if ( registeredType != null ) {
			basicTypeByJavaType.put( javaType, registeredType );
			return registeredType;
		}

		return null;
	}

	public <J> BasicType<J> standardBasicTypeForJavaType(Class<J> javaType) {
		if ( javaType == null ) {
			return null;
		}

		return standardBasicTypeForJavaType(
				javaType,
				javaTypeDescriptor -> new BasicTypeImpl<>(
						javaTypeDescriptor,
						javaTypeDescriptor.getRecommendedJdbcType( getCurrentBaseSqlTypeIndicators() )
				)
		);
	}

	public BasicType<?> standardBasicTypeForJavaType(Type javaType) {
		if ( javaType == null ) {
			return null;
		}

		return standardBasicTypeForJavaType(
				javaType,
				javaTypeDescriptor -> new BasicTypeImpl<>(
						javaTypeDescriptor,
						javaTypeDescriptor.getRecommendedJdbcType( getCurrentBaseSqlTypeIndicators() )
				)
		);
	}

	public <J> BasicType<J> standardBasicTypeForJavaType(
			Class<J> javaType,
			Function<JavaType<J>, BasicType<J>> creator) {
		return standardBasicTypeForJavaType( (Type) javaType, creator );
	}

	public <J> BasicType<J> standardBasicTypeForJavaType(
			Type javaType,
			Function<JavaType<J>, BasicType<J>> creator) {
		if ( javaType == null ) {
			return null;
		}
		//noinspection unchecked
		return (BasicType<J>) basicTypeByJavaType.computeIfAbsent(
				javaType,
				jt -> {
					// See if one exists in the BasicTypeRegistry and use that one if so
					final BasicType<J> registeredType = basicTypeRegistry.getRegisteredType( javaType );
					if ( registeredType != null ) {
						return registeredType;
					}

					// otherwise, apply the creator
					return creator.apply( javaTypeRegistry.resolveDescriptor( javaType ) );
				}
		);
	}

	public TemporalType getSqlTemporalType(SqmExpressible<?> type) {
		if ( type == null ) {
			return null;
		}
		return getSqlTemporalType( type.getRelationalJavaType().getRecommendedJdbcType( getCurrentBaseSqlTypeIndicators() ) );
	}

	public static TemporalType getSqlTemporalType(JdbcMapping jdbcMapping) {
		return getSqlTemporalType( jdbcMapping.getJdbcType() );
	}

	public static TemporalType getSqlTemporalType(JdbcMappingContainer jdbcMappings) {
		assert jdbcMappings.getJdbcTypeCount() == 1;
		return getSqlTemporalType( jdbcMappings.getSingleJdbcMapping().getJdbcType() );
	}

	public static TemporalType getSqlTemporalType(MappingModelExpressible<?> type) {
		if ( type instanceof BasicValuedMapping ) {
			return getSqlTemporalType( ( (BasicValuedMapping) type ).getJdbcMapping().getJdbcType() );
		}
		else if ( type instanceof EmbeddableValuedModelPart ) {
			// Handle the special embeddables for emulated offset/timezone handling
			final Class<?> javaTypeClass = ( (EmbeddableValuedModelPart) type ).getJavaType().getJavaTypeClass();
			if ( javaTypeClass == OffsetDateTime.class
					|| javaTypeClass == ZonedDateTime.class ) {
				return TemporalType.TIMESTAMP;
			}
			else if ( javaTypeClass == OffsetTime.class ) {
				return TemporalType.TIME;
			}
		}
		return null;
	}

	public static TemporalType getSqlTemporalType(JdbcType descriptor) {
		return getSqlTemporalType( descriptor.getDefaultSqlTypeCode() );
	}

	protected static TemporalType getSqlTemporalType(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case SqlTypes.TIMESTAMP:
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				return TemporalType.TIMESTAMP;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				return TemporalType.TIME;
			case SqlTypes.DATE:
				return TemporalType.DATE;
		}
		return null;
	}

	public static IntervalType getSqlIntervalType(JdbcMappingContainer jdbcMappings) {
		assert jdbcMappings.getJdbcTypeCount() == 1;
		return getSqlIntervalType( jdbcMappings.getSingleJdbcMapping().getJdbcType() );
	}

	public static IntervalType getSqlIntervalType(JdbcType descriptor) {
		return getSqlIntervalType( descriptor.getDefaultSqlTypeCode() );
	}

	protected static IntervalType getSqlIntervalType(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case SqlTypes.INTERVAL_SECOND:
				return IntervalType.SECOND;
		}
		return null;
	}

	public static boolean isJdbcTemporalType(SqmExpressible<?> type) {
		return matchesJavaType( type, Date.class );
	}

	public static boolean isDuration(SqmExpressible<?> type) {
		return matchesJavaType( type, Duration.class );
	}

	@Internal @SuppressWarnings("unchecked")
	public <J> MutabilityPlan<J> createMutabilityPlan(Class<? extends MutabilityPlan<?>> planClass) {
		if ( !scope.allowExtensionsInCdi ) {
			//noinspection rawtypes
			return (MutabilityPlan) FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( planClass );
		}

		final ManagedBean<? extends MutabilityPlan<?>> planBean =
				scope.getServiceRegistry().requireService( ManagedBeanRegistry.class )
						.getBean( planClass );
		return (MutabilityPlan<J>) planBean.getBeanInstance();
	}
}
