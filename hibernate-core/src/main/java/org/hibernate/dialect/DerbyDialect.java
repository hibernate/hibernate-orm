/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.DerbyConcatEmulation;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.DerbyLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.naming.Identifier;
import org.hibernate.query.CastType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DerbyCaseFragment;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDerbyDatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.sql.spi.DecimalSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Hibernate Dialect for Apache Derby / Cloudscape 10
 *
 * @author Simon Johnston
 * @author Gavin King
 *
 */
public class DerbyDialect extends Dialect {

	// KNOWN LIMITATIONS:

	// * limited set of fields for extract()
	//   (no 'day of xxxx', nor 'week of xxxx')
	// * no support for format()
	// * pad() can only pad with blanks
	// * can't cast String to Binary
	// * can't select a parameter unless wrapped
	//   in a cast or function call

	private final int version;

	int getVersion() {
		return version;
	}

	private final LimitHandler limitHandler;

	public DerbyDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	public DerbyDialect() {
		this(1000);
	}

	public DerbyDialect(int version) {
		super();
		this.version = version;

		registerColumnType( Types.BIT, 1, "boolean" ); //no bit
		registerColumnType( Types.BIT, "smallint" ); //no bit
		registerColumnType( Types.TINYINT, "smallint" ); //no tinyint

		//HHH-12827: map them both to the same type to
		//           avoid problems with schema update
//		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.NUMERIC, "decimal($p,$s)" );

		registerColumnType( Types.BINARY, "varchar($l) for bit data" );
		registerColumnType( Types.BINARY, 254, "char($l) for bit data" );
		registerColumnType( Types.VARBINARY, "varchar($l) for bit data" );
		registerColumnType( Types.LONGVARBINARY, "varchar($l) for bit data" );

		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "clob($l)" );

		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );

		registerDerbyKeywords();

		getDefaultProperties().setProperty( Environment.QUERY_LITERAL_RENDERING, "literal" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );

		limitHandler = getVersion() < 1050
				? AbstractLimitHandler.NO_LIMIT
				: new DerbyLimitHandler( getVersion() >= 1060 );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

	public int getDefaultDecimalPrecision() {
		//this is the maximum allowed in Derby
		return 31;
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
		CommonFunctionFactory.characterLength_length( queryEngine );
		CommonFunctionFactory.power_expLn( queryEngine );

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "round", "floor(?1*1e?2+0.5)/1e?2")
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();

		queryEngine.getSqmFunctionRegistry().register( "concat", new DerbyConcatEmulation() );

		//no way I can see to pad with anything other than spaces
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "lpad", "case when length(?1)<?2 then substr(char('',?2)||?1,length(?1)) else ?1 end" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, length)")
				.register();
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "rpad", "case when length(?1)<?2 then substr(?1||char('',?2),1,?2) else ?1 end" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, length)")
				.register();
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
	public String extract(TemporalUnit unit) {
		switch (unit) {
			case DAY_OF_MONTH:
				return "day(?2)";
			case DAY_OF_YEAR:
				return "({fn timestampdiff(sql_tsi_day, date(char(year(?2),4)||'-01-01'),?2)}+1)";
			case DAY_OF_WEEK:
				return "(mod({fn timestampdiff(sql_tsi_day, {d '2000-01-01'}, ?2)},7)+1)";
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
	 * Derby does have a real {@link java.sql.Types#BOOLEAN}
	 * type, but it doesn't know how to cast to it. Worse,
	 * Derby makes us use the {@code double()} function to
	 * cast things to its floating point types.
	 */
	@Override
	public String cast(CastType from, CastType to) {
		switch (to) {
			case FLOAT:
				return "cast(double(?1) as real)";
			case DOUBLE:
				return "double(?1)";
			case BOOLEAN:
				switch (from) {
					case STRING:
//						return "case when lower(?1)in('t','true') then true when lower(?1)in('f','false') then false end";
						return "case when ?1 in('t','true','T','TRUE') then true when ?1 in('f','false','F','FALSE') then false end";
					case LONG:
					case INTEGER:
						return "(?1<>0)";
				}
			default:
				return super.cast(from, to);
		}
	}

	@Override
	public String timestampadd(TemporalUnit unit, boolean timestamp) {
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				return "{fn timestampadd(sql_tsi_frac_second, mod(bigint(?2),1000000000), {fn timestampadd(sql_tsi_second, bigint((?2)/1000000000), ?3)})}";
			default:
				return "{fn timestampadd(sql_tsi_?1, bigint(?2), ?3)}";
		}
	}

	@Override
	public String timestampdiff(TemporalUnit unit, boolean fromTimestamp, boolean toTimestamp) {
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				return "{fn timestampdiff(sql_tsi_frac_second, ?2, ?3)}";
			default:
				return "{fn timestampdiff(sql_tsi_?1, ?2, ?3)}";
		}
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return getVersion() < 1070
				? super.toBooleanValueString( bool )
				: String.valueOf( bool );
	}

	@Override
	public String getCrossJoinSeparator() {
		//Derby 10.5 doesn't support 'cross join' syntax
		//Derby 10.6 and later support "cross join"
		return getVersion() < 1060 ? ", " : super.getCrossJoinSeparator();
	}

	@Override
	@SuppressWarnings("deprecation")
	public CaseFragment createCaseFragment() {
		return new DerbyCaseFragment();
	}

	@Override
	public boolean supportsSequences() {
		return getVersion() >= 1060;
	}

	@Override
	public boolean supportsPooledSequences() {
		return supportsSequences();
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		if ( supportsSequences() ) {
			return "values " + getSelectSequenceNextValString( sequenceName );
		}
		else {
			throw new MappingException( "Derby does not support sequence prior to release 10.6.1.0" );
		}
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return super.getDropSequenceString( sequenceName ) + " restrict";
	}

	@Override
	public String getQuerySequencesString() {
		if ( supportsSequences() ) {
			return "select sys.sysschemas.schemaname as sequence_schema, sys.syssequences.* from sys.syssequences left join sys.sysschemas on sys.syssequences.schemaid = sys.sysschemas.schemaid";
		}
		else {
			return null;
		}
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		if ( getQuerySequencesString() == null ) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE;
		}
		else {
			return SequenceInformationExtractorDerbyDatabaseImpl.INSTANCE;
		}
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
		return DB2Dialect.selectNullString( sqlType );
	}

	@Override
	public String getFromDual() {
		return "from (values 0) as dual";
	}

	@Override
	public boolean supportsCommentOn() {
		//HHH-4531
		return false;
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
	public String getWriteLockString(String aliases, int timeout) {
		return " for update of " + aliases + " with rs";
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
		//as far as I know, Derby doesn't support this
		return false;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
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
	public boolean supportsTuplesInSubqueries() {
		//checked on Derby 10.14
		return false;
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
	public boolean requiresCastingOfParametersInSelectClause() {
		//checked on Derby 10.14
		return true;
	}

	@Override
	public boolean supportsEmptyInList() {
		//checked on Derby 10.14
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		//checked on Derby 10.14
		return false;
	}

	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.NUMERIC
				? DecimalSqlDescriptor.INSTANCE
				: super.getSqlTypeDescriptorOverride(sqlCode);
	}

	@Override
	public String getNotExpression( String expression ) {
		return "not (" + expression + ")";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {
		builder.applyIdentifierCasing( dbMetaData );

		builder.applyReservedWords( dbMetaData );

		builder.applyReservedWords( getKeywords() );

		builder.setNameQualifierSupport( getNameQualifierSupport() );

		return builder.build();
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
//				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

				if( "40XL1".equals( sqlState ) || "40XL2".equals( sqlState ) ) {
					throw new LockTimeoutException( message, sqlException, sql );
				}
				return null;
			}
		};
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * From Derby docs:
	 * <pre>
	 *     The DECLARE GLOBAL TEMPORARY TABLE statement defines a temporary table for the current connection.
	 * </pre>
	 *
	 * Here we return a local-temp-table strategy even though we are creating global temp tables.
	 * See HHH-10238 for details why...
	 * </p>
     */
	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy( generateIdTableSupport() ) {
			@Override
			public AfterUseAction getAfterUseAction() {
				return AfterUseAction.CLEAN;
			}
		};
	}

	protected IdTableSupport generateIdTableSupport() {
		// todo (6.0) : come back and verify this - different on 5.3, specifically dropping after each use
		//
		// Prior to DB2 9.7, "real" global temporary tables that can be shared between sessions
		// are *not* supported; even though the DB2 command says to declare a "global" temp table
		// Hibernate treats it as a "local" temp table.

		return new StandardIdTableSupport( new GlobalTempTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				return super.determineIdTableName(baseName);
			}
			@Override
			public Exporter<IdTable> getIdTableExporter() {
				return generateIdTableExporter();
			}
		};
	}

	protected Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			protected String getCreateOptions() {
				return "not logged";
			}
		};
	}

	@Override
	public String translateDatetimeFormat(String format) {
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

}
