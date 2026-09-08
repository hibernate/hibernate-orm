/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.dialect.type.spi.DdlTypeBuilder;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.TemporalType;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.function.DerbyLpadEmulation;
import org.hibernate.community.dialect.function.DerbyRpadEmulation;
import org.hibernate.community.dialect.lock.internal.DerbyLockingSupport;
import org.hibernate.community.dialect.pagination.DerbyLimitHandler;
import org.hibernate.community.dialect.sequence.DerbySequenceSupport;
import org.hibernate.community.dialect.temptable.internal.DerbyLocalTemporaryTableStrategy;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.ChrLiteralEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.community.dialect.identity.internal.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.pagination.spi.AbstractLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.dialect.lock.spi.StandardLockingClauseStrategies;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcType;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;
import java.util.Set;

import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * A {@linkplain Dialect SQL dialect} for Apache Derby.
 *
 * @author Simon Johnston
 * @author Gavin King
 *
 */
public class DerbyLegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalFormatSupport getTemporalFormatSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private static final NamespaceSupport NAMESPACE_SUPPORT = new CommunityNamespaceSupport(
			false,
			CommunityNamespaceSupport::unsupportedCatalog,
			CommunityNamespaceSupport::unsupportedCatalog,
			true,
			name -> new String[] { "create schema " + name },
			name -> new String[] { "drop schema " + name + " restrict" }
	);
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( 31 )
			.defaultTimestampPrecision( 9 )
			.maxTimestampPrecision( 9 )
			.floatPrecision( 23 ).doublePrecision( 52 )
			.maxVarcharLength( 32_672 ).maxVarcharCapacity( 32_700 )
			.maxNVarcharLength( 32_672 ).maxNVarcharCapacity( 32_672 )
			.maxVarbinaryLength( 32_672 ).maxVarbinaryCapacity( 32_672 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	// KNOWN LIMITATIONS:

	// no support for nationalized data (nchar, nvarchar, nclob)
	// * limited set of fields for extract()
	//   (no 'day of xxxx', nor 'week of xxxx')
	// * no support for format()
	// * pad() can only pad with blanks
	// * can't cast String to Binary
	// * can't select a parameter unless wrapped
	//   in a cast or function call

	private final LimitHandler limitHandler = getVersion().isBefore( 10, 5 )
			? AbstractLimitHandler.NO_LIMIT
			: new DerbyLimitHandler( getVersion().isSameOrAfter( 10, 6 ) );

	public DerbyLegacyDialect() {
		this( DatabaseVersion.make( 10, 0 ) );
	}

	public DerbyLegacyDialect(DatabaseVersion version) {
		super(version);
	}

	public DerbyLegacyDialect(DialectResolutionInfo info) {
		super(info);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN ->  getVersion().isBefore( 10, 7 ) ? "smallint" : super.columnType( sqlTypeCode );
			//no tinyint
			case TINYINT -> "smallint";
			// HHH-12827: map them both to the same type to avoid problems with schema update
			// Note that 31 is the maximum precision Derby supports
			case NUMERIC -> columnType( DECIMAL );
			case VARBINARY -> "varchar($l) for bit data";
			case NCHAR -> columnType( CHAR );
			case NVARCHAR -> columnType( VARCHAR );
			case BLOB -> "blob";
			case CLOB, NCLOB -> "clob";
			case TIME, TIME_WITH_TIMEZONE -> "time";
			case TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> "timestamp";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		int varcharDdlTypeCapacity = 32_672;

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( VARBINARY, columnType( LONG32VARBINARY ), this )
						.lobKind( getLobSupport().isLobType( LONG32VARBINARY )
										? DdlTypeBuilder.LobKind.BIGGEST
										: DdlTypeBuilder.LobKind.NONE )
						.castTypeName( columnType( VARBINARY ) )
						.withTypeCapacity( varcharDdlTypeCapacity, columnType( VARBINARY ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( VARCHAR, columnType( LONG32VARCHAR ), this )
						.lobKind( getLobSupport().isLobType( LONG32VARCHAR )
										? DdlTypeBuilder.LobKind.BIGGEST
										: DdlTypeBuilder.LobKind.NONE )
						.castTypeName( columnType( VARCHAR ) )
						.withTypeCapacity( varcharDdlTypeCapacity, columnType( VARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( NVARCHAR, columnType( LONG32VARCHAR ), this )
						.lobKind( getLobSupport().isLobType( LONG32NVARCHAR )
										? DdlTypeBuilder.LobKind.BIGGEST
										: DdlTypeBuilder.LobKind.NONE )
						.castTypeName( columnType( NVARCHAR ) )
						.withTypeCapacity( varcharDdlTypeCapacity, columnType( NVARCHAR ) )
						.build()
		);

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( BINARY, columnType( LONG32VARBINARY ), this )
						.lobKind( getLobSupport().isLobType( LONG32VARBINARY )
										? DdlTypeBuilder.LobKind.BIGGEST
										: DdlTypeBuilder.LobKind.NONE )
						.castTypeName( columnType( VARBINARY ) )
						.withTypeCapacity( 254, "char($l) for bit data" )
						.withTypeCapacity( varcharDdlTypeCapacity, columnType( VARBINARY ) )
						.build()
		);

		// This is the maximum size for the CHAR datatype on Derby
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( CHAR, columnType( LONG32VARCHAR ), this )
						.lobKind( getLobSupport().isLobType( LONG32VARCHAR )
										? DdlTypeBuilder.LobKind.BIGGEST
										: DdlTypeBuilder.LobKind.NONE )
						.castTypeName( columnType( CHAR ) )
						.withTypeCapacity( 254, columnType( CHAR ) )
						.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), columnType( VARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( NCHAR, columnType( LONG32NVARCHAR ), this )
						.lobKind( getLobSupport().isLobType( LONG32NVARCHAR )
										? DdlTypeBuilder.LobKind.BIGGEST
										: DdlTypeBuilder.LobKind.NONE )
						.castTypeName( columnType( NCHAR ) )
						.withTypeCapacity( 254, columnType( NCHAR ) )
						.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), columnType( NVARCHAR ) )
						.build()
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return getVersion().isBefore( 10, 7 )
				? Types.SMALLINT
				: Types.BOOLEAN;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 15 ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		final DdlTypeRegistry ddlTypeRegistry = functionContributions.getTypeConfiguration().getDdlTypeRegistry();
		final CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

		// Derby needs an actual argument type for aggregates like SUM, AVG, MIN, MAX to determine the result type
		functionFactory.aggregates( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionContributions.getFunctionRegistry().register(
				"count",
				new CountFunction(
						this,
						functionContributions.getTypeConfiguration(),
						SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
						"||",
						ddlTypeRegistry.getDescriptor( VARCHAR )
								.getCastTypeName( Size.nil(), stringType, ddlTypeRegistry ),
						true
				)
		);
		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.DEFAULT );

		// Note that Derby does not have chr() / ascii() functions.
		// It does have a function named char(), but it's really a
		// sort of to_char() function.

		// We register an emulation instead, that can at least translate integer literals
		functionContributions.getFunctionRegistry().register(
				"chr",
				new ChrLiteralEmulation( functionContributions.getTypeConfiguration() )
		);

		functionFactory.concat_pipeOperator();
		functionFactory.cot();
		functionFactory.degrees();
		functionFactory.radians();
		functionFactory.log10();
		functionFactory.sinh();
		functionFactory.cosh();
		functionFactory.tanh();
		functionFactory.pi();
		functionFactory.rand();
		functionFactory.trim1();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.varPopSamp();
		functionFactory.stddevPopSamp();
		functionFactory.substring_substr();
		functionFactory.leftRight_substrLength();
		functionFactory.characterLength_length( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.power_expLn();
		functionFactory.round_floor();
		functionFactory.trunc_floor();
		functionFactory.octetLength_pattern( "length(?1)", SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.bitLength_pattern( "length(?1)*8", SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );

		functionContributions.getFunctionRegistry().register(
				"concat",
				new CastingConcatFunction(
						this,
						"||",
						true,
						SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
						functionContributions.getTypeConfiguration()
				)
		);

		//no way I can see to pad with anything other than spaces
		functionContributions.getFunctionRegistry().register( "lpad", new DerbyLpadEmulation( functionContributions.getTypeConfiguration() ) );
		functionContributions.getFunctionRegistry().register( "rpad", new DerbyRpadEmulation( functionContributions.getTypeConfiguration() ) );
		functionContributions.getFunctionRegistry().register( "least", new CaseLeastGreatestEmulation( true ) );
		functionContributions.getFunctionRegistry().register( "greatest", new CaseLeastGreatestEmulation( false ) );
		functionContributions.getFunctionRegistry().register( "overlay", new InsertSubstringOverlayEmulation( functionContributions.getTypeConfiguration(), true ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new DerbyLegacySqlAstTranslator<>( request );
			}
		};
	}

	/**
	 * Derby doesn't have an extract() function, and has
	 * no functions at all for calendaring, but we can
	 * emulate the most basic functionality of extract()
	 * using the functions it does have.
	 *
	 * The only supported {@link TemporalUnit}s are:
	 * {@link TemporalUnit#YEAR},
	 * {@link TemporalUnit#MONTH}
	 * {@link TemporalUnit#DAY},
	 * {@link TemporalUnit#HOUR},
	 * {@link TemporalUnit#MINUTE},
	 * {@link TemporalUnit#SECOND} (along with
	 * {@link TemporalUnit#NANOSECOND},
	 * {@link TemporalUnit#DATE}, and
	 * {@link TemporalUnit#TIME}, which are desugared
	 * by the parser).
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case DAY_OF_MONTH -> "day(?2)";
			case DAY_OF_YEAR -> "({fn timestampdiff(sql_tsi_day,date(char(year(?2),4)||'-01-01'),?2)}+1)";
			// Use the approach as outlined here: https://stackoverflow.com/questions/36357013/day-of-week-from-seconds-since-epoch
			case DAY_OF_WEEK -> "(mod(mod({fn timestampdiff(sql_tsi_day,{d '1970-01-01'},?2)}+4,7)+7,7)+1)";
			// Use the approach as outlined here: https://www.sqlservercentral.com/articles/a-simple-formula-to-calculate-the-iso-week-number
			// In SQL Server terms this is (DATEPART(dy,DATEADD(dd,DATEDIFF(dd,'17530101',@SomeDate)/7*7,'17530104'))+6)/7
			case WEEK -> "(({fn timestampdiff(sql_tsi_day,date(char(year(?2),4)||'-01-01'),{fn timestampadd(sql_tsi_day,{fn timestampdiff(sql_tsi_day,{d '1753-01-01'},?2)}/7*7,{d '1753-01-04'})})}+7)/7)";
			case QUARTER -> "((month(?2)+2)/3)";
			case EPOCH -> "{fn timestampdiff(sql_tsi_second,{ts '1970-01-01 00:00:00'},?2)}";
			default -> "?1(?2)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			case WEEK, DAY_OF_YEAR, DAY_OF_WEEK -> throw new UnsupportedOperationException("field type not supported on Derby: " + unit);
			case DAY_OF_MONTH -> "day";
			default -> TemporalOperationSupports.standard().translateExtractField(unit);
		};
	}

	/**
	 * Derby does have a real {@link Types#BOOLEAN}
	 * type, but it doesn't know how to cast to it. Worse,
	 * Derby makes us use the {@code double()} function to
	 * cast things to its floating point types.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		switch ( to ) {
			case FLOAT:
				return "cast(double(?1) as real)";
			case DOUBLE:
				return "double(?1)";
			case STRING:
				// Derby madness http://db.apache.org/derby/docs/10.8/ref/rrefsqlj33562.html
				// With a nice rant: https://blog.jooq.org/2011/10/29/derby-casting-madness-the-sequel/
				// See https://issues.apache.org/jira/browse/DERBY-2072

				// Since numerics can't be cast to varchar directly, use char(254) i.e. with the maximum char capacity
				// as an intermediate type before converting to varchar
				switch ( from ) {
					case FLOAT:
					case DOUBLE:
						// Derby can't cast to char directly, but needs to be cast to decimal first...
						return "cast(trim(cast(cast(?1 as decimal(" + getTypeSizingProfile().defaultDecimalPrecision() + "," + BigDecimalJavaType.INSTANCE.getDefaultSqlScale( this, null ) + ")) as char(254))) as ?2)";
					case INTEGER:
					case LONG:
					case FIXED:
						return "cast(trim(cast(?1 as char(254))) as ?2)";
					case DATE:
						// The maximum length of a date
						return "cast(?1 as varchar(10))";
					case TIME:
						// The maximum length of a time
						return "cast(?1 as varchar(8))";
					case TIMESTAMP:
						// The maximum length of a timestamp
						return "cast(?1 as varchar(30))";
				}
				break;
		}
		return super.castPattern( from, to );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				return "{fn timestampadd(sql_tsi_frac_second,mod(bigint(?2),1000000000),{fn timestampadd(sql_tsi_second,bigint((?2)/1000000000),?3)})}";
			default:
				return "{fn timestampadd(sql_tsi_?1,bigint(?2),?3)}";
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch (unit) {
			case NANOSECOND, NATIVE -> "{fn timestampdiff(sql_tsi_frac_second,?2,?3)}";
			default -> "{fn timestampdiff(sql_tsi_?1,?2,?3)}";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		if ( getVersion().isBefore( 10, 7 ) ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return getVersion().isBefore( 10, 6 )
				? super.getSequenceSupport()
				: DerbySequenceSupport.INSTANCE;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder(
					"select sys.sysschemas.schemaname as sequence_schema,sys.syssequences.* from sys.syssequences left join sys.sysschemas on sys.syssequences.schemaid=sys.sysschemas.schemaid"
			)
			.withoutCatalog()
			.sequenceNameColumn( "sequencename" )
			.startValueColumn( "startvalue" )
			.minimumValueColumn( "minimumvalue" )
			.maximumValueColumn( "maximumvalue" )
			.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion().isBefore( 10, 6 )
				? SequenceInformationExtractors.none()
				: SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NAMESPACE_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		return DB2Dialect.selectNullString( sqlTypeMapping.getJdbcMapping().getJdbcType().getDdlTypeCode() );
	}

	@Override
	protected LockingClauseStrategy buildLockingClauseStrategy(
			PessimisticLockKind lockKind,
			RowLockStrategy rowLockStrategy,
			LockOptions lockOptions,
			Set<NavigablePath> rootPathsForLocking) {
		return StandardLockingClauseStrategies.standard(
				request -> getLockingSupport().getLockingClauseRenderer().render( request ) + " with rs",
				lockKind,
				rowLockStrategy,
				lockOptions,
				rootPathsForLocking
		);
	}






	@Override
	public LockingSupport getLockingSupport() {
		return DerbyLockingSupport.INSTANCE;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.ORDER_BY, getVersion().isSameOrAfter( 10, 5 ) )
				.feature( SubquerySupport.Feature.MUTATION_JOIN, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "values current timestamp" );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return DB2IdentityColumnSupport.INSTANCE;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		//checked on Derby 10.14
		return TupleCountSupport.NONE;
	}

	@Override
	public ExpressionCoercionSupport getExpressionCoercionSupport() {
		return ExpressionCoercionSupport.builder()
				.requirements( ExpressionCoercionSupport.Requirement.CAST_NON_STRING_CONCATENATION_ARGUMENTS )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		if ( getVersion().isBefore( 10, 7 ) ) {
			jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, SmallIntJdbcType.INSTANCE );
		}
		jdbcTypeRegistry.addDescriptor( Types.TIMESTAMP_WITH_TIMEZONE, TimestampJdbcType.INSTANCE );

		// Derby requires a custom binder for binding untyped nulls that resolves the type through the statement
		typeContributions.contributeJdbcType( ObjectNullResolvingJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullResolvingJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
				)
		);
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return new TemplatedViolatedConstraintNameExtractor( sqle -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
			if ( sqlState != null ) {
				switch ( sqlState ) {
					case "23505":
						return TemplatedViolatedConstraintNameExtractor.extractUsingTemplate(
								"'", "'",
								sqle.getMessage()
						);
				}
			}
			return null;
		} );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
//				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			final String constraintName;

			if ( sqlState != null ) {
				switch ( sqlState ) {
					case "23505":
						// Unique constraint violation
						constraintName = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
						return new ConstraintViolationException(
								message,
								sqlException,
								sql,
								ConstraintViolationException.ConstraintKind.UNIQUE,
								constraintName
						);
					case "40XL1", "40XL2":
						return new LockTimeoutException( message, sqlException, sql );
				}
			}
			return null;
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		throw new UnsupportedOperationException("format() function not supported on Derby");
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "ADD" );
		registration.registerKeyword( "ALL" );
		registration.registerKeyword( "ALLOCATE" );
		registration.registerKeyword( "ALTER" );
		registration.registerKeyword( "AND" );
		registration.registerKeyword( "ANY" );
		registration.registerKeyword( "ARE" );
		registration.registerKeyword( "AS" );
		registration.registerKeyword( "ASC" );
		registration.registerKeyword( "ASSERTION" );
		registration.registerKeyword( "AT" );
		registration.registerKeyword( "AUTHORIZATION" );
		registration.registerKeyword( "AVG" );
		registration.registerKeyword( "BEGIN" );
		registration.registerKeyword( "BETWEEN" );
		registration.registerKeyword( "BIT" );
		registration.registerKeyword( "BOOLEAN" );
		registration.registerKeyword( "BOTH" );
		registration.registerKeyword( "BY" );
		registration.registerKeyword( "CALL" );
		registration.registerKeyword( "CASCADE" );
		registration.registerKeyword( "CASCADED" );
		registration.registerKeyword( "CASE" );
		registration.registerKeyword( "CAST" );
		registration.registerKeyword( "CHAR" );
		registration.registerKeyword( "CHARACTER" );
		registration.registerKeyword( "CHECK" );
		registration.registerKeyword( "CLOSE" );
		registration.registerKeyword( "COLLATE" );
		registration.registerKeyword( "COLLATION" );
		registration.registerKeyword( "COLUMN" );
		registration.registerKeyword( "COMMIT" );
		registration.registerKeyword( "CONNECT" );
		registration.registerKeyword( "CONNECTION" );
		registration.registerKeyword( "CONSTRAINT" );
		registration.registerKeyword( "CONSTRAINTS" );
		registration.registerKeyword( "CONTINUE" );
		registration.registerKeyword( "CONVERT" );
		registration.registerKeyword( "CORRESPONDING" );
		registration.registerKeyword( "COUNT" );
		registration.registerKeyword( "CREATE" );
		registration.registerKeyword( "CURRENT" );
		registration.registerKeyword( "CURRENT_DATE" );
		registration.registerKeyword( "CURRENT_TIME" );
		registration.registerKeyword( "CURRENT_TIMESTAMP" );
		registration.registerKeyword( "CURRENT_USER" );
		registration.registerKeyword( "CURSOR" );
		registration.registerKeyword( "DEALLOCATE" );
		registration.registerKeyword( "DEC" );
		registration.registerKeyword( "DECIMAL" );
		registration.registerKeyword( "DECLARE" );
		registration.registerKeyword( "DEFERRABLE" );
		registration.registerKeyword( "DEFERRED" );
		registration.registerKeyword( "DELETE" );
		registration.registerKeyword( "DESC" );
		registration.registerKeyword( "DESCRIBE" );
		registration.registerKeyword( "DIAGNOSTICS" );
		registration.registerKeyword( "DISCONNECT" );
		registration.registerKeyword( "DISTINCT" );
		registration.registerKeyword( "DOUBLE" );
		registration.registerKeyword( "DROP" );
		registration.registerKeyword( "ELSE" );
		registration.registerKeyword( "END" );
		registration.registerKeyword( "ENDEXEC" );
		registration.registerKeyword( "ESCAPE" );
		registration.registerKeyword( "EXCEPT" );
		registration.registerKeyword( "EXCEPTION" );
		registration.registerKeyword( "EXEC" );
		registration.registerKeyword( "EXECUTE" );
		registration.registerKeyword( "EXISTS" );
		registration.registerKeyword( "EXPLAIN" );
		registration.registerKeyword( "EXTERNAL" );
		registration.registerKeyword( "FALSE" );
		registration.registerKeyword( "FETCH" );
		registration.registerKeyword( "FIRST" );
		registration.registerKeyword( "FLOAT" );
		registration.registerKeyword( "FOR" );
		registration.registerKeyword( "FOREIGN" );
		registration.registerKeyword( "FOUND" );
		registration.registerKeyword( "FROM" );
		registration.registerKeyword( "FULL" );
		registration.registerKeyword( "FUNCTION" );
		registration.registerKeyword( "GET" );
		registration.registerKeyword( "GET_CURRENT_CONNECTION" );
		registration.registerKeyword( "GLOBAL" );
		registration.registerKeyword( "GO" );
		registration.registerKeyword( "GOTO" );
		registration.registerKeyword( "GRANT" );
		registration.registerKeyword( "GROUP" );
		registration.registerKeyword( "HAVING" );
		registration.registerKeyword( "HOUR" );
		registration.registerKeyword( "IDENTITY" );
		registration.registerKeyword( "IMMEDIATE" );
		registration.registerKeyword( "IN" );
		registration.registerKeyword( "INDICATOR" );
		registration.registerKeyword( "INITIALLY" );
		registration.registerKeyword( "INNER" );
		registration.registerKeyword( "INOUT" );
		registration.registerKeyword( "INPUT" );
		registration.registerKeyword( "INSENSITIVE" );
		registration.registerKeyword( "INSERT" );
		registration.registerKeyword( "INT" );
		registration.registerKeyword( "INTEGER" );
		registration.registerKeyword( "INTERSECT" );
		registration.registerKeyword( "INTO" );
		registration.registerKeyword( "IS" );
		registration.registerKeyword( "ISOLATION" );
		registration.registerKeyword( "JOIN" );
		registration.registerKeyword( "KEY" );
		registration.registerKeyword( "LAST" );
		registration.registerKeyword( "LEFT" );
		registration.registerKeyword( "LIKE" );
		registration.registerKeyword( "LONGINT" );
		registration.registerKeyword( "LOWER" );
		registration.registerKeyword( "LTRIM" );
		registration.registerKeyword( "MATCH" );
		registration.registerKeyword( "MAX" );
		registration.registerKeyword( "MIN" );
		registration.registerKeyword( "MINUTE" );
		registration.registerKeyword( "NATIONAL" );
		registration.registerKeyword( "NATURAL" );
		registration.registerKeyword( "NCHAR" );
		registration.registerKeyword( "NVARCHAR" );
		registration.registerKeyword( "NEXT" );
		registration.registerKeyword( "NO" );
		registration.registerKeyword( "NOT" );
		registration.registerKeyword( "NULL" );
		registration.registerKeyword( "NULLIF" );
		registration.registerKeyword( "NUMERIC" );
		registration.registerKeyword( "OF" );
		registration.registerKeyword( "ON" );
		registration.registerKeyword( "ONLY" );
		registration.registerKeyword( "OPEN" );
		registration.registerKeyword( "OPTION" );
		registration.registerKeyword( "OR" );
		registration.registerKeyword( "ORDER" );
		registration.registerKeyword( "OUT" );
		registration.registerKeyword( "OUTER" );
		registration.registerKeyword( "OUTPUT" );
		registration.registerKeyword( "OVERLAPS" );
		registration.registerKeyword( "PAD" );
		registration.registerKeyword( "PARTIAL" );
		registration.registerKeyword( "PREPARE" );
		registration.registerKeyword( "PRESERVE" );
		registration.registerKeyword( "PRIMARY" );
		registration.registerKeyword( "PRIOR" );
		registration.registerKeyword( "PRIVILEGES" );
		registration.registerKeyword( "PROCEDURE" );
		registration.registerKeyword( "PUBLIC" );
		registration.registerKeyword( "READ" );
		registration.registerKeyword( "REAL" );
		registration.registerKeyword( "REFERENCES" );
		registration.registerKeyword( "RELATIVE" );
		registration.registerKeyword( "RESTRICT" );
		registration.registerKeyword( "REVOKE" );
		registration.registerKeyword( "RIGHT" );
		registration.registerKeyword( "ROLLBACK" );
		registration.registerKeyword( "ROWS" );
		registration.registerKeyword( "RTRIM" );
		registration.registerKeyword( "SCHEMA" );
		registration.registerKeyword( "SCROLL" );
		registration.registerKeyword( "SECOND" );
		registration.registerKeyword( "SELECT" );
		registration.registerKeyword( "SESSION_USER" );
		registration.registerKeyword( "SET" );
		registration.registerKeyword( "SMALLINT" );
		registration.registerKeyword( "SOME" );
		registration.registerKeyword( "SPACE" );
		registration.registerKeyword( "SQL" );
		registration.registerKeyword( "SQLCODE" );
		registration.registerKeyword( "SQLERROR" );
		registration.registerKeyword( "SQLSTATE" );
		registration.registerKeyword( "SUBSTR" );
		registration.registerKeyword( "SUBSTRING" );
		registration.registerKeyword( "SUM" );
		registration.registerKeyword( "SYSTEM_USER" );
		registration.registerKeyword( "TABLE" );
		registration.registerKeyword( "TEMPORARY" );
		registration.registerKeyword( "TIMEZONE_HOUR" );
		registration.registerKeyword( "TIMEZONE_MINUTE" );
		registration.registerKeyword( "TO" );
		registration.registerKeyword( "TRAILING" );
		registration.registerKeyword( "TRANSACTION" );
		registration.registerKeyword( "TRANSLATE" );
		registration.registerKeyword( "TRANSLATION" );
		registration.registerKeyword( "TRUE" );
		registration.registerKeyword( "UNION" );
		registration.registerKeyword( "UNIQUE" );
		registration.registerKeyword( "UNKNOWN" );
		registration.registerKeyword( "UPDATE" );
		registration.registerKeyword( "UPPER" );
		registration.registerKeyword( "USER" );
		registration.registerKeyword( "USING" );
		registration.registerKeyword( "VALUES" );
		registration.registerKeyword( "VARCHAR" );
		registration.registerKeyword( "VARYING" );
		registration.registerKeyword( "VIEW" );
		registration.registerKeyword( "WHENEVER" );
		registration.registerKeyword( "WHERE" );
		registration.registerKeyword( "WITH" );
		registration.registerKeyword( "WORK" );
		registration.registerKeyword( "WRITE" );
		registration.registerKeyword( "XML" );
		registration.registerKeyword( "XMLEXISTS" );
		registration.registerKeyword( "XMLPARSE" );
		registration.registerKeyword( "XMLSERIALIZE" );
		registration.registerKeyword( "YEAR" );
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return DerbyLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		// It seems at least the row_number function is supported as of 10.4
		return getVersion().isBefore( 10, 4 )
				? WindowFunctionSupport.NONE
				: WindowFunctionSupport.builder()
						.features( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS )
						.build();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		builder.setAutoQuoteInitialUnderscore(true);
		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "(values 0)";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression + " dual" )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.NONE;
	}

}
