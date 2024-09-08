/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.ChrLiteralEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.community.dialect.function.DerbyLpadEmulation;
import org.hibernate.community.dialect.function.DerbyRpadEmulation;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.community.dialect.pagination.DerbyLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.community.dialect.sequence.DerbySequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorDerbyDatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcType;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

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
public class DerbyLegacyDialect extends Dialect {

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
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				return getVersion().isBefore( 10, 7 ) ? "smallint" : super.columnType( sqlTypeCode );
			case TINYINT:
				//no tinyint
				return "smallint";

			case NUMERIC:
				// HHH-12827: map them both to the same type to avoid problems with schema update
				// Note that 31 is the maximum precision Derby supports
				return columnType( DECIMAL );

			case VARBINARY:
				return "varchar($l) for bit data";

			case NCHAR:
				return columnType( CHAR );
			case NVARCHAR:
				return columnType( VARCHAR );

			case BLOB:
				return "blob";
			case CLOB:
			case NCLOB:
				return "clob";

			case TIME:
			case TIME_WITH_TIMEZONE:
				return "time";

			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp";

			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		int varcharDdlTypeCapacity = 32_672;

		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								VARBINARY,
								isLob( LONG32VARBINARY )
										? CapacityDependentDdlType.LobKind.BIGGEST_LOB
										: CapacityDependentDdlType.LobKind.NONE,
								columnType( LONG32VARBINARY ),
								columnType( VARBINARY ),
								this
						)
						.withTypeCapacity( varcharDdlTypeCapacity, columnType( VARBINARY ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								VARCHAR,
								isLob( LONG32VARCHAR )
										? CapacityDependentDdlType.LobKind.BIGGEST_LOB
										: CapacityDependentDdlType.LobKind.NONE,
								columnType( LONG32VARCHAR ),
								columnType( VARCHAR ),
								this
						)
						.withTypeCapacity( varcharDdlTypeCapacity, columnType( VARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								NVARCHAR,
								isLob( LONG32NVARCHAR )
										? CapacityDependentDdlType.LobKind.BIGGEST_LOB
										: CapacityDependentDdlType.LobKind.NONE,
								columnType( LONG32VARCHAR ),
								columnType( NVARCHAR ),
								this
						)
						.withTypeCapacity( varcharDdlTypeCapacity, columnType( NVARCHAR ) )
						.build()
		);

		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								BINARY,
								isLob( LONG32VARBINARY )
										? CapacityDependentDdlType.LobKind.BIGGEST_LOB
										: CapacityDependentDdlType.LobKind.NONE,
								columnType( LONG32VARBINARY ),
								columnType( VARBINARY ),
								this
						)
						.withTypeCapacity( 254, "char($l) for bit data" )
						.withTypeCapacity( varcharDdlTypeCapacity, columnType( VARBINARY ) )
						.build()
		);

		// This is the maximum size for the CHAR datatype on Derby
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								CHAR,
								isLob( LONG32VARCHAR )
										? CapacityDependentDdlType.LobKind.BIGGEST_LOB
										: CapacityDependentDdlType.LobKind.NONE,
								columnType( LONG32VARCHAR ),
								columnType( CHAR ),
								this
						)
						.withTypeCapacity( 254, columnType( CHAR ) )
						.withTypeCapacity( getMaxVarcharLength(), columnType( VARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								NCHAR,
								isLob( LONG32NVARCHAR )
										? CapacityDependentDdlType.LobKind.BIGGEST_LOB
										: CapacityDependentDdlType.LobKind.NONE,
								columnType( LONG32NVARCHAR ),
								columnType( NCHAR ),
								this
						)
						.withTypeCapacity( 254, columnType( NCHAR ) )
						.withTypeCapacity( getMaxVarcharLength(), columnType( NVARCHAR ) )
						.build()
		);
	}

	@Override
	public int getMaxVarcharLength() {
		return 32_672;
	}

	@Override
	public int getMaxVarcharCapacity() {
		return 32_700;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//this is the maximum allowed in Derby
		return 31;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return getVersion().isBefore( 10, 7 )
				? Types.SMALLINT
				: Types.BOOLEAN;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	@Override
	public int getFloatPrecision() {
		return 23;
	}

	@Override
	public int getDoublePrecision() {
		return 52;
	}

	@Override
	public int getDefaultTimestampPrecision() {
		return 9;
	}

	@Override
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
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DerbyLegacySqlAstTranslator<>( sessionFactory, statement );
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
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case DAY_OF_MONTH:
				return "day(?2)";
			case DAY_OF_YEAR:
				return "({fn timestampdiff(sql_tsi_day,date(char(year(?2),4)||'-01-01'),?2)}+1)";
			case DAY_OF_WEEK:
				// Use the approach as outlined here: https://stackoverflow.com/questions/36357013/day-of-week-from-seconds-since-epoch
				return "(mod(mod({fn timestampdiff(sql_tsi_day,{d '1970-01-01'},?2)}+4,7)+7,7)+1)";
			case WEEK:
				// Use the approach as outlined here: https://www.sqlservercentral.com/articles/a-simple-formula-to-calculate-the-iso-week-number
				// In SQL Server terms this is (DATEPART(dy,DATEADD(dd,DATEDIFF(dd,'17530101',@SomeDate)/7*7,'17530104'))+6)/7
				return "(({fn timestampdiff(sql_tsi_day,date(char(year(?2),4)||'-01-01'),{fn timestampadd(sql_tsi_day,{fn timestampdiff(sql_tsi_day,{d '1753-01-01'},?2)}/7*7,{d '1753-01-04'})})}+7)/7)";
			case QUARTER:
				return "((month(?2)+2)/3)";
			case EPOCH:
				return "{fn timestampdiff(sql_tsi_second,{ts '1970-01-01 00:00:00'},?2)}";
			default:
				return "?1(?2)";
		}
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch (unit) {
			case WEEK:
			case DAY_OF_YEAR:
			case DAY_OF_WEEK:
				throw new UnsupportedOperationException("field type not supported on Derby: " + unit);
			case DAY_OF_MONTH:
				return "day";
			default:
				return super.translateExtractField(unit);
		}
	}

	/**
	 * Derby does have a real {@link Types#BOOLEAN}
	 * type, but it doesn't know how to cast to it. Worse,
	 * Derby makes us use the {@code double()} function to
	 * cast things to its floating point types.
	 */
	@Override
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
						return "cast(trim(cast(cast(?1 as decimal(" + getDefaultDecimalPrecision() + "," + BigDecimalJavaType.INSTANCE.getDefaultSqlScale( this, null ) + ")) as char(254))) as ?2)";
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
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				return "{fn timestampdiff(sql_tsi_frac_second,?2,?3)}";
			default:
				return "{fn timestampdiff(sql_tsi_?1,?2,?3)}";
		}
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		if ( getVersion().isBefore( 10, 7 ) ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion().isBefore( 10, 6 )
				? super.getSequenceSupport()
				: DerbySequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return getVersion().isBefore( 10, 6 )
				? null
				: "select sys.sysschemas.schemaname as sequence_schema,sys.syssequences.* from sys.syssequences left join sys.sysschemas on sys.syssequences.schemaid=sys.sysschemas.schemaid";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion().isBefore( 10, 6 )
				? SequenceInformationExtractorNoOpImpl.INSTANCE
				: SequenceInformationExtractorDerbyDatabaseImpl.INSTANCE;
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema " + schemaName + " restrict"};
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		return DB2Dialect.selectNullString( sqlType );
	}

	@Override
	public boolean supportsCommentOn() {
		//HHH-4531
		return false;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.NONE;
	}

	@Override
	public RowLockStrategy getReadRowLockStrategy() {
		return RowLockStrategy.NONE;
	}

	@Override
	public String getForUpdateString() {
		return " for update with rs";
	}

	@Override
	public String getWriteLockString(int timeout) {
		return " for update with rs";
	}

	@Override
	public String getReadLockString(int timeout) {
		return " for read only with rs";
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		//TODO: check this!
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		//TODO: check this!
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		// To enable the lock timeout, we need a dedicated call
		// 'call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.locks.waitTimeout', '3')'
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "values current timestamp";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
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
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		//TODO: check this
		return true;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		//checked on Derby 10.14
		return false;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		// As of version 10.5 Derby supports OFFSET and FETCH as well as ORDER BY in subqueries
		return getVersion().isSameOrAfter( 10, 5 );
	}

	@Override
	public boolean requiresCastForConcatenatingNonStrings() {
		return true;
	}

	@Override
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
								.getDescriptor( Object.class )
				)
		);
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
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
					case "40XL1":
					case "40XL2":
						return new LockTimeoutException( message, sqlException, sql );
				}
			}
			return null;
		};
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		throw new UnsupportedOperationException("format() function not supported on Derby");
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		registerKeyword( "ADD" );
		registerKeyword( "ALL" );
		registerKeyword( "ALLOCATE" );
		registerKeyword( "ALTER" );
		registerKeyword( "AND" );
		registerKeyword( "ANY" );
		registerKeyword( "ARE" );
		registerKeyword( "AS" );
		registerKeyword( "ASC" );
		registerKeyword( "ASSERTION" );
		registerKeyword( "AT" );
		registerKeyword( "AUTHORIZATION" );
		registerKeyword( "AVG" );
		registerKeyword( "BEGIN" );
		registerKeyword( "BETWEEN" );
		registerKeyword( "BIT" );
		registerKeyword( "BOOLEAN" );
		registerKeyword( "BOTH" );
		registerKeyword( "BY" );
		registerKeyword( "CALL" );
		registerKeyword( "CASCADE" );
		registerKeyword( "CASCADED" );
		registerKeyword( "CASE" );
		registerKeyword( "CAST" );
		registerKeyword( "CHAR" );
		registerKeyword( "CHARACTER" );
		registerKeyword( "CHECK" );
		registerKeyword( "CLOSE" );
		registerKeyword( "COLLATE" );
		registerKeyword( "COLLATION" );
		registerKeyword( "COLUMN" );
		registerKeyword( "COMMIT" );
		registerKeyword( "CONNECT" );
		registerKeyword( "CONNECTION" );
		registerKeyword( "CONSTRAINT" );
		registerKeyword( "CONSTRAINTS" );
		registerKeyword( "CONTINUE" );
		registerKeyword( "CONVERT" );
		registerKeyword( "CORRESPONDING" );
		registerKeyword( "COUNT" );
		registerKeyword( "CREATE" );
		registerKeyword( "CURRENT" );
		registerKeyword( "CURRENT_DATE" );
		registerKeyword( "CURRENT_TIME" );
		registerKeyword( "CURRENT_TIMESTAMP" );
		registerKeyword( "CURRENT_USER" );
		registerKeyword( "CURSOR" );
		registerKeyword( "DEALLOCATE" );
		registerKeyword( "DEC" );
		registerKeyword( "DECIMAL" );
		registerKeyword( "DECLARE" );
		registerKeyword( "DEFERRABLE" );
		registerKeyword( "DEFERRED" );
		registerKeyword( "DELETE" );
		registerKeyword( "DESC" );
		registerKeyword( "DESCRIBE" );
		registerKeyword( "DIAGNOSTICS" );
		registerKeyword( "DISCONNECT" );
		registerKeyword( "DISTINCT" );
		registerKeyword( "DOUBLE" );
		registerKeyword( "DROP" );
		registerKeyword( "ELSE" );
		registerKeyword( "END" );
		registerKeyword( "ENDEXEC" );
		registerKeyword( "ESCAPE" );
		registerKeyword( "EXCEPT" );
		registerKeyword( "EXCEPTION" );
		registerKeyword( "EXEC" );
		registerKeyword( "EXECUTE" );
		registerKeyword( "EXISTS" );
		registerKeyword( "EXPLAIN" );
		registerKeyword( "EXTERNAL" );
		registerKeyword( "FALSE" );
		registerKeyword( "FETCH" );
		registerKeyword( "FIRST" );
		registerKeyword( "FLOAT" );
		registerKeyword( "FOR" );
		registerKeyword( "FOREIGN" );
		registerKeyword( "FOUND" );
		registerKeyword( "FROM" );
		registerKeyword( "FULL" );
		registerKeyword( "FUNCTION" );
		registerKeyword( "GET" );
		registerKeyword( "GET_CURRENT_CONNECTION" );
		registerKeyword( "GLOBAL" );
		registerKeyword( "GO" );
		registerKeyword( "GOTO" );
		registerKeyword( "GRANT" );
		registerKeyword( "GROUP" );
		registerKeyword( "HAVING" );
		registerKeyword( "HOUR" );
		registerKeyword( "IDENTITY" );
		registerKeyword( "IMMEDIATE" );
		registerKeyword( "IN" );
		registerKeyword( "INDICATOR" );
		registerKeyword( "INITIALLY" );
		registerKeyword( "INNER" );
		registerKeyword( "INOUT" );
		registerKeyword( "INPUT" );
		registerKeyword( "INSENSITIVE" );
		registerKeyword( "INSERT" );
		registerKeyword( "INT" );
		registerKeyword( "INTEGER" );
		registerKeyword( "INTERSECT" );
		registerKeyword( "INTO" );
		registerKeyword( "IS" );
		registerKeyword( "ISOLATION" );
		registerKeyword( "JOIN" );
		registerKeyword( "KEY" );
		registerKeyword( "LAST" );
		registerKeyword( "LEFT" );
		registerKeyword( "LIKE" );
		registerKeyword( "LONGINT" );
		registerKeyword( "LOWER" );
		registerKeyword( "LTRIM" );
		registerKeyword( "MATCH" );
		registerKeyword( "MAX" );
		registerKeyword( "MIN" );
		registerKeyword( "MINUTE" );
		registerKeyword( "NATIONAL" );
		registerKeyword( "NATURAL" );
		registerKeyword( "NCHAR" );
		registerKeyword( "NVARCHAR" );
		registerKeyword( "NEXT" );
		registerKeyword( "NO" );
		registerKeyword( "NOT" );
		registerKeyword( "NULL" );
		registerKeyword( "NULLIF" );
		registerKeyword( "NUMERIC" );
		registerKeyword( "OF" );
		registerKeyword( "ON" );
		registerKeyword( "ONLY" );
		registerKeyword( "OPEN" );
		registerKeyword( "OPTION" );
		registerKeyword( "OR" );
		registerKeyword( "ORDER" );
		registerKeyword( "OUT" );
		registerKeyword( "OUTER" );
		registerKeyword( "OUTPUT" );
		registerKeyword( "OVERLAPS" );
		registerKeyword( "PAD" );
		registerKeyword( "PARTIAL" );
		registerKeyword( "PREPARE" );
		registerKeyword( "PRESERVE" );
		registerKeyword( "PRIMARY" );
		registerKeyword( "PRIOR" );
		registerKeyword( "PRIVILEGES" );
		registerKeyword( "PROCEDURE" );
		registerKeyword( "PUBLIC" );
		registerKeyword( "READ" );
		registerKeyword( "REAL" );
		registerKeyword( "REFERENCES" );
		registerKeyword( "RELATIVE" );
		registerKeyword( "RESTRICT" );
		registerKeyword( "REVOKE" );
		registerKeyword( "RIGHT" );
		registerKeyword( "ROLLBACK" );
		registerKeyword( "ROWS" );
		registerKeyword( "RTRIM" );
		registerKeyword( "SCHEMA" );
		registerKeyword( "SCROLL" );
		registerKeyword( "SECOND" );
		registerKeyword( "SELECT" );
		registerKeyword( "SESSION_USER" );
		registerKeyword( "SET" );
		registerKeyword( "SMALLINT" );
		registerKeyword( "SOME" );
		registerKeyword( "SPACE" );
		registerKeyword( "SQL" );
		registerKeyword( "SQLCODE" );
		registerKeyword( "SQLERROR" );
		registerKeyword( "SQLSTATE" );
		registerKeyword( "SUBSTR" );
		registerKeyword( "SUBSTRING" );
		registerKeyword( "SUM" );
		registerKeyword( "SYSTEM_USER" );
		registerKeyword( "TABLE" );
		registerKeyword( "TEMPORARY" );
		registerKeyword( "TIMEZONE_HOUR" );
		registerKeyword( "TIMEZONE_MINUTE" );
		registerKeyword( "TO" );
		registerKeyword( "TRAILING" );
		registerKeyword( "TRANSACTION" );
		registerKeyword( "TRANSLATE" );
		registerKeyword( "TRANSLATION" );
		registerKeyword( "TRUE" );
		registerKeyword( "UNION" );
		registerKeyword( "UNIQUE" );
		registerKeyword( "UNKNOWN" );
		registerKeyword( "UPDATE" );
		registerKeyword( "UPPER" );
		registerKeyword( "USER" );
		registerKeyword( "USING" );
		registerKeyword( "VALUES" );
		registerKeyword( "VARCHAR" );
		registerKeyword( "VARYING" );
		registerKeyword( "VIEW" );
		registerKeyword( "WHENEVER" );
		registerKeyword( "WHERE" );
		registerKeyword( "WITH" );
		registerKeyword( "WORK" );
		registerKeyword( "WRITE" );
		registerKeyword( "XML" );
		registerKeyword( "XMLEXISTS" );
		registerKeyword( "XMLPARSE" );
		registerKeyword( "XMLSERIALIZE" );
		registerKeyword( "YEAR" );
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * From Derby docs:
	 * <pre>
	 *     The DECLARE GLOBAL TEMPORARY TABLE statement defines a temporary table for the current connection.
	 * </pre>
	 *
	 * {@link DB2Dialect} returns a {@link GlobalTemporaryTableMutationStrategy} that
	 * will make temporary tables created at startup and hence unavailable for subsequent connections.<br/>
	 * see HHH-10238.
	 */
	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableMutationStrategy(
				TemporaryTable.createIdTable(
						rootEntityDescriptor,
						basename -> "session." + TemporaryTable.ID_TABLE_PREFIX + basename,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableInsertStrategy(
				TemporaryTable.createEntityTable(
						rootEntityDescriptor,
						name -> "session." + TemporaryTable.ENTITY_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.LOCAL;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "not logged";
	}

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return false;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "declare global temporary table";
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}

	@Override
	public boolean supportsPartitionBy() {
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		// It seems at least the row_number function is supported as of 10.4
		return getVersion().isSameOrAfter( 10, 4 );
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		builder.setAutoQuoteInitialUnderscore(true);
		return super.buildIdentifierHelper(builder, dbMetaData);
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}
}
