/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.io.FilterReader;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.AnsiTrimFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BitTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.NClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SmallIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * An abstract base class for HANA dialects. <br/>
 * <a href="http://help.sap.com/hana/html/sqlmain.html">SAP HANA Reference</a> <br/>
 *
 * NOTE: This dialect is currently configured to create foreign keys with
 * <code>on update cascade</code>.
 *
 * @author Andrew Clemons <andrew.clemons@sap.com>
 */
public abstract class AbstractHANADialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			return sql + ( hasOffset ? " limit ? offset ?" : " limit ?" );
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersInReverseOrder() {
			return true;
		}
	};

	private static class CloseSuppressingReader extends FilterReader {
		protected CloseSuppressingReader(final Reader in) {
			super( in );
		}

		@Override
		public void close() {
			// do not close
		}
	}

	// the ClobTypeDescriptor and NClobTypeDescriptor for HANA are slightly
	// changed from the standard ones. The HANA JDBC driver currently closes any
	// stream passed in via
	// PreparedStatement.setCharacterStream(int,Reader,long)
	// after the stream has been processed. this causes problems later if we are
	// using non-contexual lob creation and HANA then closes our StringReader.
	// see test case LobLocatorTest

	private static final ClobTypeDescriptor HANA_CLOB_STREAM_BINDING = new ClobTypeDescriptor() {
		/** serial version uid. */
		private static final long serialVersionUID = -379042275442752102L;

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(final PreparedStatement st, final X value, final int index,
						final WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class,
							options );

					if ( value instanceof ClobImplementer ) {
						st.setCharacterStream( index, new CloseSuppressingReader( characterStream.asReader() ),
								characterStream.getLength() );
					}
					else {
						st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
					}

				}
			};
		}
	};

	private static final NClobTypeDescriptor HANA_NCLOB_STREAM_BINDING = new NClobTypeDescriptor() {
		/** serial version uid. */
		private static final long serialVersionUID = 5651116091681647859L;

		@Override
		public <X> BasicBinder<X> getNClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(final PreparedStatement st, final X value, final int index,
						final WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class,
							options );

					if ( value instanceof NClobImplementer ) {
						st.setCharacterStream( index, new CloseSuppressingReader( characterStream.asReader() ),
								characterStream.getLength() );
					}
					else {
						st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
					}

				}
			};
		}
	};

	public AbstractHANADialect() {
		super();

		registerColumnType( Types.DECIMAL, "decimal($p, $s)" );
		registerColumnType( Types.DOUBLE, "double" );

		// varbinary max length 5000
		registerColumnType( Types.BINARY, 5000, "varbinary($l)" );
		registerColumnType( Types.VARBINARY, 5000, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, 5000, "varbinary($l)" );

		// for longer values, map to blob
		registerColumnType( Types.BINARY, "blob" );
		registerColumnType( Types.VARBINARY, "blob" );
		registerColumnType( Types.LONGVARBINARY, "blob" );

		registerColumnType( Types.CHAR, "varchar(1)" );
		registerColumnType( Types.VARCHAR, 5000, "varchar($l)" );
		registerColumnType( Types.LONGVARCHAR, 5000, "varchar($l)" );
		registerColumnType( Types.NVARCHAR, 5000, "nvarchar($l)" );

		// for longer values map to clob/nclob
		registerColumnType( Types.LONGVARCHAR, "clob" );
		registerColumnType( Types.VARCHAR, "clob" );
		registerColumnType( Types.NVARCHAR, "nclob" );
		registerColumnType( Types.CLOB, "clob" );

		registerColumnType( Types.BOOLEAN, "tinyint" );

		// map bit/tinyint to smallint since tinyint is unsigned on HANA
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );

		registerHibernateType( Types.NCLOB, StandardBasicTypes.NCLOB.getName() );
		registerHibernateType( Types.NVARCHAR, StandardBasicTypes.STRING.getName() );

		registerFunction( "to_date", new StandardSQLFunction( "to_date", StandardBasicTypes.DATE ) );
		registerFunction( "to_seconddate", new StandardSQLFunction( "to_seconddate", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "to_time", new StandardSQLFunction( "to_time", StandardBasicTypes.TIME ) );
		registerFunction( "to_timestamp", new StandardSQLFunction( "to_timestamp", StandardBasicTypes.TIMESTAMP ) );

		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME, false ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP,
				false ) );
		registerFunction( "current_utcdate", new NoArgSQLFunction( "current_utcdate", StandardBasicTypes.DATE, false ) );
		registerFunction( "current_utctime", new NoArgSQLFunction( "current_utctime", StandardBasicTypes.TIME, false ) );
		registerFunction( "current_utctimestamp", new NoArgSQLFunction( "current_utctimestamp",
				StandardBasicTypes.TIMESTAMP, false ) );

		registerFunction( "add_days", new StandardSQLFunction( "add_days" ) );
		registerFunction( "add_months", new StandardSQLFunction( "add_months" ) );
		registerFunction( "add_seconds", new StandardSQLFunction( "add_seconds" ) );
		registerFunction( "add_years", new StandardSQLFunction( "add_years" ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "days_between", new StandardSQLFunction( "days_between", StandardBasicTypes.INTEGER ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", StandardBasicTypes.INTEGER ) );
		registerFunction( "isoweek", new StandardSQLFunction( "isoweek", StandardBasicTypes.STRING ) );
		registerFunction( "last_day", new StandardSQLFunction( "last_day", StandardBasicTypes.DATE ) );
		registerFunction( "localtoutc", new StandardSQLFunction( "localtoutc", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardBasicTypes.STRING ) );
		registerFunction( "next_day", new StandardSQLFunction( "next_day", StandardBasicTypes.DATE ) );
		registerFunction( "now", new NoArgSQLFunction( "now", StandardBasicTypes.TIMESTAMP, true ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardBasicTypes.STRING ) );
		registerFunction( "second", new StandardSQLFunction( "second", StandardBasicTypes.INTEGER ) );
		registerFunction( "seconds_between", new StandardSQLFunction( "seconds_between", StandardBasicTypes.LONG ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardBasicTypes.INTEGER ) );
		registerFunction( "weekday", new StandardSQLFunction( "weekday", StandardBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardBasicTypes.INTEGER ) );
		registerFunction( "utctolocal", new StandardSQLFunction( "utctolocal", StandardBasicTypes.TIMESTAMP ) );

		registerFunction( "to_bigint", new StandardSQLFunction( "to_bigint", StandardBasicTypes.LONG ) );
		registerFunction( "to_binary", new StandardSQLFunction( "to_binary", StandardBasicTypes.BINARY ) );
		registerFunction( "to_decimal", new StandardSQLFunction( "to_decimal", StandardBasicTypes.BIG_DECIMAL ) );
		registerFunction( "to_double", new StandardSQLFunction( "to_double", StandardBasicTypes.DOUBLE ) );
		registerFunction( "to_int", new StandardSQLFunction( "to_int", StandardBasicTypes.INTEGER ) );
		registerFunction( "to_integer", new StandardSQLFunction( "to_integer", StandardBasicTypes.INTEGER ) );
		registerFunction( "to_real", new StandardSQLFunction( "to_real", StandardBasicTypes.FLOAT ) );
		registerFunction( "to_smalldecimal",
				new StandardSQLFunction( "to_smalldecimal", StandardBasicTypes.BIG_DECIMAL ) );
		registerFunction( "to_smallint", new StandardSQLFunction( "to_smallint", StandardBasicTypes.SHORT ) );
		registerFunction( "to_tinyint", new StandardSQLFunction( "to_tinyint", StandardBasicTypes.BYTE ) );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "bin2hex", new StandardSQLFunction( "bin2hex", StandardBasicTypes.STRING ) );
		registerFunction( "bitand", new StandardSQLFunction( "bitand", StandardBasicTypes.LONG ) );
		registerFunction( "ceil", new StandardSQLFunction( "ceil" ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cosh", new StandardSQLFunction( "cosh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "floor", new StandardSQLFunction( "floor" ) );
		registerFunction( "greatest", new StandardSQLFunction( "greatest" ) );
		registerFunction( "hex2bin", new StandardSQLFunction( "hex2bin", StandardBasicTypes.BINARY ) );
		registerFunction( "least", new StandardSQLFunction( "least" ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "power", new StandardSQLFunction( "power" ) );
		registerFunction( "round", new StandardSQLFunction( "round" ) );
		registerFunction( "mod", new StandardSQLFunction( "mod", StandardBasicTypes.INTEGER ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sinh", new StandardSQLFunction( "sinh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tanh", new StandardSQLFunction( "tanh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "uminus", new StandardSQLFunction( "uminus" ) );

		registerFunction( "to_alphanum", new StandardSQLFunction( "to_alphanum", StandardBasicTypes.STRING ) );
		registerFunction( "to_nvarchar", new StandardSQLFunction( "to_nvarchar", StandardBasicTypes.STRING ) );
		registerFunction( "to_varchar", new StandardSQLFunction( "to_varchar", StandardBasicTypes.STRING ) );

		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase", StandardBasicTypes.STRING ) );
		registerFunction( "left", new StandardSQLFunction( "left", StandardBasicTypes.STRING ) );
		registerFunction( "length", new StandardSQLFunction( "length", StandardBasicTypes.LONG ) );
		registerFunction( "locate", new StandardSQLFunction( "locate", StandardBasicTypes.INTEGER ) );
		registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim", StandardBasicTypes.STRING ) );
		registerFunction( "nchar", new StandardSQLFunction( "nchar", StandardBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
		registerFunction( "right", new StandardSQLFunction( "right", StandardBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardBasicTypes.STRING ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardBasicTypes.STRING ) );
		registerFunction( "substr_after", new StandardSQLFunction( "substr_after", StandardBasicTypes.STRING ) );
		registerFunction( "substr_before", new StandardSQLFunction( "substr_before", StandardBasicTypes.STRING ) );
		registerFunction( "substring", new StandardSQLFunction( "substring", StandardBasicTypes.STRING ) );
		registerFunction( "trim", new AnsiTrimFunction() );
		registerFunction( "ucase", new StandardSQLFunction( "ucase", StandardBasicTypes.STRING ) );
		registerFunction( "unicode", new StandardSQLFunction( "unicode", StandardBasicTypes.INTEGER ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "length(to_binary(?1))*8" ) );

		registerFunction( "to_blob", new StandardSQLFunction( "to_blob", StandardBasicTypes.BLOB ) );
		registerFunction( "to_clob", new StandardSQLFunction( "to_clob", StandardBasicTypes.CLOB ) );
		registerFunction( "to_nclob", new StandardSQLFunction( "to_nclob", StandardBasicTypes.NCLOB ) );

		registerFunction( "coalesce", new StandardSQLFunction( "coalesce" ) );
		registerFunction( "current_connection", new NoArgSQLFunction( "current_connection", StandardBasicTypes.INTEGER,
				false ) );
		registerFunction( "current_schema", new NoArgSQLFunction( "current_schema", StandardBasicTypes.STRING, false ) );
		registerFunction( "current_user", new NoArgSQLFunction( "current_user", StandardBasicTypes.STRING, false ) );
		registerFunction( "grouping_id", new VarArgsSQLFunction( StandardBasicTypes.INTEGER, "(", ",", ")" ) );
		registerFunction( "ifnull", new StandardSQLFunction( "ifnull" ) );
		registerFunction( "map", new StandardSQLFunction( "map" ) );
		registerFunction( "nullif", new StandardSQLFunction( "nullif" ) );
		registerFunction( "session_context", new StandardSQLFunction( "session_context" ) );
		registerFunction( "session_user", new NoArgSQLFunction( "session_user", StandardBasicTypes.STRING, false ) );
		registerFunction( "sysuuid", new NoArgSQLFunction( "sysuuid", StandardBasicTypes.STRING, false ) );

		registerHanaKeywords();

		// createBlob() and createClob() are not supported by the HANA JDBC driver
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(final SQLException sqlException, final String message, final String sql) {

				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

				if ( errorCode == 131 ) {
					// 131 - Transaction rolled back by lock wait timeout
					return new LockTimeoutException( message, sqlException, sql );
				}

				if ( errorCode == 146 ) {
					// 146 - Resource busy and acquire with NOWAIT specified
					return new LockTimeoutException( message, sqlException, sql );
				}

				if ( errorCode == 132 ) {
					// 132 - Transaction rolled back due to unavailable resource
					return new LockAcquisitionException( message, sqlException, sql );
				}

				if ( errorCode == 133 ) {
					// 133 - Transaction rolled back by detected deadlock
					return new LockAcquisitionException( message, sqlException, sql );
				}

				// 259 - Invalid table name
				// 260 - Invalid column name
				// 261 - Invalid index name
				// 262 - Invalid query name
				// 263 - Invalid alias name
				if ( errorCode == 257 || ( errorCode >= 259 && errorCode <= 263 ) ) {
					throw new SQLGrammarException( message, sqlException, sql );
				}

				// 257 - Cannot insert NULL or update to NULL
				// 301 - Unique constraint violated
				// 461 - foreign key constraint violation
				// 462 - failed on update or delete by foreign key constraint violation
				if ( errorCode == 287 || errorCode == 301 || errorCode == 461 || errorCode == 462 ) {
					final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName(
							sqlException );

					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}

				return null;
			}
		};
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
	}

	@Override
	public String getAddColumnString() {
		return "add (";
	}

	@Override
	public String getAddColumnSuffixString() {
		return ")";
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getCreateSequenceString(final String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String getCreateIdTableCommand() {
						return "create global temporary table";
					}
				},
				AfterUseAction.CLEAN
		);
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp from dummy";
	}

	@Override
	public String getDropSequenceString(final String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getForUpdateString(final String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateString(final String aliases, final LockOptions lockOptions) {
		LockMode lockMode = lockOptions.getLockMode();
		final Iterator<Map.Entry<String, LockMode>> itr = lockOptions.getAliasLockIterator();
		while ( itr.hasNext() ) {
			// seek the highest lock mode
			final Map.Entry<String, LockMode> entry = itr.next();
			final LockMode lm = entry.getValue();
			if ( lm.greaterThan( lockMode ) ) {
				lockMode = lm;
			}
		}

		// not sure why this is sometimes empty
		if ( aliases == null || "".equals( aliases ) ) {
			return getForUpdateString( lockMode );
		}

		return getForUpdateString( lockMode ) + " of " + aliases;
	}

	@Override
	public String getLimitString(final String sql, final boolean hasOffset) {
		return new StringBuilder( sql.length() + 20 ).append( sql )
				.append( hasOffset ? " limit ? offset ?" : " limit ?" ).toString();
	}

	@Override
	public String getNotExpression(final String expression) {
		return "not (" + expression + ")";
	}

	@Override
	public String getQuerySequencesString() {
		return "select sequence_name from sys.sequences";
	}

	@Override
	public String getSelectSequenceNextValString(final String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getSequenceNextValString(final String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from dummy";
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(final int sqlCode) {
		switch ( sqlCode ) {
		case Types.BOOLEAN:
			return BitTypeDescriptor.INSTANCE;
		case Types.CLOB:
			return HANA_CLOB_STREAM_BINDING;
		case Types.NCLOB:
			return HANA_NCLOB_STREAM_BINDING;
		case Types.TINYINT:
			// tinyint is unsigned on HANA
			return SmallIntTypeDescriptor.INSTANCE;
		default:
			return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	protected void registerHanaKeywords() {
		registerKeyword( "all" );
		registerKeyword( "alter" );
		registerKeyword( "as" );
		registerKeyword( "before" );
		registerKeyword( "begin" );
		registerKeyword( "both" );
		registerKeyword( "case" );
		registerKeyword( "char" );
		registerKeyword( "condition" );
		registerKeyword( "connect" );
		registerKeyword( "cross" );
		registerKeyword( "cube" );
		registerKeyword( "current_connection" );
		registerKeyword( "current_date" );
		registerKeyword( "current_schema" );
		registerKeyword( "current_time" );
		registerKeyword( "current_timestamp" );
		registerKeyword( "current_user" );
		registerKeyword( "current_utcdate" );
		registerKeyword( "current_utctime" );
		registerKeyword( "current_utctimestamp" );
		registerKeyword( "currval" );
		registerKeyword( "cursor" );
		registerKeyword( "declare" );
		registerKeyword( "distinct" );
		registerKeyword( "else" );
		registerKeyword( "elseif" );
		registerKeyword( "elsif" );
		registerKeyword( "end" );
		registerKeyword( "except" );
		registerKeyword( "exception" );
		registerKeyword( "exec" );
		registerKeyword( "for" );
		registerKeyword( "from" );
		registerKeyword( "full" );
		registerKeyword( "group" );
		registerKeyword( "having" );
		registerKeyword( "if" );
		registerKeyword( "in" );
		registerKeyword( "inner" );
		registerKeyword( "inout" );
		registerKeyword( "intersect" );
		registerKeyword( "into" );
		registerKeyword( "is" );
		registerKeyword( "join" );
		registerKeyword( "leading" );
		registerKeyword( "left" );
		registerKeyword( "limit" );
		registerKeyword( "loop" );
		registerKeyword( "minus" );
		registerKeyword( "natural" );
		registerKeyword( "nextval" );
		registerKeyword( "null" );
		registerKeyword( "on" );
		registerKeyword( "order" );
		registerKeyword( "out" );
		registerKeyword( "prior" );
		registerKeyword( "return" );
		registerKeyword( "returns" );
		registerKeyword( "reverse" );
		registerKeyword( "right" );
		registerKeyword( "rollup" );
		registerKeyword( "rowid" );
		registerKeyword( "select" );
		registerKeyword( "set" );
		registerKeyword( "sql" );
		registerKeyword( "start" );
		registerKeyword( "sysdate" );
		registerKeyword( "systime" );
		registerKeyword( "systimestamp" );
		registerKeyword( "sysuuid" );
		registerKeyword( "top" );
		registerKeyword( "trailing" );
		registerKeyword( "union" );
		registerKeyword( "using" );
		registerKeyword( "utcdate" );
		registerKeyword( "utctime" );
		registerKeyword( "utctimestamp" );
		registerKeyword( "values" );
		registerKeyword( "when" );
		registerKeyword( "where" );
		registerKeyword( "while" );
		registerKeyword( "with" );
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		// HANA does not support circular constraints
		return false;
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	/**
	 * HANA currently does not support check constraints.
	 */
	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
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
	public boolean supportsExpectedLobUsagePattern() {
		// http://scn.sap.com/thread/3221812
		return false;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsTableCheck() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}

	@Override
	public int getMaxAliasLength() {
		return 128;
	}

	/**
	 * The default behaviour for 'on update restrict' on HANA is currently
	 * to not allow any updates to any column of a row if the row has a 
	 * foreign key. Make the default for foreign keys have 'on update cascade'
	 * to work around the issue.
	 */
	@Override
	public String getAddForeignKeyConstraintString(final String constraintName, final String[] foreignKey,
			final String referencedTable, final String[] primaryKey, final boolean referencesPrimaryKey) {
		return super.getAddForeignKeyConstraintString(constraintName, foreignKey, referencedTable, primaryKey, referencesPrimaryKey) + " on update cascade";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}
}
