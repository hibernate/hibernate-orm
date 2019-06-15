/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.JDBCException;
import org.hibernate.LockOptions;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.NvlCoalesceEmulation;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.Oracle12cIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.naming.Identifier;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ANSICaseFragment;
import org.hibernate.sql.ANSIJoinFragment;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.OracleJoinFragment;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorOracleDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BlobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.query.TemporalUnit.DAY;
import static org.hibernate.query.TemporalUnit.HOUR;
import static org.hibernate.query.TemporalUnit.MINUTE;
import static org.hibernate.query.TemporalUnit.MONTH;
import static org.hibernate.query.TemporalUnit.NANOSECOND;
import static org.hibernate.query.TemporalUnit.SECOND;
import static org.hibernate.query.TemporalUnit.YEAR;

/**
 * A dialect for Oracle 8i and above.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class OracleDialect extends Dialect {

	private final int version;

	int getVersion() {
		return version;
	}

	public OracleDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() );
	}

	private static final Pattern DISTINCT_KEYWORD_PATTERN = Pattern.compile( "\\bdistinct\\b" );

	private static final Pattern GROUP_BY_KEYWORD_PATTERN = Pattern.compile( "\\bgroup\\sby\\b" );

	private static final Pattern ORDER_BY_KEYWORD_PATTERN = Pattern.compile( "\\border\\sby\\b" );

	private static final Pattern UNION_KEYWORD_PATTERN = Pattern.compile( "\\bunion\\b" );

	private static final Pattern SQL_STATEMENT_TYPE_PATTERN = Pattern.compile("^(?:/\\*.*?\\*/)?\\s*(select|insert|update|delete)\\s+.*?");

	private static final int PARAM_LIST_SIZE_LIMIT = 1000;

	public static final String PREFER_LONG_RAW = "hibernate.dialect.oracle.prefer_long_raw";

	//TODO: 12c supports much simpler OFFSET 2 ROWS FETCH NEXT 1 ROWS ONLY
	private final LimitHandler limitHandler;

	public OracleDialect() {
		this(8);
	}

	public OracleDialect(int version) {
		super();
		this.version = version;

		registerCharacterTypeMappings();
		registerNumericTypeMappings();
		registerDateTimeTypeMappings();
		registerBinaryTypeMappings();
		registerReverseHibernateTypeMappings();
		registerDefaultProperties();

		limitHandler = getVersion() < 12
				? new LegacyOracleLimitHandler( getVersion() )
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.initcap( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		CommonFunctionFactory.leftRight_substr( queryEngine );
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.bitand( queryEngine );
		CommonFunctionFactory.lastDay( queryEngine );
		CommonFunctionFactory.toCharNumberDateTimestamp( queryEngine );
		CommonFunctionFactory.ceiling_ceil( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.rownumRowid( queryEngine );
		CommonFunctionFactory.sysdate( queryEngine );
		CommonFunctionFactory.systimestamp( queryEngine );
		CommonFunctionFactory.characterLength_length( queryEngine );
		CommonFunctionFactory.addMonths( queryEngine );
		CommonFunctionFactory.monthsBetween( queryEngine );

		CommonFunctionFactory.median( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );

		if ( getVersion() < 9 ) {
			queryEngine.getSqmFunctionRegistry().register( "coalesce", new NvlCoalesceEmulation() );
		}
		else {
			//Oracle has had coalesce() since 9.0.1
			CommonFunctionFactory.coalesce( queryEngine );
		}

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				StandardSpiBasicTypes.INTEGER,
				"instr(?2, ?1)",
				"instr(?2, ?1, ?3)"
		).setArgumentListSignature("(pattern, string[, start])");
	}

	@Override
	public String currentDate() {
		return getVersion() < 9 ? currentTimestamp() : "current_date";
	}

	@Override
	public String currentTime() {
		return currentTimestamp();
	}

	@Override
	public String currentTimestamp() {
		return getVersion() < 9 ? "sysdate" : currentTimestampWithTimeZone();
	}

	@Override
	public String currentLocalTime() {
		return currentLocalTimestamp();
	}

	@Override
	public String currentLocalTimestamp() {
		return getVersion() < 9 ? currentTimestamp() : "localtimestamp";
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return getVersion() < 9 ? currentTimestamp() : "current_timestamp";
	}

	/**
	 * Oracle supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using to_char() with a format string instead of extract().
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * and {@link TemporalUnit#WEEK}.
	 */
	@Override
	public String extract(TemporalUnit unit) {
		switch (unit) {
			case DAY_OF_WEEK:
				return "to_number(to_char(?2,'D'))";
			case DAY_OF_MONTH:
				return "to_number(to_char(?2,'DD'))";
			case DAY_OF_YEAR:
				return "to_number(to_char(?2,'DDD'))";
			case WEEK:
				return "to_number(to_char(?2,'IW'))"; //the ISO week number
			default:
				return super.extract(unit);
		}
	}

	@Override
	public String timestampadd(TemporalUnit unit, boolean timestamp) {
		StringBuilder pattern = new StringBuilder();
		pattern.append("(?3 + ");
		switch ( unit ) {
			case YEAR:
			case QUARTER:
			case MONTH:
				pattern.append("numtoyminterval");
				break;
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
			case SECOND:
			case NANOSECOND:
			case NATIVE:
				pattern.append("numtodsinterval");
				break;
			default:
				throw new SemanticException(unit + " is not a legal field");
		}
		pattern.append("(");
		switch ( unit ) {
			case QUARTER:
				pattern.append("3*(");
				break;
			case WEEK:
				pattern.append("7*(");
				break;
			case NANOSECOND:
			case NATIVE:
				pattern.append("1e-9*(");
				break;
		}
		pattern.append("?2");
		switch ( unit ) {
			case NATIVE:
			case NANOSECOND:
			case QUARTER:
			case WEEK:
				pattern.append(")");
				break;
		}
		pattern.append(",'");
		switch ( unit ) {
			case QUARTER:
				pattern.append("month");
				break;
			case WEEK:
				pattern.append("day");
				break;
			case NANOSECOND:
			case NATIVE:
				pattern.append("second");
				break;
			default:
				pattern.append("?1");
		}
		pattern.append("')");
		pattern.append(")");
		return pattern.toString();
	}

	@Override
	public String timestampdiff(
			TemporalUnit unit,
			boolean fromTimestamp, boolean toTimestamp) {
		StringBuilder pattern = new StringBuilder();
		boolean timestamp = toTimestamp || fromTimestamp;
		switch (unit) {
			case YEAR:
				extractField(pattern, YEAR, unit);
				break;
			case QUARTER:
			case MONTH:
				pattern.append("(");
				extractField(pattern, YEAR, unit);
				pattern.append("+");
				extractField(pattern, MONTH, unit);
				pattern.append(")");
				break;
			case WEEK:
			case DAY:
				extractField(pattern, DAY, unit);
				break;
			case HOUR:
				pattern.append("(");
				extractField(pattern, DAY, unit);
				if (timestamp) {
					pattern.append("+");
					extractField(pattern, HOUR, unit);
				}
				pattern.append(")");
				break;
			case MINUTE:
				pattern.append("(");
				extractField(pattern, DAY, unit);
				if (timestamp) {
					pattern.append("+");
					extractField(pattern, HOUR, unit);
					pattern.append("+");
					extractField(pattern, MINUTE, unit);
				}
				pattern.append(")");
				break;
			case NATIVE:
			case NANOSECOND:
			case SECOND:
				pattern.append("(");
				extractField(pattern, DAY, unit);
				if (timestamp) {
					pattern.append("+");
					extractField(pattern, HOUR, unit);
					pattern.append("+");
					extractField(pattern, MINUTE, unit);
					pattern.append("+");
					extractField(pattern, SECOND, unit);
				}
				pattern.append(")");
				break;
			default:
				throw new SemanticException("unrecognized field: " + unit);
		}
		return pattern.toString();
	}

	void extractField(
			StringBuilder pattern,
			TemporalUnit unit, TemporalUnit toUnit) {
		pattern.append("extract(");
		pattern.append( translateExtractField(unit) );
		pattern.append(" from (?3-?2) ");
		switch (unit) {
			case YEAR:
			case MONTH:
				pattern.append("year to month");
				break;
			case DAY:
			case HOUR:
			case MINUTE:
			case SECOND:
				pattern.append("day to second");
				break;
			default:
				throw new SemanticException(unit + " is not a legal field");
		}
		pattern.append(")");
		pattern.append( unit.conversionFactor( toUnit, this ) );
	}

	protected void registerCharacterTypeMappings() {
		if ( getVersion() < 9) {
			registerColumnType( Types.VARCHAR, 4000, "varchar2($l)" );
			registerColumnType( Types.VARCHAR, "long" );
		}
		else {
			registerColumnType( Types.CHAR, "char($l char)" );
			registerColumnType( Types.VARCHAR, 4000, "varchar2($l char)" );
			registerColumnType( Types.VARCHAR, "long" );
			registerColumnType( Types.NVARCHAR, "nvarchar2($l)" );
			registerColumnType( Types.LONGNVARCHAR, "nvarchar2($l)" );
		}
	}

	protected void registerNumericTypeMappings() {
		registerColumnType( Types.BIT, 1, "number(1,0)" );
		registerColumnType( Types.BIT, "number(3,0)" );
		registerColumnType( Types.BOOLEAN, "number(1,0)" );

		registerColumnType( Types.BIGINT, "number(19,0)" );
		registerColumnType( Types.SMALLINT, "number(5,0)" );
		registerColumnType( Types.TINYINT, "number(3,0)" );
		registerColumnType( Types.INTEGER, "number(10,0)" );

		registerColumnType( Types.NUMERIC, "number($p,$s)" );
		registerColumnType( Types.DECIMAL, "number($p,$s)" );
	}

	protected void registerDateTimeTypeMappings() {
		if ( getVersion() < 9 ) {
			registerColumnType( Types.DATE, "date" );
			registerColumnType( Types.TIME, "date" );
			registerColumnType( Types.TIMESTAMP, "date" );
			registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "date" );
		}
		else {
			registerColumnType( Types.DATE, "date" );
			registerColumnType( Types.TIME, "date" );
			registerColumnType( Types.TIMESTAMP, "timestamp($p)" );
			registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp($p) with time zone" );
		}
	}

	protected void registerBinaryTypeMappings() {
		registerColumnType( Types.BINARY, 2000, "raw($l)" );
		registerColumnType( Types.BINARY, "long raw" );

		registerColumnType( Types.VARBINARY, 2000, "raw($l)" );
		registerColumnType( Types.VARBINARY, "long raw" );

		registerColumnType( Types.LONGVARCHAR, "long" );
		registerColumnType( Types.LONGVARBINARY, "long raw" );
	}

	protected void registerReverseHibernateTypeMappings() {
	}

	protected void registerDefaultProperties() {
		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		if ( getVersion() < 12 ) {
			// Oracle driver reports to support getGeneratedKeys(), but they only
			// support the version taking an array of the names of the columns to
			// be returned (via its RETURNING clause).  No other driver seems to
			// support this overloaded version.
			getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "false" );
			getDefaultProperties().setProperty( Environment.BATCH_VERSIONED_DATA, "false" );
		}
		else {
			getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "true" );
			getDefaultProperties().setProperty( Environment.BATCH_VERSIONED_DATA, "true" );
		}
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.BOOLEAN ? BitSqlDescriptor.INSTANCE : super.getSqlTypeDescriptorOverride( sqlCode );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		if ( getVersion() >= 12 ) {
			// account for Oracle's deprecated support for LONGVARBINARY
			// prefer BLOB, unless the user explicitly opts out
			boolean preferLong = serviceRegistry.getService( ConfigurationService.class ).getSetting(
					PREFER_LONG_RAW,
					StandardConverters.BOOLEAN,
					false
			);

			BlobSqlDescriptor descriptor = preferLong ?
					BlobSqlDescriptor.PRIMITIVE_ARRAY_BINDING :
					BlobSqlDescriptor.DEFAULT;

			typeContributions.contributeSqlTypeDescriptor( descriptor );
		}
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return "sequence";
	}

	// features which change between 8i, 9i, and 10g ~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion() < 12
				? super.getIdentityColumnSupport()
				: new Oracle12cIdentityColumnSupport();
	}

	@Override
	@SuppressWarnings("deprecation")
	public JoinFragment createOuterJoinFragment() {
		return getVersion() < 10 ? new OracleJoinFragment() : new ANSIJoinFragment();
	}

	@Override
	public String getCrossJoinSeparator() {
		return getVersion() < 10 ? ", " : " cross join ";
	}

	/**
	 * Map case support to the Oracle DECODE function.  Oracle did not
	 * add support for CASE until 9i.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("deprecation")
	public CaseFragment createCaseFragment() {
		return getVersion() < 9
				? new DecodeCaseFragment()
				// Oracle did add support for ANSI CASE statements in 9i
				: new ANSICaseFragment();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
		if ( getVersion() >= 9  ) {
			return super.getSelectClauseNullString(sqlType);
		}
		else {
			switch(sqlType) {
				case Types.VARCHAR:
				case Types.CHAR:
					return "to_char(null)";
				case Types.DATE:
				case Types.TIME:
				case Types.TIMESTAMP:
				case Types.TIMESTAMP_WITH_TIMEZONE:
					return "to_date(null)";
				default:
					return "to_number(null)";
			}
		}
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return getVersion() < 9
				? "select sysdate from dual"
				: "select systimestamp from dual";
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getCurrentTimestampSQLFunctionName() {
		return getVersion() < 9
				? "sysdate"
				// the standard SQL function name is current_timestamp...
				: "current_timestamp";
	}


	// features which remain constant across 8i, 9i, and 10g ~~~~~~~~~~~~~~~~~~

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from dual";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
		String minOrMaxValue;
		if ( initialValue < 0 && incrementSize > 0 ) {
			minOrMaxValue = " minvalue " + initialValue;
		}
		else if ( initialValue > 0 && incrementSize < 0 ) {
			minOrMaxValue = " maxvalue " + initialValue;
		}
		else {
			minOrMaxValue = "";
		}
		return getCreateSequenceString( sequenceName )
				+ minOrMaxValue
				+ " start with " + initialValue
				+ " increment by " + incrementSize;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade constraints";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getForUpdateNowaitString() {
		return " for update nowait";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString() + " of " + aliases + " nowait";
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
	}

	@Override
	public String getFromDual() {
		return "from dual";
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from all_sequences";
	}

	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorOracleDatabaseImpl.INSTANCE;
	}

	@Override
	public String getSelectGUIDString() {
		return "select rawtohex(sys_guid()) from dual";
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {

		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
			switch ( errorCode ) {
				case 1:
				case 2291:
				case 2292:
					return extractUsingTemplate("(", ")", sqle.getMessage());
				case 1400:
					// simple nullability constraint
					return null;
				default:
					return null;
			}
		}

	};

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				// interpreting Oracle exceptions is much much more precise based on their specific vendor codes.
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				switch ( errorCode ) {

					// lock timeouts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

					case 30006:
						// ORA-30006: resource busy; acquire with WAIT timeout expired
						throw new LockTimeoutException(message, sqlException, sql);
					case 54:
						// ORA-00054: resource busy and acquire with NOWAIT specified or timeout expired
						throw new LockTimeoutException(message, sqlException, sql);
					case 4021:
						// ORA-04021 timeout occurred while waiting to lock object
						throw new LockTimeoutException(message, sqlException, sql);

					// deadlocks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

					case 60:
						// ORA-00060: deadlock detected while waiting for resource
						return new LockAcquisitionException( message, sqlException, sql );
					case 4020:
						// ORA-04020 deadlock detected while trying to lock object
						return new LockAcquisitionException( message, sqlException, sql );

					// query cancelled ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

					case 1013:
						// ORA-01013: user requested cancel of current operation
						throw new QueryTimeoutException(  message, sqlException, sql );

					// data integrity violation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

					case 1407:
						// ORA-01407: cannot update column to NULL
						final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName( sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );

					default:
						return null;
				}
			}
		};
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		//	register the type of the out param - an Oracle specific type
		statement.registerOutParameter( col, OracleTypesHelper.INSTANCE.getOracleCursorTypeSqlType() );
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				final Identifier name = super.determineIdTableName( baseName );
				return name.getText().length() > 30
						? new Identifier( name.getText().substring( 0, 30 ), false )
						: name;
			}
		};
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			protected String getCreateOptions() {
				return "on commit delete rows";
			}
		};
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public boolean forceLobAsLastValue() {
		return true;
	}

	@Override
	public boolean isEmptyStringTreatedAsNull() {
		return true;
	}

	/**
	 * For Oracle, the FOR UPDATE clause cannot be applied when using ORDER BY, DISTINCT or views.
	 *
	 * @see <a href="https://docs.oracle.com/database/121/SQLRF/statements_10002.htm#SQLRF01702">Oracle FOR UPDATE restrictions</a>
	 */
	@Override
	public boolean useFollowOnLocking(String sql, QueryOptions queryOptions) {
		if ( StringHelper.isEmpty( sql ) || queryOptions == null ) {
			return true;
		}

		sql = sql.toLowerCase( Locale.ROOT );

		return DISTINCT_KEYWORD_PATTERN.matcher( sql ).find()
				|| GROUP_BY_KEYWORD_PATTERN.matcher( sql ).find()
				|| UNION_KEYWORD_PATTERN.matcher( sql ).find()
				|| (
						queryOptions.hasLimit()
								&& (
										ORDER_BY_KEYWORD_PATTERN.matcher( sql ).find()
												|| queryOptions.getLimit().getFirstRow() != null
								)
				);
	}

	@Override
	public String getNotExpression( String expression ) {
		return "not (" + expression + ")";
	}

	@Override
	public String getQueryHintString(String sql, String hints) {
		String statementType = statementType(sql);

		final int pos = sql.indexOf( statementType );
		if ( pos > -1 ) {
			final StringBuilder buffer = new StringBuilder( sql.length() + hints.length() + 8 );
			if ( pos > 0 ) {
				buffer.append( sql.substring( 0, pos ) );
			}
			buffer
			.append( statementType )
			.append( " /*+ " )
			.append( hints )
			.append( " */" )
			.append( sql.substring( pos + statementType.length() ) );
			sql = buffer.toString();
		}

		return sql;
	}

	@Override
	public int getMaxAliasLength() {
		// Oracle's max identifier length is 30, but Hibernate needs to add "uniqueing info" so we account for that,
		return 20;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		// Oracle supports returning cursors
		return StandardCallableStatementSupport.REF_CURSOR_INSTANCE;
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL";
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	private String statementType(String sql) {
		Matcher matcher = SQL_STATEMENT_TYPE_PATTERN.matcher( sql );

		if ( matcher.matches() && matcher.groupCount() == 1 ) {
			return matcher.group(1);
		}

		throw new IllegalArgumentException( "Can't determine SQL statement type for statement: " + sql );
	}

	/**
	 * HHH-4907, I don't know if oracle 8 supports this syntax, so I'd think it is better add this
	 * method here. Reopen this issue if you found/know 8 supports it.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return getVersion() >= 9;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return " for update skip locked";
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString() + " of " + aliases + " skip locked";
	}

	@Override
	public boolean supportsSkipLocked() {
		return true;
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( getVersion() >= 10 && timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString();
		}
		else if ( getVersion() >= 9 && timeout == LockOptions.NO_WAIT ) {
			return " for update nowait";
		}
		else if ( getVersion() >= 9 && timeout > 0 ) {
			// convert from milliseconds to seconds
			final float seconds = timeout / 1e3f;
			return " for update wait " + Math.round( seconds );
		}
		else {
			return super.getWriteLockString( timeout );
		}
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( getVersion() >= 10 && timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString( aliases );
		}
		else {
			return super.getWriteLockString( aliases, timeout );
		}
	}

	@Override
	public String getReadLockString(int timeout) {
		return getWriteLockString( timeout );
	}

	public static Replacer datetimeFormat(String format, boolean useFm) {
		String fm = useFm ? "fm" : "";
		return new Replacer( format, "'", "\"" )
				//era
				.replace("GG", "AD")
				.replace("G", "AD")

				//year
				.replace("yyyy", "YYYY")
				.replace("yyy", fm + "YYYY")
				.replace("yy", "YY")
				.replace("y", fm + "YYYY")

				//month of year
				.replace("MMMM", fm + "Month")
				.replace("MMM", "Mon")
				.replace("MM", "MM")
				.replace("M", fm + "MM")

				//week of year
				.replace("ww", "IW")
				.replace("w", fm + "IW")
				//year for week
				.replace("YYYY", "IYYY")
				.replace("YYY", fm + "IYYY")
				.replace("YY", "IY")
				.replace("Y", fm + "IYYY")

				//week of month
				.replace("W", "W")

				//day of week
				.replace("EEEE", fm + "Day")
				.replace("EEE", "Dy")
				.replace("ee", "D")
				.replace("e", fm + "D")

				//day of month
				.replace("dd", "DD")
				.replace("d", fm + "DD")

				//day of year
				.replace("DDD", "DDD")
				.replace("DD", fm + "DDD")
				.replace("D", fm + "DDD")

				//am pm
				.replace("aa", "AM")
				.replace("a", "AM")

				//hour
				.replace("hh", "HH12")
				.replace("HH", "HH24")
				.replace("h", fm + "HH12")
				.replace("H", fm + "HH24")

				//minute
				.replace("mm", "MI")
				.replace("m", fm + "MI")

				//second
				.replace("ss", "SS")
				.replace("s", fm + "SS")

				//fractional seconds
				.replace("SSSSSS", "FF6")
				.replace("SSSSS", "FF5")
				.replace("SSSS", "FF4")
				.replace("SSS", "FF3")
				.replace("SS", "FF2")
				.replace("S", "FF1")

				//timezones
				.replace("zzz", "TZR")
				.replace("zz", "TZR")
				.replace("z", "TZR")
				.replace("ZZZ", "TZHTZM")
				.replace("ZZ", "TZHTZM")
				.replace("Z", "TZHTZM")
				.replace("xxx", "TZH:TZM")
				.replace("xx", "TZHTZM")
				.replace("x", "TZH"); //note special case
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		return (ResultSet) statement.getObject( position );
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
		statement.registerOutParameter( name, OracleTypesHelper.INSTANCE.getOracleCursorTypeSqlType() );
		return 1;
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		return (ResultSet) statement.getObject( name );
	}

}
