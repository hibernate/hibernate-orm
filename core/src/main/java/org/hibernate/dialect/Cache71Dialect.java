/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.NvlFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.function.StandardJDBCEscapeFunction;
import org.hibernate.dialect.function.ConvertFunction;
import org.hibernate.dialect.function.ConditionalParenthesisFunction;
import org.hibernate.dialect.lock.*;
import org.hibernate.exception.CacheSQLStateConverter;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.CacheJoinFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.util.StringHelper;

/**
 * Cach&eacute; 2007.1 dialect. This class is required in order to use Hibernate with Intersystems Cach� SQL.<br>
 * <br>
 * Compatible with Cach� 2007.1.
 * <br>
 * <head>
 * <title>Cach&eacute; and Hibernate</title>
 * </head>
 * <body>
 * <h1>Cach&eacute; and Hibernate</h1>
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
 * <dl>
 * <dt><b>Note 1</b></dt>
 * <dd>Please contact your administrator for the userid and password you should use when attempting access via JDBC.
 * By default, these are chosen to be "_SYSTEM" and "SYS" respectively as noted in the SQL standard.</dd>
 * </dl>
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

	/**
	 * Creates new <code>Cach�71Dialect</code> instance. Sets up the JDBC /
	 * Cach� type mappings.
	 */
	public Cache71Dialect() {
		super();
		commonRegistration();
		register71Functions();
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
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );	// binary %Stream
		registerColumnType( Types.LONGVARCHAR, "longvarchar" );		// character %Stream
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.REAL, "real" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TINYINT, "tinyint" );
		// TBD should this be varbinary($1)?
		//		registerColumnType(Types.VARBINARY,     "binary($1)");
		registerColumnType( Types.VARBINARY, "longvarbinary" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.BLOB, "longvarbinary" );
		registerColumnType( Types.CLOB, "longvarchar" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "false" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		//getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, NO_BATCH);

		getDefaultProperties().setProperty( Environment.USE_SQL_COMMENTS, "false" );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "acos", new StandardJDBCEscapeFunction( "acos", Hibernate.DOUBLE ) );
		registerFunction( "%alphaup", new StandardSQLFunction( "%alphaup", Hibernate.STRING ) );
		registerFunction( "ascii", new StandardSQLFunction( "ascii", Hibernate.STRING ) );
		registerFunction( "asin", new StandardJDBCEscapeFunction( "asin", Hibernate.DOUBLE ) );
		registerFunction( "atan", new StandardJDBCEscapeFunction( "atan", Hibernate.DOUBLE ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( Hibernate.INTEGER, "($length(?1)*8)" ) );
		// hibernate impelemnts cast in Dialect.java
		registerFunction( "ceiling", new StandardSQLFunction( "ceiling", Hibernate.INTEGER ) );
		registerFunction( "char", new StandardJDBCEscapeFunction( "char", Hibernate.CHARACTER ) );
		registerFunction( "character_length", new StandardSQLFunction( "character_length", Hibernate.INTEGER ) );
		registerFunction( "char_length", new StandardSQLFunction( "char_length", Hibernate.INTEGER ) );
		registerFunction( "cos", new StandardJDBCEscapeFunction( "cos", Hibernate.DOUBLE ) );
		registerFunction( "cot", new StandardJDBCEscapeFunction( "cot", Hibernate.DOUBLE ) );
		registerFunction( "coalesce", new VarArgsSQLFunction( "coalesce(", ",", ")" ) );
		registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "", "||", "" ) );
		registerFunction( "convert", new ConvertFunction() );
		registerFunction( "curdate", new StandardJDBCEscapeFunction( "curdate", Hibernate.DATE ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", Hibernate.DATE, false ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", Hibernate.TIME, false ) );
		registerFunction(
				"current_timestamp", new ConditionalParenthesisFunction( "current_timestamp", Hibernate.TIMESTAMP )
		);
		registerFunction( "curtime", new StandardJDBCEscapeFunction( "curtime", Hibernate.TIME ) );
		registerFunction( "database", new StandardJDBCEscapeFunction( "database", Hibernate.STRING ) );
		registerFunction( "dateadd", new VarArgsSQLFunction( Hibernate.TIMESTAMP, "dateadd(", ",", ")" ) );
		registerFunction( "datediff", new VarArgsSQLFunction( Hibernate.INTEGER, "datediff(", ",", ")" ) );
		registerFunction( "datename", new VarArgsSQLFunction( Hibernate.STRING, "datename(", ",", ")" ) );
		registerFunction( "datepart", new VarArgsSQLFunction( Hibernate.INTEGER, "datepart(", ",", ")" ) );
		registerFunction( "day", new StandardSQLFunction( "day", Hibernate.INTEGER ) );
		registerFunction( "dayname", new StandardJDBCEscapeFunction( "dayname", Hibernate.STRING ) );
		registerFunction( "dayofmonth", new StandardJDBCEscapeFunction( "dayofmonth", Hibernate.INTEGER ) );
		registerFunction( "dayofweek", new StandardJDBCEscapeFunction( "dayofweek", Hibernate.INTEGER ) );
		registerFunction( "dayofyear", new StandardJDBCEscapeFunction( "dayofyear", Hibernate.INTEGER ) );
		// is it necessary to register %exact since it can only appear in a where clause?
		registerFunction( "%exact", new StandardSQLFunction( "%exact", Hibernate.STRING ) );
		registerFunction( "exp", new StandardJDBCEscapeFunction( "exp", Hibernate.DOUBLE ) );
		registerFunction( "%external", new StandardSQLFunction( "%external", Hibernate.STRING ) );
		registerFunction( "$extract", new VarArgsSQLFunction( Hibernate.INTEGER, "$extract(", ",", ")" ) );
		registerFunction( "$find", new VarArgsSQLFunction( Hibernate.INTEGER, "$find(", ",", ")" ) );
		registerFunction( "floor", new StandardSQLFunction( "floor", Hibernate.INTEGER ) );
		registerFunction( "getdate", new StandardSQLFunction( "getdate", Hibernate.TIMESTAMP ) );
		registerFunction( "hour", new StandardJDBCEscapeFunction( "hour", Hibernate.INTEGER ) );
		registerFunction( "ifnull", new VarArgsSQLFunction( "ifnull(", ",", ")" ) );
		registerFunction( "%internal", new StandardSQLFunction( "%internal" ) );
		registerFunction( "isnull", new VarArgsSQLFunction( "isnull(", ",", ")" ) );
		registerFunction( "isnumeric", new StandardSQLFunction( "isnumeric", Hibernate.INTEGER ) );
		registerFunction( "lcase", new StandardJDBCEscapeFunction( "lcase", Hibernate.STRING ) );
		registerFunction( "left", new StandardJDBCEscapeFunction( "left", Hibernate.STRING ) );
		registerFunction( "len", new StandardSQLFunction( "len", Hibernate.INTEGER ) );
		registerFunction( "$length", new VarArgsSQLFunction( "$length(", ",", ")" ) );
		// aggregate functions shouldn't be registered, right?
		//registerFunction( "list", new StandardSQLFunction("list",Hibernate.STRING) );
		// stopped on $list
		registerFunction( "$list", new VarArgsSQLFunction( "$list(", ",", ")" ) );
		registerFunction( "$listdata", new VarArgsSQLFunction( "$listdata(", ",", ")" ) );
		registerFunction( "$listfind", new VarArgsSQLFunction( "$listfind(", ",", ")" ) );
		registerFunction( "$listget", new VarArgsSQLFunction( "$listget(", ",", ")" ) );
		registerFunction( "$listlength", new StandardSQLFunction( "$listlength", Hibernate.INTEGER ) );
		registerFunction( "locate", new StandardSQLFunction( "$FIND", Hibernate.INTEGER ) );
		registerFunction( "log", new StandardJDBCEscapeFunction( "log", Hibernate.DOUBLE ) );
		registerFunction( "log10", new StandardJDBCEscapeFunction( "log", Hibernate.DOUBLE ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "minute", new StandardJDBCEscapeFunction( "minute", Hibernate.INTEGER ) );
		registerFunction( "mod", new StandardJDBCEscapeFunction( "mod", Hibernate.DOUBLE ) );
		registerFunction( "month", new StandardJDBCEscapeFunction( "month", Hibernate.INTEGER ) );
		registerFunction( "monthname", new StandardJDBCEscapeFunction( "monthname", Hibernate.STRING ) );
		registerFunction( "now", new StandardJDBCEscapeFunction( "monthname", Hibernate.TIMESTAMP ) );
		registerFunction( "nullif", new VarArgsSQLFunction( "nullif(", ",", ")" ) );
		registerFunction( "nvl", new NvlFunction() );
		registerFunction( "%odbcin", new StandardSQLFunction( "%odbcin" ) );
		registerFunction( "%odbcout", new StandardSQLFunction( "%odbcin" ) );
		registerFunction( "%pattern", new VarArgsSQLFunction( Hibernate.STRING, "", "%pattern", "" ) );
		registerFunction( "pi", new StandardJDBCEscapeFunction( "pi", Hibernate.DOUBLE ) );
		registerFunction( "$piece", new VarArgsSQLFunction( Hibernate.STRING, "$piece(", ",", ")" ) );
		registerFunction( "position", new VarArgsSQLFunction( Hibernate.INTEGER, "position(", " in ", ")" ) );
		registerFunction( "power", new VarArgsSQLFunction( Hibernate.STRING, "power(", ",", ")" ) );
		registerFunction( "quarter", new StandardJDBCEscapeFunction( "quarter", Hibernate.INTEGER ) );
		registerFunction( "repeat", new VarArgsSQLFunction( Hibernate.STRING, "repeat(", ",", ")" ) );
		registerFunction( "replicate", new VarArgsSQLFunction( Hibernate.STRING, "replicate(", ",", ")" ) );
		registerFunction( "right", new StandardJDBCEscapeFunction( "right", Hibernate.STRING ) );
		registerFunction( "round", new VarArgsSQLFunction( Hibernate.FLOAT, "round(", ",", ")" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", Hibernate.STRING ) );
		registerFunction( "second", new StandardJDBCEscapeFunction( "second", Hibernate.INTEGER ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", Hibernate.INTEGER ) );
		registerFunction( "sin", new StandardJDBCEscapeFunction( "sin", Hibernate.DOUBLE ) );
		registerFunction( "space", new StandardSQLFunction( "space", Hibernate.STRING ) );
		registerFunction( "%sqlstring", new VarArgsSQLFunction( Hibernate.STRING, "%sqlstring(", ",", ")" ) );
		registerFunction( "%sqlupper", new VarArgsSQLFunction( Hibernate.STRING, "%sqlupper(", ",", ")" ) );
		registerFunction( "sqrt", new StandardJDBCEscapeFunction( "SQRT", Hibernate.DOUBLE ) );
		registerFunction( "%startswith", new VarArgsSQLFunction( Hibernate.STRING, "", "%startswith", "" ) );
		// below is for Cache' that don't have str in 2007.1 there is str and we register str directly
		registerFunction( "str", new SQLFunctionTemplate( Hibernate.STRING, "cast(?1 as char varying)" ) );
		registerFunction( "string", new VarArgsSQLFunction( Hibernate.STRING, "string(", ",", ")" ) );
		// note that %string is deprecated
		registerFunction( "%string", new VarArgsSQLFunction( Hibernate.STRING, "%string(", ",", ")" ) );
		registerFunction( "substr", new VarArgsSQLFunction( Hibernate.STRING, "substr(", ",", ")" ) );
		registerFunction( "substring", new VarArgsSQLFunction( Hibernate.STRING, "substring(", ",", ")" ) );
		registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", Hibernate.TIMESTAMP, false ) );
		registerFunction( "tan", new StandardJDBCEscapeFunction( "tan", Hibernate.DOUBLE ) );
		registerFunction( "timestampadd", new StandardJDBCEscapeFunction( "timestampadd", Hibernate.DOUBLE ) );
		registerFunction( "timestampdiff", new StandardJDBCEscapeFunction( "timestampdiff", Hibernate.DOUBLE ) );
		registerFunction( "tochar", new VarArgsSQLFunction( Hibernate.STRING, "tochar(", ",", ")" ) );
		registerFunction( "to_char", new VarArgsSQLFunction( Hibernate.STRING, "to_char(", ",", ")" ) );
		registerFunction( "todate", new VarArgsSQLFunction( Hibernate.STRING, "todate(", ",", ")" ) );
		registerFunction( "to_date", new VarArgsSQLFunction( Hibernate.STRING, "todate(", ",", ")" ) );
		registerFunction( "tonumber", new StandardSQLFunction( "tonumber" ) );
		registerFunction( "to_number", new StandardSQLFunction( "tonumber" ) );
		// TRIM(end_keyword string-expression-1 FROM string-expression-2)
		// use Hibernate implementation "From" is one of the parameters they pass in position ?3
		//registerFunction( "trim", new SQLFunctionTemplate(Hibernate.STRING, "trim(?1 ?2 from ?3)") );
		registerFunction( "truncate", new StandardJDBCEscapeFunction( "truncate", Hibernate.STRING ) );
		registerFunction( "ucase", new StandardJDBCEscapeFunction( "ucase", Hibernate.STRING ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		// %upper is deprecated
		registerFunction( "%upper", new StandardSQLFunction( "%upper" ) );
		registerFunction( "user", new StandardJDBCEscapeFunction( "user", Hibernate.STRING ) );
		registerFunction( "week", new StandardJDBCEscapeFunction( "user", Hibernate.INTEGER ) );
		registerFunction( "xmlconcat", new VarArgsSQLFunction( Hibernate.STRING, "xmlconcat(", ",", ")" ) );
		registerFunction( "xmlelement", new VarArgsSQLFunction( Hibernate.STRING, "xmlelement(", ",", ")" ) );
		// xmlforest requires a new kind of function constructor
		registerFunction( "year", new StandardJDBCEscapeFunction( "year", Hibernate.INTEGER ) );
	}

	protected final void register71Functions() {
		this.registerFunction( "str", new VarArgsSQLFunction( Hibernate.STRING, "str(", ",", ")" ) );
	}

	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasAlterTable() {
		// Does this dialect support the ALTER TABLE syntax?
		return true;
	}

	public boolean qualifyIndexName() {
		// Do we need to qualify index names with the schema name?
		return false;
	}

	public boolean supportsUnique() {
		// Does this dialect support the UNIQUE column syntax?
		return true;
	}

	/**
	 * The syntax used to add a foreign key constraint to a table.
	 *
	 * @return String
	 */
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		// The syntax used to add a foreign key constraint to a table.
		return new StringBuffer( 300 )
				.append( " ADD CONSTRAINT " )
				.append( constraintName )
				.append( " FOREIGN KEY " )
				.append( constraintName )
				.append( " (" )
				.append( StringHelper.join( ", ", foreignKey ) )	// identifier-commalist
				.append( ") REFERENCES " )
				.append( referencedTable )
				.append( " (" )
				.append( StringHelper.join( ", ", primaryKey ) ) // identifier-commalist
				.append( ") " )
				.toString();
	}

	public boolean supportsCheck() {
		// Does this dialect support check constraints?
		return false;
	}

	public String getAddColumnString() {
		// The syntax used to add a column to a table
		return " add column";
	}

	public String getCascadeConstraintsString() {
		// Completely optional cascading drop clause.
		return "";
	}

	public boolean dropConstraints() {
		// Do we need to drop constraints before dropping tables in this dialect?
		return true;
	}

	public boolean supportsCascadeDelete() {
		return true;
	}

	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}

	// temporary table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsTemporaryTables() {
		return true;
	}

	public String generateTemporaryTableName(String baseTableName) {
		String name = super.generateTemporaryTableName( baseTableName );
		return name.length() > 25 ? name.substring( 1, 25 ) : name;
	}

	public String getCreateTemporaryTableString() {
		return "create global temporary table";
	}

	public Boolean performTemporaryTableDDLInIsolation() {
		return Boolean.FALSE;
	}

	public String getCreateTemporaryTablePostfix() {
		return "";
	}

	public boolean dropTemporaryTableAfterUse() {
		return true;
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsIdentityColumns() {
		return true;
	}

	public Class getNativeIdentifierGeneratorClass() {
		return IdentityGenerator.class;
	}

	public boolean hasDataTypeInIdentityColumn() {
		// Whether this dialect has an Identity clause added to the data type or a completely seperate identity
		// data type
		return true;
	}

	public String getIdentityColumnString() throws MappingException {
		// The keyword used to specify an identity column, if identity column key generation is supported.
		return "identity";
	}

	public String getIdentitySelectString() {
		return "SELECT LAST_IDENTITY() FROM %TSQL_sys.snf";
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

	public boolean supportsForUpdate() {
		// Does this dialect support the FOR UPDATE syntax?
		return false;
	}

	public boolean supportsForUpdateOf() {
		// Does this dialect support FOR UPDATE OF, allowing particular rows to be locked?
		return false;
	}

	public boolean supportsForUpdateNowait() {
		// Does this dialect support the Oracle-style FOR UPDATE NOWAIT syntax?
		return false;
	}

	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

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

	public boolean supportsLimit() {
		return true;
	}

	public boolean supportsLimitOffset() {
		return false;
	}

	public boolean supportsVariableLimit() {
		return true;
	}

	public boolean bindLimitParametersFirst() {
		// Does the LIMIT clause come at the start of the SELECT statement, rather than at the end?
		return true;
	}

	public boolean useMaxForLimit() {
		// Does the LIMIT clause take a "maximum" row number instead of a total number of returned rows?
		return true;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		if ( hasOffset ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}

		// This does not support the Cache SQL 'DISTINCT BY (comma-list)' extensions,
		// but this extension is not supported through Hibernate anyway.
		int insertionPoint = sql.startsWith( "select distinct" ) ? 15 : 6;

		return new StringBuffer( sql.length() + 8 )
				.append( sql )
				.insert( insertionPoint, " TOP ? " )
				.toString();
	}

	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return ( ResultSet ) ps.getObject( 1 );
	}

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public String getLowercaseFunction() {
		// The name of the SQL function that transforms a string to lowercase
		return "lower";
	}

	public String getNullColumnString() {
		// The keyword used to specify a nullable column.
		return " null";
	}

	public JoinFragment createOuterJoinFragment() {
		// Create an OuterJoinGenerator for this dialect.
		return new CacheJoinFragment();
	}

	public String getNoColumnsInsertString() {
		// The keyword used to insert a row without specifying
		// any column values
		return " default values";
	}

	public SQLExceptionConverter buildSQLExceptionConverter() {
		return new CacheSQLStateConverter( EXTRACTER );
	}

	public static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		public String extractConstraintName(SQLException sqle) {
			return extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() );
		}
	};


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsEmptyInList() {
		return false;
	}

	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}
}
