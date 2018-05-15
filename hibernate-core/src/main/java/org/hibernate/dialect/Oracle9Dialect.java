/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.NvlFunctionTemplate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.spi.ConcatFunctionTemplate;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorOracleDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

/**
 * An SQL dialect for Oracle 9 (uses ANSI-style syntax where possible).
 *
 * @author Gavin King
 * @author David Channon
 *
 * @deprecated Use either Oracle9iDialect or Oracle10gDialect instead
 */
@SuppressWarnings("deprecation")
@Deprecated
public class Oracle9Dialect extends Dialect {

	private static final int PARAM_LIST_SIZE_LIMIT = 1000;

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			Oracle9Dialect.class.getName()
	);

	/**
	 * Constructs a Oracle9Dialect
	 */
	public Oracle9Dialect() {
		super();
		LOG.deprecatedOracle9Dialect();
		registerColumnType( Types.BIT, "number(1,0)" );
		registerColumnType( Types.BIGINT, "number(19,0)" );
		registerColumnType( Types.SMALLINT, "number(5,0)" );
		registerColumnType( Types.TINYINT, "number(3,0)" );
		registerColumnType( Types.INTEGER, "number(10,0)" );
		registerColumnType( Types.CHAR, "char(1 char)" );
		registerColumnType( Types.VARCHAR, 4000, "varchar2($l char)" );
		registerColumnType( Types.VARCHAR, "long" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, 2000, "raw($l)" );
		registerColumnType( Types.VARBINARY, "long raw" );
		registerColumnType( Types.NUMERIC, "number($p,$s)" );
		registerColumnType( Types.DECIMAL, "number($p,$s)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		// Oracle driver reports to support getGeneratedKeys(), but they only
		// support the version taking an array of the names of the columns to
		// be returned (via its RETURNING clause).  No other driver seems to
		// support this overloaded version.
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "false" );
		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		getDefaultProperties().setProperty( Environment.BATCH_VERSIONED_DATA, "false" );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		CommonFunctionFactory.abs( registry );
		CommonFunctionFactory.sign( registry );

		CommonFunctionFactory.acos( registry );
		CommonFunctionFactory.asin( registry );
		CommonFunctionFactory.atan( registry );

		CommonFunctionFactory.cos( registry );
		CommonFunctionFactory.cosh( registry );
		CommonFunctionFactory.exp( registry );
		CommonFunctionFactory.ln( registry );
		CommonFunctionFactory.sin( registry );
		CommonFunctionFactory.sinh( registry );
		CommonFunctionFactory.stddev( registry );
		CommonFunctionFactory.sqrt( registry );
		CommonFunctionFactory.tan( registry );
		CommonFunctionFactory.tanh( registry );
		CommonFunctionFactory.variance( registry );

		CommonFunctionFactory.round( registry );
		CommonFunctionFactory.trunc( registry );
		CommonFunctionFactory.ceil( registry );
		CommonFunctionFactory.floor( registry );

		registry.namedTemplateBuilder( "chr" )
				.setInvariantType( StandardSpiBasicTypes.CHARACTER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "initcap" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.lower( registry );
		registry.namedTemplateBuilder( "ltrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "rtrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		CommonFunctionFactory.soundex( registry );
		CommonFunctionFactory.upper( registry );
		registry.namedTemplateBuilder( "ascii" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();

		registry.namedTemplateBuilder( "to_char" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 3 )
				.register();
		registry.namedTemplateBuilder( "to_date" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 1, 3 )
				.register();

		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.namedTemplateBuilder( "current_timestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 0, 1 )
				.register();

		registry.namedTemplateBuilder( "last_day" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
		registry.registerNoArgs( "sysdate", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "systimestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "uid", StandardSpiBasicTypes.INTEGER );
		registry.registerNoArgs( "user", StandardSpiBasicTypes.STRING );

		registry.registerNoArgs( "rowid", StandardSpiBasicTypes.LONG );
		registry.registerNoArgs( "rownum", StandardSpiBasicTypes.LONG );

		// Multi-param string dialect functions...
		registry.register( "concat", new ConcatFunctionTemplate( "", "||", "" ) );
		registry.namedTemplateBuilder( "instr" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 4 )
				.register();
		registry.namedTemplateBuilder( "instrb" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 4 )
				.register();
		registry.namedTemplateBuilder( "lpad" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "replace" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "rpad" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "substr" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "substrb" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "translate" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 3 )
				.register();

		registry.registerAlternateKey( "substring", "substr" );
		registry.registerPattern( "locate",  "instr(?2,?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "bit_length", "vsize(?1)*8", StandardSpiBasicTypes.INTEGER );
		registry.register( "coalesce", new NvlFunctionTemplate() );

		// Multi-param numeric dialect functions...
		registry.namedTemplateBuilder( "atan2" )
				.setInvariantType( StandardSpiBasicTypes.FLOAT )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.log( registry );
		CommonFunctionFactory.mod( registry );
		registry.namedTemplateBuilder( "nvl" )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "nvl2" )
				.setExactArgumentCount( 3 )
				.register();
		registry.namedTemplateBuilder( "power" )
				.setInvariantType( StandardSpiBasicTypes.FLOAT )
				.setExactArgumentCount( 2 )
				.register();

		// Multi-param date dialect functions...
		registry.namedTemplateBuilder( "add_months" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "months_between" )
				.setInvariantType( StandardSpiBasicTypes.FLOAT )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "next_day" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();

		registry.registerAlternateKey( "str", "to_char" );
	}

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
	public String getCreateSequenceString(String sequenceName) {
		//starts with 1, implicitly
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
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
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {

		sql = sql.trim();
		boolean isForUpdate = false;
		if ( sql.toLowerCase(Locale.ROOT).endsWith( " for update" ) ) {
			sql = sql.substring( 0, sql.length() - 11 );
			isForUpdate = true;
		}

		final StringBuilder pagingSelect = new StringBuilder( sql.length() + 100 );
		if ( hasOffset ) {
			pagingSelect.append( "select * from ( select row_.*, rownum rownum_ from ( " );
		}
		else {
			pagingSelect.append( "select * from ( " );
		}
		pagingSelect.append( sql );
		if ( hasOffset ) {
			pagingSelect.append( " ) row_ where rownum <= ?) where rownum_ > ?" );
		}
		else {
			pagingSelect.append( " ) where rownum <= ?" );
		}

		if ( isForUpdate ) {
			pagingSelect.append( " for update" );
		}

		return pagingSelect.toString();
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
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
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
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
			if ( errorCode == 1 || errorCode == 2291 || errorCode == 2292 ) {
				return extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() );
			}
			else if ( errorCode == 1400 ) {
				// simple nullability constraint
				return null;
			}
			else {
				return null;
			}
		}
	};

	@Override
	public int registerResultSetOutParameter(java.sql.CallableStatement statement, int col) throws SQLException {
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
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() ) {
			@Override
			protected String determineIdTableName(String baseName) {
				final String name = super.determineIdTableName( baseName );
				return name.length() > 30 ? name.substring( 0, 30 ) : name;
			}
		};
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			public String getCreateCommand() {
				return "create global temporary table";
			}

			@Override
			public String getCreateOptions() {
				return "on commit delete rows";
			}
		};
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select systimestamp from dual";
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
	public String getNotExpression(String expression) {
		return "not (" + expression + ")";
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}
}
