/*
 * Created on Aug 24, 2005
 * This is the Hibernate dialect for the Unisys 2200 Relational Database (RDMS).
 * This dialect was developed for use with Hibernate 3.0.5. Other versions may
 * require modifications to the dialect.
 *
 * Version History:
 * Also change the version displayed below in the constructor
 * 1.1
 * 1.0  2005-10-24  CDH - First dated version for use with CP 11
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;

import java.sql.Types;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ploski and Hanson
 */
public class RDMSOS2200Dialect extends Dialect {
	private static Log log = LogFactory.getLog(RDMSOS2200Dialect.class);

	public RDMSOS2200Dialect() {
		super();
        // Display the dialect version.
		log.info("RDMSOS2200Dialect version: 1.0");

        /**
         * This section registers RDMS Biult-in Functions (BIFs) with Hibernate.
         * The first parameter is the 'register' function name with Hibernate.
         * The second parameter is the defined RDMS SQL Function and it's
         * characteristics. If StandardSQLFunction(...) is used, the RDMS BIF
         * name and the return type (if any) is specified.  If
         * SQLFunctionTemplate(...) is used, the return type and a template
         * string is provided, plus an optional hasParenthesesIfNoArgs flag.
         */
		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction("ascii", new StandardSQLFunction("ascii", Hibernate.INTEGER) );
		registerFunction("char_length", new StandardSQLFunction("char_length", Hibernate.INTEGER) );
		registerFunction("character_length", new StandardSQLFunction("character_length", Hibernate.INTEGER) );
		registerFunction("length", new StandardSQLFunction("length", Hibernate.INTEGER) );

		// The RDMS concat() function only supports 2 parameters
		registerFunction( "concat", new SQLFunctionTemplate(Hibernate.STRING, "concat(?1, ?2)") );
		registerFunction( "instr", new StandardSQLFunction("instr", Hibernate.STRING) );
		registerFunction( "lpad", new StandardSQLFunction("lpad", Hibernate.STRING) );
		registerFunction( "replace", new StandardSQLFunction("replace", Hibernate.STRING) );
		registerFunction( "rpad", new StandardSQLFunction("rpad", Hibernate.STRING) );
		registerFunction( "substr", new StandardSQLFunction("substr", Hibernate.STRING) );

		registerFunction("lcase", new StandardSQLFunction("lcase") );
		registerFunction("lower", new StandardSQLFunction("lower") );
		registerFunction("ltrim", new StandardSQLFunction("ltrim") );
		registerFunction("reverse", new StandardSQLFunction("reverse") );
		registerFunction("rtrim", new StandardSQLFunction("rtrim") );

		// RDMS does not directly support the trim() function, we use rtrim() and ltrim()
		registerFunction("trim", new SQLFunctionTemplate(Hibernate.INTEGER, "ltrim(rtrim(?1))" ) );
		registerFunction("soundex", new StandardSQLFunction("soundex") );
		registerFunction("space", new StandardSQLFunction("space", Hibernate.STRING) );
		registerFunction("ucase", new StandardSQLFunction("ucase") );
		registerFunction("upper", new StandardSQLFunction("upper") );

		registerFunction("acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction("asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction("atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction("cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction("cosh", new StandardSQLFunction("cosh", Hibernate.DOUBLE) );
		registerFunction("cot", new StandardSQLFunction("cot", Hibernate.DOUBLE) );
		registerFunction("exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction("ln", new StandardSQLFunction("ln", Hibernate.DOUBLE) );
		registerFunction("log", new StandardSQLFunction("log", Hibernate.DOUBLE) );
		registerFunction("log10", new StandardSQLFunction("log10", Hibernate.DOUBLE) );
		registerFunction("pi", new NoArgSQLFunction("pi", Hibernate.DOUBLE) );
		registerFunction("rand", new NoArgSQLFunction("rand", Hibernate.DOUBLE) );
		registerFunction("sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction("sinh", new StandardSQLFunction("sinh", Hibernate.DOUBLE) );
		registerFunction("sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction("tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );
		registerFunction("tanh", new StandardSQLFunction("tanh", Hibernate.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "trunc", new StandardSQLFunction("trunc") );
		registerFunction( "ceil", new StandardSQLFunction("ceil") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "chr", new StandardSQLFunction("chr", Hibernate.CHARACTER) );
		registerFunction( "initcap", new StandardSQLFunction("initcap") );

		registerFunction( "user", new NoArgSQLFunction("user", Hibernate.STRING, false) );

		registerFunction( "current_date", new NoArgSQLFunction("current_date", Hibernate.DATE, false) );
		registerFunction( "current_time", new NoArgSQLFunction("current_timestamp", Hibernate.TIME, false) );
		registerFunction( "current_timestamp", new NoArgSQLFunction("current_timestamp", Hibernate.TIMESTAMP, false) );
		registerFunction("curdate", new NoArgSQLFunction("curdate",Hibernate.DATE) );
		registerFunction("curtime", new NoArgSQLFunction("curtime",Hibernate.TIME) );
		registerFunction("days", new StandardSQLFunction("days",Hibernate.INTEGER) );
		registerFunction("dayofmonth", new StandardSQLFunction("dayofmonth",Hibernate.INTEGER) );
		registerFunction("dayname", new StandardSQLFunction("dayname",Hibernate.STRING) );
		registerFunction("dayofweek", new StandardSQLFunction("dayofweek",Hibernate.INTEGER) );
		registerFunction("dayofyear", new StandardSQLFunction("dayofyear",Hibernate.INTEGER) );
		registerFunction("hour", new StandardSQLFunction("hour",Hibernate.INTEGER) );
		registerFunction("last_day", new StandardSQLFunction("last_day",Hibernate.DATE) );
		registerFunction("microsecond", new StandardSQLFunction("microsecond",Hibernate.INTEGER) );
		registerFunction("minute", new StandardSQLFunction("minute",Hibernate.INTEGER) );
		registerFunction("month", new StandardSQLFunction("month",Hibernate.INTEGER) );
		registerFunction("monthname", new StandardSQLFunction("monthname",Hibernate.STRING) );
		registerFunction("now", new NoArgSQLFunction("now",Hibernate.TIMESTAMP) );
		registerFunction("quarter", new StandardSQLFunction("quarter",Hibernate.INTEGER) );
		registerFunction("second", new StandardSQLFunction("second",Hibernate.INTEGER) );
		registerFunction("time", new StandardSQLFunction("time",Hibernate.TIME) );
		registerFunction("timestamp", new StandardSQLFunction("timestamp",Hibernate.TIMESTAMP) );
		registerFunction("week", new StandardSQLFunction("week",Hibernate.INTEGER) );
		registerFunction("year", new StandardSQLFunction("year",Hibernate.INTEGER) );

		registerFunction("atan2", new StandardSQLFunction("atan2",Hibernate.DOUBLE) );
		registerFunction( "mod", new StandardSQLFunction("mod",Hibernate.INTEGER) );
		registerFunction( "nvl", new StandardSQLFunction("nvl") );
		registerFunction( "power", new StandardSQLFunction("power", Hibernate.DOUBLE) );

		/**
		 * For a list of column types to register, see section A-1
		 * in 7862 7395, the Unisys JDBC manual.
		 *
		 * Here are column sizes as documented in Table A-1 of
		 * 7831 0760, "Enterprise Relational Database Server
		 * for ClearPath OS2200 Administration Guide"
		 * Numeric - 21
		 * Decimal - 22 (21 digits plus one for sign)
		 * Float   - 60 bits
		 * Char    - 28000
		 * NChar   - 14000
		 * BLOB+   - 4294967296 (4 Gb)
		 * + RDMS JDBC driver does not support BLOBs
		 *
		 * DATE, TIME and TIMESTAMP literal formats are
		 * are all described in section 2.3.4 DATE Literal Format
		 * in 7830 8160.
		 * The DATE literal format is: YYYY-MM-DD
		 * The TIME literal format is: HH:MM:SS[.[FFFFFF]]
		 * The TIMESTAMP literal format is: YYYY-MM-DD HH:MM:SS[.[FFFFFF]]
		 *
		 * Note that $l (dollar-L) will use the length value if provided.
		 * Also new for Hibernate3 is the $p percision and $s (scale) parameters
		 */
		registerColumnType(Types.BIT, "SMALLINT");
		registerColumnType(Types.TINYINT, "SMALLINT");
		registerColumnType(Types.BIGINT, "NUMERIC(21,0)");
		registerColumnType(Types.SMALLINT, "SMALLINT");
		registerColumnType(Types.CHAR, "CHARACTER(1)");
		registerColumnType(Types.DOUBLE, "DOUBLE PRECISION");
		registerColumnType(Types.FLOAT, "FLOAT");
		registerColumnType(Types.REAL, "REAL");
		registerColumnType(Types.INTEGER, "INTEGER");
		registerColumnType(Types.NUMERIC, "NUMERIC(21,$l)");
		registerColumnType(Types.DECIMAL, "NUMERIC(21,$l)");
		registerColumnType(Types.DATE, "DATE");
		registerColumnType(Types.TIME, "TIME");
		registerColumnType(Types.TIMESTAMP, "TIMESTAMP");
		registerColumnType(Types.VARCHAR, "CHARACTER($l)");
        registerColumnType(Types.BLOB, "BLOB($l)" );
        /*
         * The following types are not supported in RDMS/JDBC and therefore commented out.
         * However, in some cases, mapping them to CHARACTER columns works
         * for many applications, but does not work for all cases.
         */
        // registerColumnType(Types.VARBINARY, "CHARACTER($l)");
        // registerColumnType(Types.BLOB, "CHARACTER($l)" );  // For use prior to CP 11.0
        // registerColumnType(Types.CLOB, "CHARACTER($l)" );
	}


	// Dialect method overrides ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * RDMS does not support qualifing index names with the schema name.
     */
	public boolean qualifyIndexName() {
		return false;
	}

	/**
	 * The RDMS DB supports the 'FOR UPDATE OF' clause. However, the RDMS-JDBC
     * driver does not support this feature, so a false is return.
     * The base dialect also returns a false, but we will leave this over-ride
     * in to make sure it stays false.
	 */
	public boolean forUpdateOfColumns() {
		return false;
	}

	/**
	 * Since the RDMS-JDBC driver does not support for updates, this string is
     * set to an empty string. Whenever, the driver does support this feature,
     * the returned string should be " FOR UPDATE OF". Note that RDMS does not
     * support the string 'FOR UPDATE' string.
	 */
	public String getForUpdateString() {
		return ""; // Original Dialect.java returns " for update";
	}

    /**
     * RDMS does not support adding Unique constraints via create and alter table.
     */
	public boolean supportsUniqueConstraintInCreateAlterTable() {
	    return true;
	}

	// Verify the state of this new method in Hibernate 3.0 Dialect.java
    /**
     * RDMS does not support Cascade Deletes.
     * Need to review this in the future when support is provided.
     */
	public boolean supportsCascadeDelete() {
		return false; // Origial Dialect.java returns true;
	}

	/**
     * Currently, RDMS-JDBC does not support ForUpdate.
     * Need to review this in the future when support is provided.
	 */
    public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	public String getAddColumnString() {
		return "add";
	}

	public String getNullColumnString() {
		// The keyword used to specify a nullable column.
		return " null";
	}

    // *** Sequence methods - start. The RDMS dialect needs these

    // methods to make it possible to use the Native Id generator

	public boolean supportsSequences() {
		return true;
	}

	public String getSequenceNextValString(String sequenceName) {
	    // The where clause was added to eliminate this statement from Brute Force Searches.
        return  "select permuted_id('NEXT',31) from rdms.rdms_dummy where key_col = 1 ";
	}

	public String getCreateSequenceString(String sequenceName) {
        // We must return a valid RDMS/RSA command from this method to
        // prevent RDMS/RSA from issuing *ERROR 400
        return "";
	}

	public String getDropSequenceString(String sequenceName) {
        // We must return a valid RDMS/RSA command from this method to
        // prevent RDMS/RSA from issuing *ERROR 400
        return "";
	}

	// *** Sequence methods - end

    public String getCascadeConstraintsString() {
        // Used with DROP TABLE to delete all records in the table.
        return " including contents";
    }

	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	public boolean supportsLimit() {
		return true;
	}

	public boolean supportsLimitOffset() {
		return false;
	}

    public String getLimitString(String sql, int offset, int limit) {
        if (offset>0) throw new UnsupportedOperationException("RDMS does not support paged queries");
		return new StringBuffer(sql.length() + 40)
			.append(sql)
			.append(" fetch first ")
			.append(limit)
			.append(" rows only ")
			.toString();
	}

	public boolean supportsVariableLimit() {
		return false;
	}

	public boolean supportsUnionAll() {
		// RDMS supports the UNION ALL clause.
          return true;
	}

	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// RDMS has no known variation of a "SELECT ... FOR UPDATE" syntax...
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}
}
