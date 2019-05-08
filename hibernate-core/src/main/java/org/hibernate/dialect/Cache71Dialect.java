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

import org.hibernate.LockMode;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.query.sqm.produce.function.spi.PairedFunctionTemplate;
import org.hibernate.dialect.identity.Chache71IdentityColumnSupport;
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
import org.hibernate.exception.internal.CacheSQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.metamodel.model.domain.spi.Lockable;
import org.hibernate.naming.Identifier;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.sql.CacheJoinFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Cach&eacute; 2007.1 dialect.
 *
 * This class is required in order to use Hibernate with Intersystems Cach&eacute; SQL.  Compatible with
 * Cach&eacute; 2007.1.
 *
 * <h2>PREREQUISITES</h2>
 * These setup instructions assume that both Cach&eacute; and Hibernate are installed and operational.
 * <br>
 * <h2>HIBERNATE DIRECTORIES AND FILES</h2>
 * JBoss distributes the InterSystems Cache' dialect for Hibernate 3.2.1
 * For earlier versions of Hibernate please contact
 * <a href="http://www.intersystems.com/support/cache-support.html">InterSystems Worldwide Response Center</A> (WRC)
 * for the appropriate source files.
 * <br>
 * <h2>CACH&Eacute; DOCUMENTATION</h2>
 * Documentation for Cach&eacute; is available online when Cach&eacute; is running.
 * It can also be obtained from the
 * <a href="http://www.intersystems.com/cache/downloads/documentation.html">InterSystems</A> website.
 * The book, "Object-oriented Application Development Using the Cach&eacute; Post-relational Database:
 * is also available from Springer-Verlag.
 * <br>
 * <h2>HIBERNATE DOCUMENTATION</h2>
 * Hibernate comes with extensive electronic documentation.
 * In addition, several books on Hibernate are available from
 * <a href="http://www.manning.com">Manning Publications Co</a>.
 * Three available titles are "Hibernate Quickly", "Hibernate in Action", and "Java Persistence with Hibernate".
 * <br>
 * <h2>TO SET UP HIBERNATE FOR USE WITH CACH&Eacute;</h2>
 * The following steps assume that the directory where Cach&eacute; was installed is C:\CacheSys.
 * This is the default installation directory for  Cach&eacute;.
 * The default installation directory for Hibernate is assumed to be C:\Hibernate.
 * <p/>
 * If either product is installed in a different location, the pathnames that follow should be modified appropriately.
 * <p/>
 * Cach&eacute; version 2007.1 and above is recommended for use with
 * Hibernate.  The next step depends on the location of your
 * CacheDB.jar depending on your version of Cach&eacute;.
 * <ol>
 * <li>Copy C:\CacheSys\dev\java\lib\JDK15\CacheDB.jar to C:\Hibernate\lib\CacheDB.jar.</li>
 * <p/>
 * <li>Insert the following files into your Java classpath:
 * <p/>
 * <ul>
 * <li>All jar files in the directory C:\Hibernate\lib</li>
 * <li>The directory (or directories) where hibernate.properties and/or hibernate.cfg.xml are kept.</li>
 * </ul>
 * </li>
 * <p/>
 * <li>In the file, hibernate.properties (or hibernate.cfg.xml),
 * specify the Cach&eacute; dialect and the Cach&eacute; version URL settings.</li>
 * </ol>
 * <p/>
 * For example, in Hibernate 3.2, typical entries in hibernate.properties would have the following
 * "name=value" pairs:
 * <p/>
 * <table cols=3 border cellpadding=5 cellspacing=0>
 * <tr>
 * <th>Property Name</th>
 * <th>Property Value</th>
 * </tr>
 * <tr>
 * <td>hibernate.dialect</td>
 * <td>org.hibernate.dialect.Cache71Dialect</td>
 * </tr>
 * <tr>
 * <td>hibernate.connection.driver_class</td>
 * <td>com.intersys.jdbc.CacheDriver</td>
 * </tr>
 * <tr>
 * <td>hibernate.connection.username</td>
 * <td>(see note 1)</td>
 * </tr>
 * <tr>
 * <td>hibernate.connection.password</td>
 * <td>(see note 1)</td>
 * </tr>
 * <tr>
 * <td>hibernate.connection.url</td>
 * <td>jdbc:Cache://127.0.0.1:1972/USER</td>
 * </tr>
 * </table>
 * <p/>
 * <b>NOTE:</b> Please contact your administrator for the userid and password you should use when
 *         attempting access via JDBC.  By default, these are chosen to be "_SYSTEM" and "SYS" respectively
 *         as noted in the SQL standard.
 * <br>
 * <h2>CACH&Eacute; VERSION URL</h2>
 * This is the standard URL for the JDBC driver.
 * For a JDBC driver on the machine hosting Cach&eacute;, use the IP "loopback" address, 127.0.0.1.
 * For 1972, the default port, specify the super server port of your Cach&eacute; instance.
 * For USER, substitute the NAMESPACE which contains your Cach&eacute; database data.
 * <br>
 * <h2>CACH&Eacute; DIALECTS</h2>
 * Choices for Dialect are:
 * <br>
 * <p/>
 * <ol>
 * <li>org.hibernate.dialect.Cache71Dialect (requires Cach&eacute;
 * 2007.1 or above)</li>
 * <p/>
 * </ol>
 * <br>
 * <h2>SUPPORT FOR IDENTITY COLUMNS</h2>
 * Cach&eacute; 2007.1 or later supports identity columns.  For
 * Hibernate to use identity columns, specify "native" as the
 * generator.
 * <br>
 * <h2>SEQUENCE DIALECTS SUPPORT SEQUENCES</h2>
 * <p/>
 * To use Hibernate sequence support with Cach&eacute; in a namespace, you must FIRST load the following file into that namespace:
 * <pre>
 *     etc\CacheSequences.xml
 * </pre>
 * For example, at the COS terminal prompt in the namespace, run the
 * following command:
 * <p>
 * d LoadFile^%apiOBJ("c:\hibernate\etc\CacheSequences.xml","ck")
 * <p>
 * In your Hibernate mapping you can specify sequence use.
 * <p>
 * For example, the following shows the use of a sequence generator in a Hibernate mapping:
 * <pre>
 *     &lt;id name="id" column="uid" type="long" unsaved-value="null"&gt;
 *         &lt;generator class="sequence"/&gt;
 *     &lt;/id&gt;
 * </pre>
 * <br>
 * <p/>
 * Some versions of Hibernate under some circumstances call
 * getSelectSequenceNextValString() in the dialect.  If this happens
 * you will receive the error message: new MappingException( "Dialect
 * does not support sequences" ).
 * <br>
 * <h2>HIBERNATE FILES ASSOCIATED WITH CACH&Eacute; DIALECT</h2>
 * The following files are associated with Cach&eacute; dialect:
 * <p/>
 * <ol>
 * <li>src\org\hibernate\dialect\Cache71Dialect.java</li>
 * <li>src\org\hibernate\dialect\function\ConditionalParenthesisFunction.java</li>
 * <li>src\org\hibernate\dialect\function\ConvertFunction.java</li>
 * <li>src\org\hibernate\exception\CacheSQLStateConverter.java</li>
 * <li>src\org\hibernate\sql\CacheJoinFragment.java</li>
 * </ol>
 * Cache71Dialect ships with Hibernate 3.2.  All other dialects are distributed by InterSystems and subclass Cache71Dialect.
 *
 * @author Jonathan Levinson
 */

public class Cache71Dialect extends Dialect {

	private LimitHandler limitHandler;

	/**
	 * Creates new <code>Cache71Dialect</code> instance. Sets up the JDBC /
	 * Cach&eacute; type mappings.
	 */
	public Cache71Dialect() {
		super();
		commonRegistration();
		this.limitHandler = new TopLimitHandler( true, true );
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

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "false" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		getDefaultProperties().setProperty( Environment.USE_SQL_COMMENTS, "false" );
	}

	static void useJdbcEscape(QueryEngine queryEngine, String name) {
		queryEngine.getSqmFunctionRegistry().wrapInJdbcEscape(
				name,
				queryEngine.getSqmFunctionRegistry().findFunctionTemplate(name)
		);
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerPattern( "bit_length", "($length(?1)*8)", StandardSpiBasicTypes.INTEGER );

		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		CommonFunctionFactory.concat_operator( queryEngine );
		CommonFunctionFactory.chr_char( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.daynameMonthname( queryEngine );
		CommonFunctionFactory.toCharNumberDateTimestamp( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.repeat_replicate( queryEngine );
		CommonFunctionFactory.leftRight( queryEngine );
		CommonFunctionFactory.extract_datepart( queryEngine );

		PairedFunctionTemplate.register(queryEngine, "locate", StandardSpiBasicTypes.INTEGER, "$find(?2, ?1)", "$find(?2, ?1, ?3)");

		useJdbcEscape(queryEngine, "sin");
		useJdbcEscape(queryEngine, "cos");
		useJdbcEscape(queryEngine, "tan");
		useJdbcEscape(queryEngine, "asin");
		useJdbcEscape(queryEngine, "acos");
		useJdbcEscape(queryEngine, "atan");
		useJdbcEscape(queryEngine, "atan2");
		useJdbcEscape(queryEngine, "exp");
		useJdbcEscape(queryEngine, "log");
		useJdbcEscape(queryEngine, "log10");
		useJdbcEscape(queryEngine, "pi");
		useJdbcEscape(queryEngine, "truncate");

		useJdbcEscape(queryEngine, "left");
		useJdbcEscape(queryEngine, "right");

		useJdbcEscape(queryEngine, "hour");
		useJdbcEscape(queryEngine, "minute");
		useJdbcEscape(queryEngine, "second");
		useJdbcEscape(queryEngine, "week");
		useJdbcEscape(queryEngine, "quarter");
		useJdbcEscape(queryEngine, "dayname");
		useJdbcEscape(queryEngine, "monthname");
		useJdbcEscape(queryEngine, "dayofweek");
		useJdbcEscape(queryEngine, "dayofmonth");
		useJdbcEscape(queryEngine, "dayofyear");

		// below is for Cache' that don't have str in 2007.1 there is str and we register str directly
//		queryEngine.getSqmFunctionRegistry().registerPattern( "str", "cast(?1 as char varying)", StandardSpiBasicTypes.STRING );
		// this is for recent versions
		queryEngine.getSqmFunctionRegistry().registerNamed( "str", StandardSpiBasicTypes.STRING );

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
				.append( String.join( ", ", foreignKey ) )
				.append( ") REFERENCES " )
				.append( referencedTable )
				.append( " (" )
				.append( String.join( ", ", primaryKey ) )
				.append( ") " )
				.toString();
	}

	/**
	 * Does this dialect support check constraints?
	 *
	 * @return {@code false} (Cache does not support check constraints)
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
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( new GlobalTempTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				final Identifier name = super.determineIdTableName( baseName );
				return name.getText().length() > 25
						? new Identifier( name.getText().substring( 1, 25 ), false )
						: name;
			}
		};
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return "identity";
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new Chache71IdentityColumnSupport();
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
		// InterSystems Cache' does not current support "SELECT ... FOR UPDATE" syntax...
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

		// This does not support the Cache SQL 'DISTINCT BY (comma-list)' extensions,
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
		// The keyword used to specify a nullable column.
		return " null";
	}

	@Override
	public JoinFragment createOuterJoinFragment() {
		// Create an OuterJoinGenerator for this dialect.
		return new CacheJoinFragment();
	}

	@Override
	public String getNoColumnsInsertString() {
		// The keyword used to insert a row without specifying
		// any column values
		return " default values";
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new CacheSQLExceptionConversionDelegate( this );
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	/**
	 * The Cache ViolatedConstraintNameExtracter.
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
}
