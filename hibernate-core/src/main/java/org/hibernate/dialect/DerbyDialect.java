/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.DerbyConcatFunction;
import org.hibernate.dialect.function.DerbyLpadEmulation;
import org.hibernate.dialect.function.DerbyRpadEmulation;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.DerbyLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.DerbySequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.CastType;
import org.hibernate.query.IntervalType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.idtable.IdTable;
import org.hibernate.query.sqm.mutation.internal.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.idtable.TempIdTableExporter;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDerbyDatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.DecimalJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcType;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

import jakarta.persistence.TemporalType;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * Hibernate Dialect for Apache Derby / Cloudscape 10
 *
 * @author Simon Johnston
 * @author Gavin King
 *
 */
public class DerbyDialect extends Dialect {

	// KNOWN LIMITATIONS:

	// no support for nationalized data (nchar, nvarchar, nclob)
	// * limited set of fields for extract()
	//   (no 'day of xxxx', nor 'week of xxxx')
	// * no support for format()
	// * pad() can only pad with blanks
	// * can't cast String to Binary
	// * can't select a parameter unless wrapped
	//   in a cast or function call

	private final int version;

	private final LimitHandler limitHandler;

	public DerbyDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
		registerKeywords( info );
	}

	public DerbyDialect() {
		this(1000);
	}

	public DerbyDialect(int version) {
		super();
		this.version = version;

		if ( getVersion() < 1070) {
			registerColumnType( Types.BOOLEAN, "smallint" ); //no boolean before 10.7
		}
		registerColumnType( Types.TINYINT, "smallint" ); //no tinyint
		registerColumnType( Types.CHAR, 254, "char($l)" );

		//HHH-12827: map them both to the same type to avoid problems with schema update
		//Note that 31 is the maximum precision Derby supports
//		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.NUMERIC, "decimal($p,$s)" );

		registerColumnType( Types.BINARY, 254, "char($l) for bit data" );
		registerColumnType( Types.BINARY, 32672, "varchar($l) for bit data" );
		registerColumnType( Types.BINARY, "long varchar for bit data" );
		registerColumnType( Types.VARBINARY, 32672, "varchar($l) for bit data" );
		registerColumnType( Types.VARBINARY, "long varchar for bit data" );

		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "clob($l)" );

		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );

		registerColumnType( Types.LONGVARCHAR, "long varchar" );

		registerDerbyKeywords();

		limitHandler = getVersion() < 1050
				? AbstractLimitHandler.NO_LIMIT
				: new DerbyLimitHandler( getVersion() >= 1060 );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	@Override
	public String getTypeName(int code, Size size) throws HibernateException {
		switch ( code ) {
			case Types.CHAR:
				// This is the maximum size for the CHAR datatype on Derby
				if ( size.getLength() > 254 ) {
					return "char(254)";
				}
		}
		return super.getTypeName( code, size );
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//this is the maximum allowed in Derby
		return 31;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return getVersion() < 1070
				? Types.SMALLINT
				: Types.BOOLEAN;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
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
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );

		// Derby needs an actual argument type for aggregates like SUM, AVG, MIN, MAX to determine the result type
		CommonFunctionFactory.aggregates(
				this,
				queryEngine,
				SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
				"||",
				getCastTypeName( stringType, null, null, null )
		);

		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.chr_char( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.trim1( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		CommonFunctionFactory.leftRight_substrLength( queryEngine );
		CommonFunctionFactory.characterLength_length( queryEngine, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		CommonFunctionFactory.power_expLn( queryEngine );

		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "round", "floor(?1*1e?2+0.5)/1e?2")
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().register( "concat", new DerbyConcatFunction( this, queryEngine.getTypeConfiguration() ) );

		//no way I can see to pad with anything other than spaces
		queryEngine.getSqmFunctionRegistry().register( "lpad", new DerbyLpadEmulation( queryEngine.getTypeConfiguration() ) );
		queryEngine.getSqmFunctionRegistry().register( "rpad", new DerbyRpadEmulation( queryEngine.getTypeConfiguration() ) );
		queryEngine.getSqmFunctionRegistry().register( "least", new CaseLeastGreatestEmulation( true ) );
		queryEngine.getSqmFunctionRegistry().register( "greatest", new CaseLeastGreatestEmulation( false ) );
		queryEngine.getSqmFunctionRegistry().register( "overlay", new InsertSubstringOverlayEmulation( queryEngine.getTypeConfiguration(), true ) );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DerbySqlAstTranslator<>( sessionFactory, statement );
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
				throw new NotYetImplementedFor6Exception("field type not supported on Derby: " + unit);
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
						return "cast(trim(cast(cast(?1 as decimal) as char(254))) as ?2)";
					case INTEGER:
					case FIXED:
						return "cast(trim(cast(?1 as char(254))) as ?2)";
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
		if ( getVersion() < 1070 ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion() < 1060
				? super.getSequenceSupport()
				: DerbySequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return getVersion() < 1060
				? null
				: "select sys.sysschemas.schemaname as sequence_schema,sys.syssequences.* from sys.syssequences left join sys.sysschemas on sys.syssequences.schemaid=sys.sysschemas.schemaid";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion() < 1060
				? SequenceInformationExtractorNoOpImpl.INSTANCE
				: SequenceInformationExtractorDerbyDatabaseImpl.INSTANCE;
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema " + schemaName + " restrict"};
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
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
		return new DB2IdentityColumnSupport();
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		//TODO: check this
		return true;
	}

	@Override
	public boolean supportsParametersInInsertSelect() {
		//TODO: check this
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		//checked on Derby 10.14
		return false;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		return false;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeDescriptorRegistry();
		if ( getVersion() < 1070 ) {
			jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, SmallIntJdbcType.INSTANCE );
		}
		jdbcTypeRegistry.addDescriptor( Types.NUMERIC, DecimalJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.TIMESTAMP_WITH_TIMEZONE, TimestampJdbcType.INSTANCE );

		// Derby requires a custom binder for binding untyped nulls that resolves the type through the statement
		typeContributions.contributeJdbcTypeDescriptor( ObjectNullResolvingJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullResolvingJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeDescriptorRegistry()
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
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
//				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

			if ( "40XL1".equals( sqlState ) || "40XL2".equals( sqlState ) ) {
				throw new LockTimeoutException( message, sqlException, sql );
			}
			return null;
		};
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		throw new NotYetImplementedFor6Exception("format() function not supported on Derby");
	}

	private void registerDerbyKeywords() {
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
	 * {@link DB2Dialect} returns a {@link org.hibernate.query.sqm.mutation.internal.idtable.GlobalTemporaryTableStrategy} that
	 * will make temporary tables created at startup and hence unavailable for subsequent connections.<br/>
	 * see HHH-10238.
	 */
	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableStrategy(
				new IdTable( rootEntityDescriptor, basename -> "session.HT_" + basename, this ),
				() -> new TempIdTableExporter( true, this::getTypeName ) {
					@Override
					protected String getCreateCommand() {
						return "declare global temporary table";
					}

					@Override
					protected String getCreateOptions() {
						return "not logged";
					}
				},
				AfterUseAction.CLEAN,
				TempTableDdlTransactionHandling.NONE,
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public boolean supportsPartitionBy() {
		return false;
	}
}
