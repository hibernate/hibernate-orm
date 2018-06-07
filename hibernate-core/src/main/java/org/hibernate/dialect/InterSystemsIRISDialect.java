/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * A Hibernate dialect for InterSystems IRIS
 *
 * intended for  Hibernate 5.2+  and jdk 1.8
 *
 *  Hibernate works with intersystems-jdbc-3.0.0.jar, located in the dev\java\lib\JDK18 sub-directory
 *  of the InterSystems IRIS installation directory.
 * 	Hibernate properties example
 * 		hibernate.dialect org.hibernate.dialect.ISCDialect
 *		hibernate.connection.driver_class com.intersystems.jdbc.IRISDriver
 *		hibernate.connection.url jdbc:IRIS://127.0.0.1:1972/USER/*
 *		hibernate.connection.username _SYSTEM*
 *		hibernate.connection.password SYS*
 *   Change items marked by '*' to correspond to your system.
 *
 *
 * @author Jonathan Levinson, Ralph Vater, Dmitry Umansky
 *
 **/

package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.ConditionalParenthesisFunction;
import org.hibernate.dialect.function.ConvertFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.NvlFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardJDBCEscapeFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.identity.InterSystemsIRISIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.exception.internal.InterSystemsIRISSQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.InterSystemsIRISJoinFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.StandardBasicTypes;

public class InterSystemsIRISDialect extends Dialect {

	private LimitHandler limitHandler;

	/**
	 * Creates new <code>InterSystemsIRISDialect</code> instance. Sets up the JDBC /
	 * Cach&eacute; type mappings.
	 */
	public InterSystemsIRISDialect() {
		super();
		commonRegistration();
		registerFunctions();
		this.limitHandler = new TopLimitHandler(true, true);
	}

	protected final void commonRegistration() {
		// Note: For object <-> SQL datatype mappings see:
		//	 Configuration Manager | Advanced | SQL | System DDL Datatype Mappings
		//
		//	TBD	registerColumnType(Types.BINARY,        "binary($1)");
		// changed 08-11-2005, jsl
		registerColumnType( Types.BINARY, "varbinary($1)" );
		registerColumnType( Types.BIGINT, "BigInt" );
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "decimal" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
		registerColumnType( Types.LONGVARCHAR, "longvarchar" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.REAL, "real" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.VARBINARY, "longvarbinary" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.BLOB, "longvarbinary" );
		registerColumnType( Types.CLOB, "longvarchar" );
		registerColumnType(Types.BOOLEAN, "integer");
		registerColumnType( Types.BINARY, "varbinary($l)" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "false" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		getDefaultProperties().setProperty( Environment.USE_SQL_COMMENTS, "false" );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "acos", new StandardJDBCEscapeFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "%alphaup", new StandardSQLFunction( "%alphaup", StandardBasicTypes.STRING ) );
		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.STRING ) );
		registerFunction( "asin", new StandardJDBCEscapeFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardJDBCEscapeFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "($length(?1)*8)" ) );
		registerFunction( "ceiling", new StandardSQLFunction( "ceiling", StandardBasicTypes.INTEGER ) );
		registerFunction( "char", new StandardJDBCEscapeFunction( "char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "character_length", new StandardSQLFunction( "character_length", StandardBasicTypes.INTEGER ) );
		registerFunction( "char_length", new StandardSQLFunction( "char_length", StandardBasicTypes.INTEGER ) );
		registerFunction( "cos", new StandardJDBCEscapeFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardJDBCEscapeFunction( "cot", StandardBasicTypes.DOUBLE ) );
		registerFunction( "coalesce", new VarArgsSQLFunction( "coalesce(", ",", ")" ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "", "||", "" ) );
		registerFunction( "convert", new ConvertFunction() );
		registerFunction( "curdate", new StandardJDBCEscapeFunction( "curdate", StandardBasicTypes.DATE ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME, false ) );
		registerFunction(
				"current_timestamp", new ConditionalParenthesisFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP )
		);
		registerFunction( "curtime", new StandardJDBCEscapeFunction( "curtime", StandardBasicTypes.TIME ) );
		registerFunction( "database", new StandardJDBCEscapeFunction( "database", StandardBasicTypes.STRING ) );
		registerFunction( "dateadd", new VarArgsSQLFunction( StandardBasicTypes.TIMESTAMP, "dateadd(", ",", ")" ) );
		registerFunction( "datediff", new VarArgsSQLFunction( StandardBasicTypes.INTEGER, "datediff(", ",", ")" ) );
		registerFunction( "datename", new VarArgsSQLFunction( StandardBasicTypes.STRING, "datename(", ",", ")" ) );
		registerFunction( "datepart", new VarArgsSQLFunction( StandardBasicTypes.INTEGER, "datepart(", ",", ")" ) );
		registerFunction( "day", new StandardSQLFunction( "day", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayname", new StandardJDBCEscapeFunction( "dayname", StandardBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardJDBCEscapeFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardJDBCEscapeFunction( "dayofweek", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardJDBCEscapeFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		// is it necessary to register %exact since it can only appear in a where clause?
		registerFunction( "%exact", new StandardSQLFunction( "%exact", StandardBasicTypes.STRING ) );
		registerFunction( "exp", new StandardJDBCEscapeFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "%external", new StandardSQLFunction( "%external", StandardBasicTypes.STRING ) );
		registerFunction( "$extract", new VarArgsSQLFunction( StandardBasicTypes.INTEGER, "$extract(", ",", ")" ) );
		registerFunction( "$find", new VarArgsSQLFunction( StandardBasicTypes.INTEGER, "$find(", ",", ")" ) );
		registerFunction( "floor", new StandardSQLFunction( "floor", StandardBasicTypes.INTEGER ) );
		registerFunction( "getdate", new StandardSQLFunction( "getdate", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "hour", new StandardJDBCEscapeFunction( "hour", StandardBasicTypes.INTEGER ) );
		registerFunction( "ifnull", new VarArgsSQLFunction( "ifnull(", ",", ")" ) );
		registerFunction( "%internal", new StandardSQLFunction( "%internal" ) );
		registerFunction( "isnull", new VarArgsSQLFunction( "isnull(", ",", ")" ) );
		registerFunction( "isnumeric", new StandardSQLFunction( "isnumeric", StandardBasicTypes.INTEGER ) );
		registerFunction( "lcase", new StandardJDBCEscapeFunction( "lcase", StandardBasicTypes.STRING ) );
		registerFunction( "left", new StandardJDBCEscapeFunction( "left", StandardBasicTypes.STRING ) );
		registerFunction( "len", new StandardSQLFunction( "len", StandardBasicTypes.INTEGER ) );
		registerFunction( "$length", new VarArgsSQLFunction( "$length(", ",", ")" ) );
		registerFunction( "$list", new VarArgsSQLFunction( "$list(", ",", ")" ) );
		registerFunction( "$listdata", new VarArgsSQLFunction( "$listdata(", ",", ")" ) );
		registerFunction( "$listfind", new VarArgsSQLFunction( "$listfind(", ",", ")" ) );
		registerFunction( "$listget", new VarArgsSQLFunction( "$listget(", ",", ")" ) );
		registerFunction( "$listlength", new StandardSQLFunction( "$listlength", StandardBasicTypes.INTEGER ) );
		registerFunction( "locate", new StandardSQLFunction( "$FIND", StandardBasicTypes.INTEGER ) );
		registerFunction( "log", new StandardJDBCEscapeFunction( "log", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardJDBCEscapeFunction( "log", StandardBasicTypes.DOUBLE ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "minute", new StandardJDBCEscapeFunction( "minute", StandardBasicTypes.INTEGER ) );
		registerFunction( "mod", new StandardJDBCEscapeFunction( "mod", StandardBasicTypes.DOUBLE ) );
		registerFunction( "month", new StandardJDBCEscapeFunction( "month", StandardBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardJDBCEscapeFunction( "monthname", StandardBasicTypes.STRING ) );
		registerFunction( "now", new StandardJDBCEscapeFunction( "monthname", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "nullif", new VarArgsSQLFunction( "nullif(", ",", ")" ) );
		registerFunction( "nvl", new NvlFunction() );
		registerFunction( "%odbcin", new StandardSQLFunction( "%odbcin" ) );
		registerFunction( "%odbcout", new StandardSQLFunction( "%odbcin" ) );
		registerFunction( "%pattern", new VarArgsSQLFunction( StandardBasicTypes.STRING, "", "%pattern", "" ) );
		registerFunction( "pi", new StandardJDBCEscapeFunction( "pi", StandardBasicTypes.DOUBLE ) );
		registerFunction( "$piece", new VarArgsSQLFunction( StandardBasicTypes.STRING, "$piece(", ",", ")" ) );
		registerFunction( "position", new VarArgsSQLFunction( StandardBasicTypes.INTEGER, "position(", " in ", ")" ) );
		registerFunction( "power", new VarArgsSQLFunction( StandardBasicTypes.STRING, "power(", ",", ")" ) );
		registerFunction( "quarter", new StandardJDBCEscapeFunction( "quarter", StandardBasicTypes.INTEGER ) );
		registerFunction( "repeat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "repeat(", ",", ")" ) );
		registerFunction( "replicate", new VarArgsSQLFunction( StandardBasicTypes.STRING, "replicate(", ",", ")" ) );
		registerFunction( "right", new StandardJDBCEscapeFunction( "right", StandardBasicTypes.STRING ) );
		registerFunction( "round", new VarArgsSQLFunction( StandardBasicTypes.FLOAT, "round(", ",", ")" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardBasicTypes.STRING ) );
		registerFunction( "second", new StandardJDBCEscapeFunction( "second", StandardBasicTypes.INTEGER ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );
		registerFunction( "sin", new StandardJDBCEscapeFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "space", new StandardSQLFunction( "space", StandardBasicTypes.STRING ) );
		registerFunction( "%sqlstring", new VarArgsSQLFunction( StandardBasicTypes.STRING, "%sqlstring(", ",", ")" ) );
		registerFunction( "%sqlupper", new VarArgsSQLFunction( StandardBasicTypes.STRING, "%sqlupper(", ",", ")" ) );
		registerFunction( "sqrt", new StandardJDBCEscapeFunction( "SQRT", StandardBasicTypes.DOUBLE ) );
		registerFunction( "%startswith", new VarArgsSQLFunction( StandardBasicTypes.STRING, "", "%startswith", "" ) );
		registerFunction( "str", new SQLFunctionTemplate( StandardBasicTypes.STRING, "cast(?1 as char varying)" ) );
		registerFunction( "string", new VarArgsSQLFunction( StandardBasicTypes.STRING, "string(", ",", ")" ) );
		// note that %string is deprecated
		registerFunction( "%string", new VarArgsSQLFunction( StandardBasicTypes.STRING, "%string(", ",", ")" ) );
		registerFunction( "substr", new VarArgsSQLFunction( StandardBasicTypes.STRING, "substr(", ",", ")" ) );
		registerFunction( "substring", new VarArgsSQLFunction( StandardBasicTypes.STRING, "substring(", ",", ")" ) );
		registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction( "tan", new StandardJDBCEscapeFunction( "tan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "timestampadd", new StandardJDBCEscapeFunction( "timestampadd", StandardBasicTypes.DOUBLE ) );
		registerFunction( "timestampdiff", new StandardJDBCEscapeFunction( "timestampdiff", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tochar", new VarArgsSQLFunction( StandardBasicTypes.STRING, "tochar(", ",", ")" ) );
		registerFunction( "to_char", new VarArgsSQLFunction( StandardBasicTypes.STRING, "to_char(", ",", ")" ) );
		registerFunction( "todate", new VarArgsSQLFunction( StandardBasicTypes.STRING, "todate(", ",", ")" ) );
		registerFunction( "to_date", new VarArgsSQLFunction( StandardBasicTypes.STRING, "todate(", ",", ")" ) );
		registerFunction( "tonumber", new StandardSQLFunction( "tonumber" ) );
		registerFunction( "to_number", new StandardSQLFunction( "tonumber" ) );
		// TRIM(end_keyword string-expression-1 FROM string-expression-2)
		// use Hibernate implementation "From" is one of the parameters they pass in position ?3
		//registerFunction( "trim", new SQLFunctionTemplate(StandardBasicTypes.STRING, "trim(?1 ?2 from ?3)") );
		registerFunction( "truncate", new StandardJDBCEscapeFunction( "truncate", StandardBasicTypes.STRING ) );
		registerFunction( "ucase", new StandardJDBCEscapeFunction( "ucase", StandardBasicTypes.STRING ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		// %upper is deprecated
		registerFunction( "%upper", new StandardSQLFunction( "%upper" ) );
		registerFunction( "user", new StandardJDBCEscapeFunction( "user", StandardBasicTypes.STRING ) );
		registerFunction( "week", new StandardJDBCEscapeFunction( "user", StandardBasicTypes.INTEGER ) );
		registerFunction( "xmlconcat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "xmlconcat(", ",", ")" ) );
		registerFunction( "xmlelement", new VarArgsSQLFunction( StandardBasicTypes.STRING, "xmlelement(", ",", ")" ) );
		// xmlforest requires a new kind of function constructor
		registerFunction( "year", new StandardJDBCEscapeFunction( "year", StandardBasicTypes.INTEGER ) );

	}

	protected final void registerFunctions() {
		this.registerFunction( "str", new VarArgsSQLFunction( StandardBasicTypes.STRING, "str(", ",", ")" ) );
		//new overwrites
		registerFunction( "year", new StandardSQLFunction( "year", StandardBasicTypes.INTEGER ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardJDBCEscapeFunction( "log10", StandardBasicTypes.DOUBLE ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP, false ));
	}



	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean hasAlterTable() {
		// Does this dialect support the ALTER TABLE syntax?
		return true;
	}

	@Override
	public boolean qualifyIndexName() {
		// Do we need to qualify index names with the schema name?
		return false;
	}

	@Override
	@SuppressWarnings("StringBufferReplaceableByString")
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		// The syntax used to add a foreign key constraint to a table.
		return new StringBuilder( 300 )
				.append( " ADD CONSTRAINT " )
				.append( constraintName )
				.append( " FOREIGN KEY " )
				.append( constraintName )
				.append( " (" )
				.append( StringHelper.join( ", ", foreignKey ) )
				.append( ") REFERENCES " )
				.append( referencedTable )
				.append( " (" )
				.append( StringHelper.join( ", ", primaryKey ) )
				.append( ") " )
				.toString();
	}

	/**
	 * Does this dialect support check constraints?
	 *
	 * @return {@code false} (InterSystemsIRIS does not support check constraints)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public boolean supportsCheck() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		// The syntax used to add a column to a table
		return " add column";
	}

	@Override
	public String getCascadeConstraintsString() {
		// Completely optional cascading drop clause.
		return "";
	}

	@Override
	public boolean dropConstraints() {
		// Do we need to drop constraints before dropping tables in this dialect?
		return true;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return true;
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String generateIdTableName(String baseName) {
						final String name = super.generateIdTableName( baseName );
						return name.length() > 25 ? name.substring( 1, 25 ) : name;
					}

					@Override
					public String getCreateIdTableCommand() {
						return "create global temporary table";
					}
				},
				AfterUseAction.CLEAN
		);
	}

/*	@Override
	public Class getNativeIdentifierGeneratorStrategy() {
		return  IdentityGenerator.class;
	}
*/
	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new InterSystemsIRISIdentityColumnSupport();
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsSequences() {
		return false;
	}

// It really does support sequences, but InterSystems elects to suggest usage of IDENTITY instead :/
// Anyway, below are the actual support overrides for users wanting to use this combo...
//
//	public String getSequenceNextValString(String sequenceName) {
//		return "select InterSystems.Sequences_GetNext('" + sequenceName + "') from InterSystems.Sequences where ucase(name)=ucase('" + sequenceName + "')";
//	}
//
//	public String getSelectSequenceNextValString(String sequenceName) {
//		return "(select InterSystems.Sequences_GetNext('" + sequenceName + "') from InterSystems.Sequences where ucase(name)=ucase('" + sequenceName + "'))";
//	}
//
//	public String getCreateSequenceString(String sequenceName) {
//		return "insert into InterSystems.Sequences(Name) values (ucase('" + sequenceName + "'))";
//	}
//
//	public String getDropSequenceString(String sequenceName) {
//		return "delete from InterSystems.Sequences where ucase(name)=ucase('" + sequenceName + "')";
//	}
//
//	public String getQuerySequencesString() {
//		return "select name from InterSystems.Sequences";
//	}

	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {

		// Just to make some tests happy, but InterSystems IRIS doesn't really support this.
		// need to use READ_COMMITTED as isolation level
		if (LockMode.UPGRADE == lockMode)
		{
			return new SelectLockingStrategy(lockable, lockMode);
		}

		// InterSystems InterSystemsIRIS does not current support "SELECT ... FOR UPDATE" syntax...
		// Set your transaction mode to READ_COMMITTED before using
		if ( lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_WRITE) {
			return new PessimisticWriteUpdateLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_READ) {
			return new PessimisticReadUpdateLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC) {
			return new OptimisticLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC_FORCE_INCREMENT) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	// LIMIT support (ala TOP) ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public LimitHandler getLimitHandler() {
		if ( isLegacyLimitHandlerBehaviorEnabled() ) {
			return super.getLimitHandler();
		}
		return limitHandler;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsVariableLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean bindLimitParametersFirst() {
		// Does the LIMIT clause come at the start of the SELECT statement, rather than at the end?
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean useMaxForLimit() {
		// Does the LIMIT clause take a "maximum" row number instead of a total number of returned rows?
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getLimitString(String sql, boolean hasOffset) {
		if ( hasOffset ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}

		// This does not support the InterSystems IRIS SQL 'DISTINCT BY (comma-list)' extensions,
		// but this extension is not supported through Hibernate anyway.
		final int insertionPoint = sql.startsWith( "select distinct" ) ? 15 : 6;

		return new StringBuilder( sql.length() + 8 )
				.append( sql )
				.insert( insertionPoint, " TOP ? " )
				.toString();
	}

	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getLowercaseFunction() {
		// The name of the SQL function that transforms a string to lowercase
		return "lower";
	}

	@Override
	public String getNullColumnString() {
		return "";
	}

	@Override
	public JoinFragment createOuterJoinFragment() {
		// Create an OuterJoinGenerator for this dialect.
		return new InterSystemsIRISJoinFragment();
	}

	@Override
	public String getNoColumnsInsertString() {
		// The keyword used to insert a row without specifying
		// any column values
		return " default values";
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new InterSystemsIRISSQLExceptionConversionDelegate( this );
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	/**
	 * The InterSystemsI RIS ViolatedConstraintNameExtracter.
	 */
	public static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			return extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() );
		}
	};


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	/**
	 * ddl like ""value" integer null check ("value">=2 AND "value"<=10)" isn't supported
	 */
	@Override
	public boolean supportsColumnCheck()
	{
		return false;
	}

	/**
	 * select count(distinct a,b,c) from hasi
	 * isn't supported ;)
	 */
	@Override
	public boolean supportsTupleDistinctCounts()
	{
		return false;
	}

	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}

	@Override
	public ScrollMode defaultScrollMode()
	{
		return super.defaultScrollMode();
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}
}
