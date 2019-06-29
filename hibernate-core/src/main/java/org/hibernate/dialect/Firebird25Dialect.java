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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.MappingException;
import org.hibernate.dialect.function.AnsiTrimFunction;
import org.hibernate.dialect.function.AvgWithArgumentCastFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorFirebird25DatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Dialect for Firebird 2.5.
 * <p>
 * Implementation note: This dialect intentionally does not inherit from {@link FirebirdDialect}
 * to abandon the dependency on {@link InterbaseDialect}.
 * </p>
 *
 * @author Mark Rotteveel
 */
public class Firebird25Dialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {

		private final Pattern selectPattern = Pattern.compile( "\\bselect\\b", Pattern.CASE_INSENSITIVE );

		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			final Matcher matcher = selectPattern.matcher( sql );
			if ( !matcher.find() ) {
				throw new UnsupportedOperationException( "Can't add limit to query without select" );
			}
			final int insertionPoint = matcher.end();
			return new StringBuilder( sql.length() + 20 )
					.append( sql )
					// Using first / skip, as rows has more limitations with placements
					.insert( insertionPoint, hasOffset ? " first ? skip ?" : " first ?" )
					.toString();
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersFirst() {
			return true;
		}

		@Override
		public boolean bindLimitParametersInReverseOrder() {
			return true;
		}
	};

	private static final ViolatedConstraintNameExtracter EXTRACTER = new ViolatedConstraintNameExtracter() {

		final Pattern foreignUniqueOrPrimaryKeyPattern = Pattern.compile( "violation of .+? constraint \"([^\"]+)\"" );
		final Pattern checkConstraintPattern = Pattern.compile(
				"Operation violates CHECK constraint (.+?) on view or table" );

		@Override
		public String extractConstraintName(SQLException sqle) {
			String message = sqle.getMessage();
			if ( message != null ) {
				Matcher foreignUniqueOrPrimaryKeyMatcher =
						foreignUniqueOrPrimaryKeyPattern.matcher( message );
				if ( foreignUniqueOrPrimaryKeyMatcher.find() ) {
					return foreignUniqueOrPrimaryKeyMatcher.group( 1 );
				}

				Matcher checkConstraintMatcher = checkConstraintPattern.matcher( message );
				if ( checkConstraintMatcher.find() ) {
					return checkConstraintMatcher.group( 1 );
				}
			}
			return null;
		}
	};

	private static final int MAX_NUMERIC_PRECISION = 18;
	protected static final int MAX_CHAR_BYTES = 32767;
	protected static final int MAX_VARCHAR_BYTES = 32765;
	protected static final int MAX_UTF8_CHAR_LENGTH = 8191;
	protected static final int MAX_UTF8_VARCHAR_LENGTH = 8191;

	/**
	 * Constructs a Firebird25Dialect
	 */
	public Firebird25Dialect() {
		super();
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BOOLEAN, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );

		registerColumnType( Types.VARBINARY, MAX_VARCHAR_BYTES, "varchar($l) character set octets" );
		registerColumnType( Types.VARBINARY, "blob sub_type binary" );
		registerColumnType( Types.LONGVARBINARY, "blob sub_type binary" );
		registerColumnType( Types.BINARY, MAX_CHAR_BYTES, "char($l) character set octets" );
		registerColumnType( Types.BINARY, "blob sub_type binary" );
		registerColumnType( Types.BLOB, "blob sub_type binary" );

		registerColumnType( Types.CHAR, MAX_UTF8_CHAR_LENGTH, "char($l)" );
		// 8191 is max length with UTF8
		registerColumnType( Types.VARCHAR, MAX_UTF8_VARCHAR_LENGTH, "varchar($l)" );
		// NCHAR/NCHAR VARYING is an alias for (var)char(x) character set ISO8859_1
		// Using max length UTF8, even though ISO889_1 is used (and theoretical limit is 32767/32765)
		registerColumnType( Types.NCHAR, MAX_UTF8_CHAR_LENGTH, "nchar($l)" );
		registerColumnType( Types.NVARCHAR, MAX_UTF8_VARCHAR_LENGTH, "nchar varying($l)" );
		registerColumnType( Types.VARCHAR, "blob sub_type text" );
		registerColumnType( Types.LONGVARCHAR, "blob sub_type text" );
		registerColumnType( Types.CLOB, "blob sub_type text" );
		// Firebird doesn't have NCLOB, but Jaybird emulates NCLOB support
		registerColumnType( Types.NCLOB, "blob sub_type text" );

		registerFunction(
				"substring",
				new SQLFunctionTemplate( StandardBasicTypes.STRING, "substring(?1 from ?2 for ?3)" )
		);
		registerFunction( "locate", new StandardSQLFunction( "position", StandardBasicTypes.INTEGER ) );
		registerFunction( "trim", new AnsiTrimFunction() );
		registerFunction( "length", new StandardSQLFunction( "char_length", StandardBasicTypes.INTEGER ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );

		registerFunction( "str", new SQLFunctionTemplate( StandardBasicTypes.STRING, "cast(?1 as varchar(256))" ) );

		registerFunction( "avg", new AvgWithArgumentCastFunction( "double precision" ) );

		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME, false ) );
		registerFunction(
				"current_timestamp",
				new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP, false )
		);

		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );

		registerKeyword( "position" );
	}

	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		if ( ( code == Types.NUMERIC || code == Types.DECIMAL )
				&& precision == Column.DEFAULT_PRECISION && scale == Column.DEFAULT_SCALE
				&& Column.DEFAULT_PRECISION > getMaxNumericPrecision() ) {
			// Hibernate defaults to precision 19 and scale 2, max precision for Firebird 3.0 and earlier is 18.
			// We assume this request was done with the default precision and scale and reduce precision to maximum supported
			precision = getMaxNumericPrecision();
		}
		return super.getTypeName( code, length, precision, scale );
	}

	/**
	 * @return Maximum numeric precision
	 */
	protected int getMaxNumericPrecision() {
		return MAX_NUMERIC_PRECISION;
	}

	/**
	 * @return Default precision to apply in {@link #getCastTypeName(int)} and {@link #cast(String, int, int)}.
	 */
	protected int getDefaultPrecision() {
		int maxPrecision = getMaxNumericPrecision();
		return maxPrecision < Column.DEFAULT_PRECISION ? maxPrecision : Column.DEFAULT_PRECISION;
	}

	@Override
	public String getCastTypeName(int code) {
		return getTypeName( code, Column.DEFAULT_LENGTH, getDefaultPrecision(), Column.DEFAULT_SCALE );
	}

	@Override
	public String cast(String value, int jdbcTypeCode, int length) {
		return cast( value, jdbcTypeCode, length, getDefaultPrecision(), Column.DEFAULT_SCALE );
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		// Technically yes, but only through GEN_ID(name, increment)
		return false;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) throws MappingException {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from RDB$DATABASE";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) throws MappingException {
		return "next value for " + sequenceName;
	}

	@Override
	protected String getCreateSequenceString(String sequenceName) throws MappingException {
		return "create sequence " + sequenceName;
	}

	@Override
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		if ( initialValue == 1 ) {
			return super.getCreateSequenceStrings( sequenceName, initialValue, incrementSize );
		}
		return new String[] {
				getCreateSequenceString( sequenceName, initialValue, incrementSize ),
				"alter sequence " + sequenceName + " restart with " + ( initialValue - 1 )
		};
	}

	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		if ( incrementSize != 1 ) {
			throw new MappingException( getClass().getName() + " does not support pooled sequences" );
		}
		// Ignore initialValue and incrementSize
		return getCreateSequenceString( sequenceName );
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getQuerySequencesString() {
		return "select RDB$GENERATOR_NAME from RDB$GENERATORS where coalesce(RDB$SYSTEM_FLAG, 0) = 0";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorFirebird25DatabaseImpl.INSTANCE;
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid_to_char(gen_uuid()) from RDB$DATABASE";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public boolean supportsLockTimeouts() {
		// Lock timeouts are only supported when specified as part of the transaction
		return false;
	}

	@Override
	public String getForUpdateString() {
		// locking only happens on fetch
		return " with lock";
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		// "WITH LOCK can only be used with a top-level, single-table SELECT statement.", https://www.firebirdsql.org/file/documentation/reference_manuals/fblangref25-en/html/fblangref25-dml-select.html#fblangref25-dml-with-lock
		return false;
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String generateIdTableName(String baseName) {
						final String name = super.generateIdTableName( baseName );
						return name.length() > 31 ? name.substring( 0, 31 ) : name;
					}

					@Override
					public String getCreateIdTableCommand() {
						return "create global temporary table";
					}

					@Override
					public String getCreateIdTableStatementOptions() {
						return "on commit delete rows";
					}
				},
				AfterUseAction.CLEAN
		);
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
	public String getCurrentTimestampSelectString() {
		return "select CURRENT_TIMESTAMP from RDB$DATABASE";
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				final String sqlExceptionMessage = sqlException.getMessage();
				//final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

				// Some of the error codes will only surface in Jaybird 3 or higher, as older versions return less specific error codes first
				switch ( errorCode ) {
					case 335544336:
						// isc_deadlock (deadlock, note: not necessarily a deadlock, can also be an update conflict)
						if (sqlExceptionMessage != null
								&& sqlExceptionMessage.contains( "update conflicts with concurrent update" )) {
							return new LockTimeoutException( message, sqlException, sql );
						}
						return new LockAcquisitionException( message, sqlException, sql );
					case 335544345:
						// isc_lock_conflict (lock conflict on no wait transaction)
					case 335544510:
						// isc_lock_timeout (lock time-out on wait transaction)
						return new LockTimeoutException( message, sqlException, sql );
					case 335544474:
						// isc_bad_lock_level (invalid lock level {0})
					case 335544475:
						// isc_relation_lock (lock on table {0} conflicts with existing lock)
					case 335544476:
						// isc_record_lock (requested record lock conflicts with existing lock)
						return new LockAcquisitionException( message, sqlException, sql );
					case 335544466:
						// isc_foreign_key (violation of FOREIGN KEY constraint "{0}" on table "{1}")
					case 336396758:
						// *no error name* (violation of FOREIGN KEY constraint "{0}")
					case 335544558:
						// isc_check_constraint (Operation violates CHECK constraint {0} on view or table {1})
					case 336396991:
						// *no error name* (Operation violates CHECK constraint {0} on view or table)
					case 335544665:
						// isc_unique_key_violation (violation of PRIMARY or UNIQUE KEY constraint "{0}" on table "{1}")
						final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName(
								sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}

				// Apply heuristics based on exception message
				String exceptionMessage = sqlException.getMessage();
				if ( exceptionMessage != null ) {
					if ( exceptionMessage.contains( "violation of " )
							|| exceptionMessage.contains( "violates CHECK constraint" ) ) {
						final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName(
								sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
					}
				}

				return null;
			}
		};
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public int getMaxAliasLength() {
		return 20;
	}

	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData dbMetaData) throws SQLException {
		// Any use of keywords as identifiers will result in token unknown error, so enable auto quote always
		builder.setAutoQuoteKeywords( true );

		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No create schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No drop schema syntax supported by " + getClass().getName() );
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean requiresCastingOfParametersInSelectClause() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		// May need changes in Jaybird for this to work
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		// Blob ids are only guaranteed to work in the same transaction
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		// see http://www.firebirdsql.org/file/documentation/reference_manuals/fblangref25-en/html/fblangref25-commons-predicates.html#fblangref25-commons-in
		return 1500;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}
}
