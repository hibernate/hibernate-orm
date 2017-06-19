/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.io.FilterReader;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
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
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.spi.AnsiTrimFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.ConcatFunctionTemplate;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.BasicBinder;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

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

	private static final ClobSqlDescriptor HANA_CLOB_STREAM_BINDING = new ClobSqlDescriptor() {
		/** serial version uid. */
		private static final long serialVersionUID = -379042275442752102L;

		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Clob.class );
		}

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

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							options
					);

					if ( value instanceof ClobImplementer ) {
						st.setCharacterStream(
								name,
								new CloseSuppressingReader( characterStream.asReader() ),
								characterStream.getLength()
						);
					}
					else {
						st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
					}
				}
			};
		}
	};

	private static final NClobSqlDescriptor HANA_NCLOB_STREAM_BINDING = new NClobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( NClob.class );
		}

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
						st.setCharacterStream(
								index,
								new CloseSuppressingReader( characterStream.asReader() ),
								characterStream.getLength()
						);
					}
					else {
						st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
					}

				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							options
					);

					if ( value instanceof NClobImplementer ) {
						st.setCharacterStream(
								name,
								new CloseSuppressingReader( characterStream.asReader() ),
								characterStream.getLength()
						);
					}
					else {
						st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
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

		registerHibernateType( Types.NCLOB, StandardSpiBasicTypes.NCLOB.getJavaTypeDescriptor().getTypeName() );
		registerHibernateType( Types.NVARCHAR, StandardSpiBasicTypes.STRING.getJavaTypeDescriptor().getTypeName() );

		registerHanaKeywords();

		// createBlob() and createClob() are not supported by the HANA JDBC driver
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );
		registry.registerNamed( "to_date", StandardSpiBasicTypes.DATE );
		registry.registerNamed( "to_seconddate", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "to_time", StandardSpiBasicTypes.TIME );
		registry.registerNamed( "to_timestamp", StandardSpiBasicTypes.TIMESTAMP );

		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "current_utcdate", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "current_utctime", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_utctimestamp", StandardSpiBasicTypes.TIMESTAMP );

		registry.registerNamed( "add_days" );
		registry.registerNamed( "add_months" );
		registry.registerNamed( "add_seconds" );
		registry.registerNamed( "add_years" );
		registry.registerNamed( "dayname", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "dayofmonth", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "dayofyear", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "days_between", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "hour", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "isoweek", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "last_day", StandardSpiBasicTypes.DATE );
		registry.registerNamed( "localtoutc", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "minute", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "month", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "monthname", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "next_day", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "now", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "quarter", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "second", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "seconds_between", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "week", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "weekday", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "year", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "utctolocal", StandardSpiBasicTypes.TIMESTAMP );


		registry.registerNamed( "to_bigint", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "to_binary", StandardSpiBasicTypes.BINARY );
		registry.registerNamed( "to_decimal", StandardSpiBasicTypes.BIG_DECIMAL );
		registry.registerNamed( "to_double", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "to_int", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "to_integer", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "to_real", StandardSpiBasicTypes.FLOAT );
		registry.registerNamed( "to_smalldecimal", StandardSpiBasicTypes.BIG_DECIMAL );
		registry.registerNamed( "to_smallint", StandardSpiBasicTypes.SHORT );
		registry.registerNamed( "to_tinyint", StandardSpiBasicTypes.BYTE );

		registry.registerNamed( "abs" );
		registry.registerNamed( "acos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "asin", StandardSpiBasicTypes.DOUBLE );
		registry.namedTemplateBuilder( "atan2", "atan" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		registry.registerNamed( "bin2hex", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "bitand", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "ceil" );
		registry.registerNamed( "cos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "cosh", StandardSpiBasicTypes.DOUBLE );
		registry.namedTemplateBuilder( "cot", "cos" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		registry.registerNamed( "exp", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "floor" );
		registry.registerNamed( "greatest" );
		registry.registerNamed( "hex2bin", StandardSpiBasicTypes.BINARY );
		registry.registerNamed( "least" );
		registry.registerNamed( "ln", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "log", StandardSpiBasicTypes.DOUBLE );
		registry.namedTemplateBuilder( "log", "ln" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		registry.registerNamed( "power" );
		registry.registerNamed( "round" );
		registry.registerNamed( "mod", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "sign", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "sin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sinh", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sqrt", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "tan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "tanh", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "uminus" );

		registry.registerNamed( "to_alphanum", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "to_nvarchar", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "to_varchar", StandardSpiBasicTypes.STRING );

		registry.registerNamed( "ascii", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "char", StandardSpiBasicTypes.CHARACTER );
		registry.register( "concat", ConcatFunctionTemplate.INSTANCE );
		registry.registerNamed( "lcase", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "left", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "length", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "locate", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "lpad", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "ltrim", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "nchar", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "replace", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "right", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "rpad", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "rtrim", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "substr_after", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "substr_before", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "substring", StandardSpiBasicTypes.STRING );
		registry.register( "trim", AnsiTrimFunctionTemplate.INSTANCE );
		registry.registerNamed( "ucase", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "unicode", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "bit_length", "length(to_binary(?1))*8", StandardSpiBasicTypes.INTEGER );

		registry.registerNamed( "to_blob", StandardSpiBasicTypes.BLOB );
		registry.registerNamed( "to_clob", StandardSpiBasicTypes.CLOB );
		registry.registerNamed( "to_nclob", StandardSpiBasicTypes.NCLOB );

		registry.registerNamed( "coalesce" );
		registry.registerNoArgs( "current_connection", StandardSpiBasicTypes.INTEGER );
		registry.registerNoArgs( "current_schema", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "current_user", StandardSpiBasicTypes.STRING );
		registry.registerVarArgs( "grouping_id", StandardSpiBasicTypes.INTEGER, "(", ",", ")" );
		registry.registerNamed( "ifnull" );
		registry.registerNamed( "map" );
		registry.registerNamed( "nullif" );
		registry.registerNamed( "session_context" );
		registry.registerNoArgs( "session_user", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "sysuuid", StandardSpiBasicTypes.STRING );
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
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( getIdTableExporter() );
	}

	@Override
	protected Exporter<IdTable> getIdTableExporter() {
		return new GlobalTempTableExporter();
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

		String clause = getForUpdateString( lockMode ) + " of " + aliases;
		if(lockOptions.getTimeOut() == LockOptions.NO_WAIT) {
			clause += " nowait";
		}
		return clause;
	}

	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait";
	}

	@Override
	public String getReadLockString(int timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString();
		}
		else {
			return getForUpdateString();
		}
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
			return BitSqlDescriptor.INSTANCE;
		case Types.CLOB:
			return HANA_CLOB_STREAM_BINDING;
		case Types.NCLOB:
			return HANA_NCLOB_STREAM_BINDING;
		case Types.TINYINT:
			// tinyint is unsigned on HANA
			return SmallIntSqlDescriptor.INSTANCE;
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
		registerKeyword( "beforeQuery" );
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
