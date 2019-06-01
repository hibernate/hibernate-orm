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
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.persistence.EnumType;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.uuid.LocalObjectUuidHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationProcess;
import org.hibernate.metamodel.model.domain.spi.BasicTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.StandardBasicTypes.StandardBasicType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry;
import org.hibernate.type.internal.TypeConfigurationRegistry;

import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.query.BinaryArithmeticOperator.DIVIDE;

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

	private boolean initialized;

	// things available during both boot and runtime ("active") lifecycle phases
	private final transient JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;
	private final transient SqlTypeDescriptorRegistry sqlTypeDescriptorRegistry;
	private final transient BasicTypeRegistry basicTypeRegistry;

	private final transient Map<SqlTypeDescriptor,Map<BasicJavaDescriptor, SqlExpressableType>> jdbcValueMapperCache = new ConcurrentHashMap<>();

	private final Map<String, BasicValueMapper> namedMappers = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<StandardBasicType<?>,BasicValueMapper<?>> standardBasicTypeResolutionCache = new ConcurrentHashMap<>();


	public TypeConfiguration() {
		log.debugf( "Instantiating TypeConfiguration : %s", uuid );

		this.scope = new Scope( this );

		this.javaTypeDescriptorRegistry = new JavaTypeDescriptorRegistry( this );
		this.sqlTypeDescriptorRegistry = new SqlTypeDescriptorRegistry( this );

		this.basicTypeRegistry = new BasicTypeRegistry( this );

		StandardSpiBasicTypes.prime( this );

		this.initialized = true;

		TypeConfigurationRegistry.INSTANCE.registerTypeConfiguration( this );
	}

	public String getUuid() {
		return uuid;
	}

	public JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeConfiguration initialization incomplete; not yet ready for access" );
		}
		return javaTypeDescriptorRegistry;
	}

	public SqlTypeDescriptorRegistry getSqlTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeConfiguration initialization incomplete; not yet ready for access" );
		}
		return sqlTypeDescriptorRegistry;
	}

	public SqlTypeDescriptorIndicators getCurrentBaseSqlTypeIndicators() {
		return scope.getCurrentBaseSqlTypeIndicators();
	}

	/**
	 * Resolve the BasicValueMapper related with the StandardBasicType in relation to
	 * this TypeConfiguration.
	 *
	 * StandardBasicType references exist statically.  This call acts as a cacheable lookup
	 * for the related BasicValueMapper scoped to this TypeConfiguration
	 */
	@SuppressWarnings("unchecked")
	public <J> BasicTypeResolution<J> resolveStandardBasicType(StandardBasicType<J> standardBasicType) {
		return (BasicTypeResolution<J>) standardBasicTypeResolutionCache.computeIfAbsent(
				standardBasicType,
				key -> new StandardBasicTypeResolutionImpl<>( standardBasicType )
		);
	}

	public interface BasicTypeResolution<J> extends BasicValueMapper<J>, BasicValuedExpressableType<J>, BasicTypeDescriptor<J> {
		@Override
		default PersistenceType getPersistenceType() {
			return PersistenceType.BASIC;
		}
	}

	private class StandardBasicTypeResolutionImpl<J> implements BasicTypeResolution<J> {
		private final StandardBasicType<J> standardBasicType;
		private final SqlExpressableType sqlExpressableType;

		@SuppressWarnings("unchecked")
		public StandardBasicTypeResolutionImpl(StandardBasicType<J> standardBasicType) {
			this.standardBasicType = standardBasicType;
			this.sqlExpressableType = standardBasicType.getRelationalSqlTypeDescriptor().getSqlExpressableType(
					standardBasicType.getRelationalJavaTypeDescriptor(),
					TypeConfiguration.this
			);
		}

		@Override
		public Class<J> getJavaType() {
			return getDomainJavaDescriptor().getJavaType();
		}

		@Override
		public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
			return standardBasicType.getDomainJavaTypeDescriptor();
		}

		@Override
		public SqlExpressableType getSqlExpressableType() {
			return sqlExpressableType;
		}

		@Override
		public BasicValueConverter getValueConverter() {
			return standardBasicType.getValueConverter();
		}

		@Override
		public MutabilityPlan<J> getMutabilityPlan() {
			return standardBasicType.getMutabilityPlan();
		}

		@Override
		public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
			return standardBasicType.getDomainJavaTypeDescriptor();
		}
	}

	@SuppressWarnings("unchecked")
	public <J> BasicValuedExpressableType<J> standardExpressableTypeForJavaType(Class<J> javaType) {
		return standardExpressableTypeForJavaType(
				(BasicJavaDescriptor) getJavaTypeDescriptorRegistry().getDescriptor( javaType )
		);
	}

	private final ConcurrentHashMap<BasicJavaDescriptor<?>,BasicTypeResolution> basicTypeResolutionsByJavaType = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <J> BasicTypeResolution<J> standardExpressableTypeForJavaType(BasicJavaDescriptor<J> javaTypeDescriptor) {
		if ( javaTypeDescriptor == null ) {
			return null;
		}
		return basicTypeResolutionsByJavaType.computeIfAbsent(
				javaTypeDescriptor,
				StandardJavaTypeResolutionImpl::new
		);
	}

	private class StandardJavaTypeResolutionImpl<J> implements BasicTypeResolution<J> {

		private final BasicJavaDescriptor<J> javaTypeDescriptor;
		private final SqlTypeDescriptor sqlTypeDescriptor;

		private final SqlExpressableType sqlExpressableType;

		@SuppressWarnings("unchecked")
		public StandardJavaTypeResolutionImpl(BasicJavaDescriptor<J> javaTypeDescriptor) {
			this.javaTypeDescriptor = javaTypeDescriptor;

			this.sqlTypeDescriptor = javaTypeDescriptor.getJdbcRecommendedSqlType( getCurrentBaseSqlTypeIndicators() );

			this.sqlExpressableType = sqlTypeDescriptor.getSqlExpressableType( javaTypeDescriptor, TypeConfiguration.this );
		}

		@Override
		public Class<J> getJavaType() {
			return getDomainJavaDescriptor().getJavaType();
		}

		@Override
		public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
			return javaTypeDescriptor;
		}

		@Override
		public SqlExpressableType getSqlExpressableType() {
			return sqlExpressableType;
		}

		@Override
		public BasicValueConverter getValueConverter() {
			return null;
		}

		@Override
		public MutabilityPlan<J> getMutabilityPlan() {
			return javaTypeDescriptor.getMutabilityPlan();
		}

		@Override
		public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
			return javaTypeDescriptor;
		}

		@Override
		public void visitJdbcTypes(
				Consumer<SqlExpressableType> action,
				Clause clause,
				TypeConfiguration typeConfiguration) {
			action.accept( getSqlExpressableType() );
		}
	}

	public BasicTypeRegistry getBasicTypeRegistry() {
		return basicTypeRegistry;
	}

	/**
	 * Resolve the SqlExpressableType for a SqlTypeDescriptor and
	 * BasicJavaDescriptor combo.  This form creates one if not already
	 * cached using the passed `creator`
	 */
	public SqlExpressableType resolveSqlExpressableType(
			SqlTypeDescriptor sqlTypeDescriptor,
			BasicJavaDescriptor javaDescriptor,
			java.util.function.Function<BasicJavaDescriptor, SqlExpressableType> creator) {
		final Map<BasicJavaDescriptor, SqlExpressableType> cacheForSqlType = jdbcValueMapperCache.computeIfAbsent(
				sqlTypeDescriptor,
				x -> new ConcurrentHashMap<>()
		);

		return cacheForSqlType.computeIfAbsent( javaDescriptor, x -> creator.apply( javaDescriptor ) );
	}

	/**
	 * Resolve the SqlExpressableType for a SqlTypeDescriptor and
	 * BasicJavaDescriptor combo.  This form throws an exception if
	 * not already cached.
	 */
	public SqlExpressableType resolveSqlExpressableType(
			SqlTypeDescriptor sqlTypeDescriptor,
			BasicJavaDescriptor javaDescriptor) {
		final Map<BasicJavaDescriptor, SqlExpressableType> cacheForSqlType = jdbcValueMapperCache.computeIfAbsent(
				sqlTypeDescriptor,
				x -> new ConcurrentHashMap<>()
		);

		final SqlExpressableType sqlExpressableType = cacheForSqlType.get( javaDescriptor );
		if ( sqlExpressableType == null ) {
			throw new IllegalArgumentException(
					"No SqlExpressableType cached for [" + sqlTypeDescriptor + "] and ["
							+ javaDescriptor + "] combination"
			);
		}

		return sqlExpressableType;
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
	 */
	public MetadataBuildingContext getMetadataBuildingContext() {
		return scope.getMetadataBuildingContext();
	}

	public void scope(MetadataBuildingContext metadataBuildingContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );
	}

	public MetamodelImplementor scope(SessionFactoryImplementor sessionFactory, BootstrapContext bootstrapContext) {
		assert scope.metadataBuildingContext != null;
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactoryImpl [%s]", this, sessionFactory );
		scope.setSessionFactory( sessionFactory );
		sessionFactory.addObserver( this );
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactory [%s]", this, sessionFactory );

		return RuntimeModelCreationProcess.execute(
				sessionFactory,
				bootstrapContext,
				scope.getMetadataBuildingContext()
		);
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
		scope.unsetMetadataBuildingContext();
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		log.tracef( "Handling #sessionFactoryClosed from [%s] for TypeConfiguration", factory );

		TypeConfigurationRegistry.INSTANCE.deregisterTypeConfiguration( this );

		scope.unsetSessionFactory( factory );

		// todo (6.0) : finish this
		//		release Database, descriptor Maps, etc... things that are only
		// 		valid while the TypeConfiguration is scoped to SessionFactory
	}

	public void registerBasicValueMapper(BasicValueMapper valueMapper, String... registrationKeys) {
		for ( String registrationKey : registrationKeys ) {
			namedMappers.put( registrationKey, valueMapper );
		}
	}

	public BasicValueMapper getNamedBasicValueMapper(String name) {
		return namedMappers.get( name );
	}


	// todo (6.0) - have this algorithm be extendable by users.
	// 		I have received at least one user request for this, and I can completely see the
	// 		benefit of this as they described it.  Basically consider a query containing
	// 		`p.x + p.y`.  If `y` is a standard integer type, but `x` is a custom (user) integral
	// 		type, then what is the type of the arithmetic expression?  From the HipChat discussion:
	//
	//		[8:18 AM] Steve Ebersole: btw... what got me started thinking about this is thinking of ways to allow custom hooks into the types of literals recognized (and how) and the types of validation checks we do
	//		[8:18 AM] Steve Ebersole: allowing custom literal types becomes easy(er) if we follow the escape-like syntax
	//		[8:19 AM] Steve Ebersole: {[something] ...}
	//		[8:20 AM] Steve Ebersole: where `{[something]` triggers recognition of a literal
	//		[8:20 AM] Steve Ebersole: and `[something]` is a key to some registered resolver
	//		[8:21 AM] Steve Ebersole: e.g. for `{ts '2017-04-26 ...'}` we'd grab the timestamp literal handler
	//		[8:21 AM] Steve Ebersole: because of the `ts`
	//
	interface CustomExpressionTypeResolver {
		BasicValuedExpressableType resolveArithmeticType(
				BasicValuedExpressableType firstType,
				BasicValuedExpressableType secondType,
				boolean isDivision);

		BasicValuedExpressableType resolveSumFunctionType(BasicValuedExpressableType argumentType);

		BasicType resolveCastTargetType(String name);
	}
	//
	// A related discussion is recognition of a literal in the HQL, specifically for custom types.  From the same HipChat discussion:
	//		- allowing custom literal types becomes	easy(er) if we follow the escape-like syntax:
	//		- `{[something] ...}`
	//		- where `{` triggers recognition of a literal (by convention)
	//		- and `[something]` is a key to a registered (custom) resolver
	//
	interface HqlLiteralResolver {
		String getKey();

		<T> SqmLiteral<T> resolveLiteral(String literal);
	}
	//
	//		I say related because both deal with custom user types as used in a SQM.

	/**
	 * @see QueryHelper#highestPrecedenceType2
	 */
	public BasicValuedExpressableType<?> resolveArithmeticType(
			BasicValuedExpressableType<?> firstType,
			BasicValuedExpressableType<?> secondType,
			BinaryArithmeticOperator operator) {
		return resolveArithmeticType( firstType, secondType, operator == DIVIDE );
	}

	/**
	 * Determine the result type of an arithmetic operation as defined by the
	 * rules in section 6.5.7.1.
	 *
	 * @see QueryHelper#highestPrecedenceType2
	 */
	public BasicValuedExpressableType<?> resolveArithmeticType(
			BasicValuedExpressableType<?> firstType,
			BasicValuedExpressableType<?> secondType,
			boolean isDivision) {

		if ( isTemporalType( firstType ) ) {
			if ( secondType==null || isTemporalType( secondType ) ) {
				// special case for subtraction of two dates
				// or timestamps resulting in a duration
				return getBasicTypeRegistry().getBasicType( Duration.class );
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
		else if ( firstType==null && isTemporalType( secondType ) ) {
			// subtraction of a date or timestamp from a
			// parameter (which doesn't have a type yet)
			return getBasicTypeRegistry().getBasicType( Duration.class );
		}

		if ( isDivision ) {
			// covered under the note in 6.5.7.1 discussing the unportable
			// "semantics of the SQL division operation"..
			return getBasicTypeRegistry().getBasicType( Number.class );
		}

		// non-division

		if ( matchesJavaType( firstType, Double.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Double.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Float.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Float.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, BigDecimal.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, BigDecimal.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, BigInteger.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, BigInteger.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Long.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Long.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Integer.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Integer.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Short.class ) ) {
			return getBasicTypeRegistry().getBasicType( Integer.class );
		}
		else if ( matchesJavaType( secondType, Short.class ) ) {
			return getBasicTypeRegistry().getBasicType( Integer.class );
		}
		else {
			return getBasicTypeRegistry().getBasicType( Number.class );
		}
	}

	public static boolean isDuration(ExpressableType<?> type) {
		return matchesJavaType( type, Duration.class );
	}

	public static boolean isJDBCTemporalType(ExpressableType<?> type) {
		return matchesJavaType( type, java.util.Date.class );
	}

	public boolean isTemporalType(ExpressableType<?> type) {
		return type != null
			&& isTemporalType( type.getJavaTypeDescriptor().getJdbcRecommendedSqlType( getCurrentBaseSqlTypeIndicators() ) );
	}

	public boolean isTimestampType(ExpressableType<?> type) {
		return type != null
			&& isTimestampType( type.getJavaTypeDescriptor().getJdbcRecommendedSqlType( getCurrentBaseSqlTypeIndicators() ) );
	}

	public static boolean isTimestampType(SqlExpressableType type) {
		return isTimestampType( type.getSqlTypeDescriptor() );
	}

	public static boolean isTimestampType(SqlTypeDescriptor descriptor) {
		int jdbcTypeCode = descriptor.getJdbcTypeCode();
		return jdbcTypeCode == Types.TIMESTAMP
			|| jdbcTypeCode == Types.TIMESTAMP_WITH_TIMEZONE;
	}

	public static boolean isTemporalType(SqlExpressableType type) {
		return isTemporalType( type.getSqlTypeDescriptor() );
	}

	public static boolean isTemporalType(SqlTypeDescriptor descriptor) {
		int jdbcTypeCode = descriptor.getJdbcTypeCode();
		return jdbcTypeCode == Types.TIMESTAMP
			|| jdbcTypeCode == Types.TIMESTAMP_WITH_TIMEZONE
			|| jdbcTypeCode == Types.TIME
			|| jdbcTypeCode == Types.TIME_WITH_TIMEZONE
			|| jdbcTypeCode == Types.DATE;
	}

	@SuppressWarnings("unchecked")
	private static boolean matchesJavaType(ExpressableType<?> type, Class javaType) {
		assert javaType != null;
		return type != null && javaType.isAssignableFrom( type.getJavaType() );
	}

	public BasicValuedExpressableType resolveSumFunctionType(BasicValuedExpressableType argumentType) {
			if ( matchesJavaType( argumentType, Double.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, Float.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, BigDecimal.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, BigInteger.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, Long.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, Integer.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, Short.class ) ) {
				return getBasicTypeRegistry().getBasicType( Integer.class );
			}
			else {
				return getBasicTypeRegistry().getBasicType( Number.class );
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
	public BasicValuedExpressableType<?> resolveCastTargetType(String name) {
		switch ( name.toLowerCase() ) {
			case "string": return standardExpressableTypeForJavaType( String.class );
			case "character": return standardExpressableTypeForJavaType( Character.class );
			case "byte": return standardExpressableTypeForJavaType( Byte.class );
			case "integer": return standardExpressableTypeForJavaType( Integer.class );
			case "long": return standardExpressableTypeForJavaType( Long.class );
			case "float": return standardExpressableTypeForJavaType( Float.class );
			case "double": return standardExpressableTypeForJavaType( Double.class );
			case "time": return standardExpressableTypeForJavaType( Time.class );
			case "date": return standardExpressableTypeForJavaType( Date.class );
			case "timestamp": return standardExpressableTypeForJavaType( Timestamp.class );
			case "localtime": return standardExpressableTypeForJavaType( LocalTime.class );
			case "localdate": return standardExpressableTypeForJavaType( LocalDate.class );
			case "localdatetime": return standardExpressableTypeForJavaType( LocalDateTime.class );
			case "biginteger": return standardExpressableTypeForJavaType( BigInteger.class );
			case "bigdecimal": return standardExpressableTypeForJavaType( BigDecimal.class );
			case "binary":
				//TODO: why does this not work:
//				standardExpressableTypeForJavaType( byte[].class );
				return resolveStandardBasicType( StandardSpiBasicTypes.BINARY );
			//this one is very fragile ... works well for BIT or BOOLEAN columns only
			//works OK, I suppose, for integer columns, but not at all for char columns
			case "boolean": return standardExpressableTypeForJavaType( Boolean.class );
			default: throw new HibernateException( "unrecognized cast target type: " + name );
		}
	}

	/**
	 * Encapsulation of lifecycle concerns for a TypeConfiguration, mainly in
	 * regards to eventually being associated with a SessionFactory.  Goes
	 * 3 "lifecycle" stages, pertaining to {@link #getMetadataBuildingContext()}
	 * and {@link #getSessionFactory()}:
	 *
	 * 		* "Initialization" is where the {@link TypeConfiguration} is first
	 * 			built as the "boot model" ({@link org.hibernate.boot.model}) of
	 * 			the user's domain model is converted into the "runtime model"
	 * 			({@link org.hibernate.metamodel.model}).  During this phase,
	 * 			{@link #getMetadataBuildingContext()} will be accessible but
	 * 			{@link #getSessionFactory} will throw an exception.
	 * 		* "Runtime" is where the "runtime model" is accessible while the
	 * 			SessionFactory is still unclosed.  During this phase
	 * 			{@link #getSessionFactory()} is accessible while
	 * 			{@link #getMetadataBuildingContext()} will now throw an
	 * 			exception
	 * 		* "Sunset" is after the SessionFactory has been closed.  During this
	 * 			phase both {@link #getSessionFactory()} and
	 * 			{@link #getMetadataBuildingContext()} will now throw an exception
	 *
	 * Each stage or phase is consider a "scope" for the TypeConfiguration.
	 */
	private static class Scope implements Serializable {
		private final TypeConfiguration typeConfiguration;

		private transient MetadataBuildingContext metadataBuildingContext;
		private transient SessionFactoryImplementor sessionFactory;
		private transient SqlTypeDescriptorIndicators currentSqlTypeIndicators = new SqlTypeDescriptorIndicators() {
			@Override
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}

			@Override
			public int getPreferredSqlTypeCodeForBoolean() {
				return Types.BOOLEAN;
			}
		};

		private String sessionFactoryName;
		private String sessionFactoryUuid;

		public Scope(TypeConfiguration typeConfiguration) {
			this.typeConfiguration = typeConfiguration;
		}

		public SqlTypeDescriptorIndicators getCurrentBaseSqlTypeIndicators() {
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

			this.currentSqlTypeIndicators = new SqlTypeDescriptorIndicators() {
				private final boolean globalNationalization = metadataBuildingContext.getBuildingOptions().useNationalizedCharacterData();
				private final int preferredBooleanSqlTypeCode = metadataBuildingContext.getPreferredSqlTypeCodeForBoolean();
				private final EnumType implicitEnumStorage = metadataBuildingContext.getBuildingOptions().getImplicitEnumType();

				@Override
				public boolean isNationalized() {
					return globalNationalization;
				}

				@Override
				public int getPreferredSqlTypeCodeForBoolean() {
					return preferredBooleanSqlTypeCode;
				}

				@Override
				public EnumType getEnumeratedType() {
					return implicitEnumStorage;
				}

				@Override
				public TypeConfiguration getTypeConfiguration() {
					return typeConfiguration;
				}
			};
		}

		public void unsetMetadataBuildingContext() {
			this.metadataBuildingContext = null;
		}

		public SessionFactoryImplementor getSessionFactory() {
			if ( sessionFactory == null ) {
				if ( sessionFactoryName == null && sessionFactoryUuid == null ) {
					throw new HibernateException( "TypeConfiguration was not yet scoped to SessionFactory" );
				}
				sessionFactory = (SessionFactoryImplementor) SessionFactoryRegistry.INSTANCE.findSessionFactory(
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
					sessionFactory = (SessionFactoryImplementor) SessionFactoryRegistry.INSTANCE.findSessionFactory(
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
	// Custom serialization hook

	private Object readResolve() throws InvalidObjectException {
		log.trace( "Resolving serialized TypeConfiguration - readResolve" );
		return TypeConfigurationRegistry.INSTANCE.findTypeConfiguration( getUuid() );
	}
}
