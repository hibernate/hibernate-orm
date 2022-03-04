/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.uuid.LocalObjectUuidHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.internal.ArrayTupleType;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.BasicTypeImpl;

import jakarta.persistence.TemporalType;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Defines a set of available Type instances as isolated from other configurations.  The
 * isolation is defined by each instance of a TypeConfiguration.
 * <p/>
 * Note that each Type is inherently "scoped" to a TypeConfiguration.  We only ever access
 * a Type through a TypeConfiguration - specifically the TypeConfiguration in effect for
 * the current persistence unit.
 * <p/>
 * Even though each Type instance is scoped to a TypeConfiguration, Types do not inherently
 * have access to that TypeConfiguration (mainly because Type is an extension contract - meaning
 * that Hibernate does not manage the full set of Types available in ever TypeConfiguration).
 * However Types will often want access to the TypeConfiguration, which can be achieved by the
 * Type simply implementing the {@link TypeConfigurationAware} interface.
 *
 * @author Steve Ebersole
 *
 * @since 5.3
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
		return scope.getCurrentBaseSqlTypeIndicators();
	}

	public Map<Integer, Set<String>> getJdbcToHibernateTypeContributionMap() {
		return jdbcToHibernateTypeContributionMap;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Scoping

	/**
	 * Obtain the MetadataBuildingContext currently scoping the
	 * TypeConfiguration.
	 *
	 * @apiNote This will throw an exception if the SessionFactory is not yet
	 * bound here.  See {@link Scope} for more details regarding the stages
	 * a TypeConfiguration goes through
	 *
	 * @return The MetadataBuildingContext
	 */
	public MetadataBuildingContext getMetadataBuildingContext() {
		return scope.getMetadataBuildingContext();
	}

	public void scope(MetadataBuildingContext metadataBuildingContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );
	}

	public MappingMetamodelImpl scope(SessionFactoryImplementor sessionFactory) {
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactoryImplementor [%s]", this, sessionFactory );

		if ( scope.getMetadataBuildingContext() == null ) {
			throw new IllegalStateException( "MetadataBuildingContext not known" );
		}

		scope.setSessionFactory( sessionFactory );
		sessionFactory.addObserver( this );

		return new MappingMetamodelImpl( sessionFactory, this );
	}

	/**
	 * Obtain the SessionFactory currently scoping the TypeConfiguration.
	 *
	 * @apiNote This will throw an exception if the SessionFactory is not yet
	 * bound here.  See {@link Scope} for more details regarding the stages
	 * a TypeConfiguration goes through (this is "runtime stage")
	 *
	 * @return The SessionFactory
	 *
	 * @throws IllegalStateException if the TypeConfiguration is currently not
	 * associated with a SessionFactory (in "runtime stage").
	 */
	public SessionFactoryImplementor getSessionFactory() {
		return scope.getSessionFactory();
	}

	/**
	 * Obtain the ServiceRegistry scoped to the TypeConfiguration.
	 *
	 * @apiNote Depending on what the {@link Scope} is currently scoped to will determine where the
	 * {@link ServiceRegistry} is obtained from.
	 *
	 * @return The ServiceRegistry
	 */
	public ServiceRegistry getServiceRegistry() {
		return scope.getServiceRegistry();
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
	 * Understands the following target type names for the cast() function:
	 *
	 * - String
	 * - Character
	 * - Byte, Integer, Long
	 * - Float, Double
	 * - Time, Date, Timestamp
	 * - LocalDate, LocalTime, LocalDateTime
	 * - BigInteger
	 * - BigDecimal
	 * - Binary
	 * - Boolean (fragile)
	 *
	 * (The type names are not case-sensitive.)
	 */
	public BasicValuedMapping resolveCastTargetType(String name) {
		switch ( name.toLowerCase() ) {
			case "string": return getBasicTypeForJavaType( String.class );
			case "character": return getBasicTypeForJavaType( Character.class );
			case "byte": return getBasicTypeForJavaType( Byte.class );
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
			case "binary": return getBasicTypeForJavaType( byte[].class );
			//this one is very fragile ... works well for BIT or BOOLEAN columns only
			//works OK, I suppose, for integer columns, but not at all for char columns
			case "boolean": return getBasicTypeForJavaType( Boolean.class );
			case "truefalse": return basicTypeRegistry.getRegisteredType( StandardBasicTypes.TRUE_FALSE.getName() );
			case "yesno": return basicTypeRegistry.getRegisteredType( StandardBasicTypes.YES_NO.getName() );
			case "numericboolean": return basicTypeRegistry.getRegisteredType( StandardBasicTypes.NUMERIC_BOOLEAN.getName() );
			default: {
				final BasicType<Object> registeredBasicType = basicTypeRegistry.getRegisteredType( name );
				if ( registeredBasicType != null ) {
					return registeredBasicType;
				}

				try {
					final ClassLoaderService cls = getServiceRegistry().getService( ClassLoaderService.class );
					final Class<?> javaTypeClass = cls.classForName( name );

					final JavaType<?> jtd = javaTypeRegistry.resolveDescriptor( javaTypeClass );
					final JdbcType jdbcType = jtd.getRecommendedJdbcType( getCurrentBaseSqlTypeIndicators() );
					return basicTypeRegistry.resolve( jtd, jdbcType );
				}
				catch (Exception ignore) {
				}

				throw new HibernateException( "unrecognized cast target type: " + name );
			}
		}
	}

	/**
	 * Encapsulation of lifecycle concerns for a TypeConfiguration:<ol>
	 *     <li>
	 *         "Boot" is where the {@link TypeConfiguration} is first
	 *         built as the boot-model ({@link org.hibernate.boot.model}) of
	 *         the user's domain model is converted into the runtime-model
	 *         ({@link org.hibernate.metamodel.model}). During this phase,
	 *         {@link #getMetadataBuildingContext()} will be accessible but
	 *         {@link #getSessionFactory} will throw an exception.
	 *     </li>
	 *     <li>
	 *         "Runtime" is where the runtime-model is accessible.  During this
	 *         phase {@link #getSessionFactory()} is accessible while
	 *         {@link #getMetadataBuildingContext()} will now throw an exception
	 *     </li>
	 *     <li>
	 *        "Sunset" is after the SessionFactory has been closed.  At this point, both
	 *        {@link #getSessionFactory()} and {@link #getMetadataBuildingContext()}
	 *        will now throw an exception
	 *     </li>
	 * </ol>
	 * <p/>
	 * {@link #getServiceRegistry()} is available for both "Boot" and "Runtime".
	 *
	 * Each stage or phase is consider a scope for the TypeConfiguration.
	 */
	private static class Scope implements Serializable {
		private final TypeConfiguration typeConfiguration;

		private transient MetadataBuildingContext metadataBuildingContext;
		private transient SessionFactoryImplementor sessionFactory;

		private String sessionFactoryName;
		private String sessionFactoryUuid;

		private final transient JdbcTypeIndicators currentSqlTypeIndicators = new JdbcTypeIndicators() {
			@Override
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}

			@Override
			public int getPreferredSqlTypeCodeForBoolean() {
				return Types.BOOLEAN;
			}
		};

		public Scope(TypeConfiguration typeConfiguration) {
			this.typeConfiguration = typeConfiguration;
		}

		public JdbcTypeIndicators getCurrentBaseSqlTypeIndicators() {
			return currentSqlTypeIndicators;
		}

		public MetadataBuildingContext getMetadataBuildingContext() {
			if ( metadataBuildingContext == null ) {
				throw new HibernateException( "TypeConfiguration is not currently scoped to MetadataBuildingContext" );
			}
			return metadataBuildingContext;
		}

		public ServiceRegistry getServiceRegistry() {
			if ( metadataBuildingContext != null ) {
				return metadataBuildingContext.getBootstrapContext().getServiceRegistry();
			}
			else if ( sessionFactory != null ) {
				return sessionFactory.getServiceRegistry();
			}
			return null;
		}

		public void setMetadataBuildingContext(MetadataBuildingContext metadataBuildingContext) {
			this.metadataBuildingContext = metadataBuildingContext;
		}

		public SessionFactoryImplementor getSessionFactory() {
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
		 * Used by TypeFactory scoping.
		 *
		 * @param factory The SessionFactory that the TypeFactory is being bound to
		 */
		void setSessionFactory(SessionFactoryImplementor factory) {
			if ( this.sessionFactory != null ) {
				log.scopingTypesToSessionFactoryAfterAlreadyScoped( this.sessionFactory, factory );
			}
			else {
				this.sessionFactoryUuid = factory.getUuid();
				String sfName = factory.getSessionFactoryOptions().getSessionFactoryName();
				if ( sfName == null ) {
					final CfgXmlAccessService cfgXmlAccessService = factory.getServiceRegistry()
							.getService( CfgXmlAccessService.class );
					if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
						sfName = cfgXmlAccessService.getAggregatedConfig().getSessionFactoryName();
					}
				}
				this.sessionFactoryName = sfName;
			}
			this.sessionFactory = factory;
		}

		public void unsetSessionFactory(SessionFactory factory) {
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
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final ConcurrentMap<ArrayCacheKey, ArrayTupleType> arrayTuples = new ConcurrentHashMap<>();

	public SqmExpressible<?> resolveTupleType(List<? extends SqmTypedNode<?>> typedNodes) {
		final SqmExpressible<?>[] components = new SqmExpressible<?>[typedNodes.size()];
		for ( int i = 0; i < typedNodes.size(); i++ ) {
			final SqmExpressible<?> sqmExpressible = typedNodes.get( i ).getNodeType();
			components[i] = sqmExpressible != null
					? sqmExpressible
					: getBasicTypeForJavaType( Object.class );
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
			// it's either addition/subtraction of durations
			// or prefix scalar multiplication of a duration
			return secondType;
		}
		else if ( firstType==null && getSqlTemporalType( secondType ) != null ) {
			// subtraction of a date or timestamp from a
			// parameter (which doesn't have a type yet)
			return getBasicTypeRegistry().getRegisteredType( Duration.class );
		}

		if ( secondType == null || firstType != null
				&& firstType.getExpressibleJavaType().isWider( secondType.getExpressibleJavaType() ) ) {
			return firstType;
		}
		return secondType;
	}

	private static boolean matchesJavaType(SqmExpressible<?> type, Class<?> javaType) {
		assert javaType != null;
		return type != null && javaType.isAssignableFrom( type.getExpressibleJavaType().getJavaTypeClass() );
	}


	private final ConcurrentHashMap<Class<?>, BasicType<?>> basicTypeByJavaType = new ConcurrentHashMap<>();

	public <J> BasicType<J> getBasicTypeForJavaType(Class<J> javaType) {
		final BasicType<?> existing = basicTypeByJavaType.get( javaType );
		if ( existing != null ) {
			//noinspection unchecked
			return (BasicType<J>) existing;
		}

		final BasicType<J> registeredType = getBasicTypeRegistry().getRegisteredType( javaType );
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

	public <J> BasicType<J> standardBasicTypeForJavaType(
			Class<J> javaType,
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
		return getSqlTemporalType( type.getExpressibleJavaType().getRecommendedJdbcType( getCurrentBaseSqlTypeIndicators() ) );
	}

	public static TemporalType getSqlTemporalType(JdbcMapping jdbcMapping) {
		return getSqlTemporalType( jdbcMapping.getJdbcType() );
	}

	public static TemporalType getSqlTemporalType(JdbcMappingContainer jdbcMappings) {
		assert jdbcMappings.getJdbcTypeCount() == 1;
		return getSqlTemporalType( jdbcMappings.getJdbcMappings().get( 0 ).getJdbcType() );
	}

	public static TemporalType getSqlTemporalType(MappingModelExpressible<?> type) {
		if ( type instanceof BasicValuedMapping ) {
			return getSqlTemporalType( ( (BasicValuedMapping) type ).getJdbcMapping().getJdbcType() );
		}
		return null;
	}

	public static TemporalType getSqlTemporalType(JdbcType descriptor) {
		return getSqlTemporalType( descriptor.getDefaultSqlTypeCode() );
	}

	protected static TemporalType getSqlTemporalType(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				return TemporalType.TIMESTAMP;
			case Types.TIME:
			case Types.TIME_WITH_TIMEZONE:
				return TemporalType.TIME;
			case Types.DATE:
				return TemporalType.DATE;
		}
		return null;
	}

	public static IntervalType getSqlIntervalType(JdbcMappingContainer jdbcMappings) {
		assert jdbcMappings.getJdbcTypeCount() == 1;
		return getSqlIntervalType( jdbcMappings.getJdbcMappings().get( 0 ).getJdbcType() );
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

}
