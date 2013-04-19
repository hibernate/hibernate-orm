/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.dialect;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.type.StandardBasicTypes;
import java.sql.SQLException;

/**
 * A dialect for the Teradata database created by MCR as part of the
 * dialect certification process.
 *
 * @author Jay Nance
 */
public class TeradataDialect extends Dialect {
	private static final int PARAM_LIST_SIZE_LIMIT = 1024;

	/**
	 * Constructor
	 */
	public TeradataDialect() {
		super();
		//registerColumnType data types
		registerColumnType( Types.NUMERIC, "NUMERIC($p,$s)" );
		registerColumnType( Types.DOUBLE, "DOUBLE PRECISION" );
		registerColumnType( Types.BIGINT, "BIGINT" );
		registerColumnType( Types.BIT, "BYTEINT" );
		registerColumnType( Types.TINYINT, "BYTEINT" );
		registerColumnType( Types.VARBINARY, "VARBYTE($l)" );
		registerColumnType( Types.BINARY, "VARBYTE(100)" );
		registerColumnType( Types.LONGVARBINARY, "VARBYTE(32000)" );
		registerColumnType( Types.LONGVARCHAR, "VARCHAR(32000)" );
		registerColumnType( Types.CHAR, "CHAR(1)" );
		registerColumnType( Types.DECIMAL, "DECIMAL" );
		registerColumnType( Types.INTEGER, "INTEGER" );
		registerColumnType( Types.SMALLINT, "SMALLINT" );
		registerColumnType( Types.FLOAT, "FLOAT" );
		registerColumnType( Types.VARCHAR, "VARCHAR($l)" );
		registerColumnType( Types.DATE, "DATE" );
		registerColumnType( Types.TIME, "TIME" );
		registerColumnType( Types.TIMESTAMP, "TIMESTAMP" );
		// hibernate seems to ignore this type...
		registerColumnType( Types.BOOLEAN, "BYTEINT" );
		registerColumnType( Types.BLOB, "BLOB" );
		registerColumnType( Types.CLOB, "CLOB" );

		registerFunction( "year", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "extract(year from ?1)" ) );
		registerFunction( "length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "character_length(?1)" ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "substring", new SQLFunctionTemplate( StandardBasicTypes.STRING, "substring(?1 from ?2 for ?3)" ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardBasicTypes.STRING, "position(?1 in ?2)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( StandardBasicTypes.STRING, "?1 mod ?2" ) );
		registerFunction( "str", new SQLFunctionTemplate( StandardBasicTypes.STRING, "cast(?1 as varchar(255))" ) );

		// bit_length feels a bit broken to me. We have to cast to char in order to
		// pass when a numeric value is supplied. But of course the answers given will
		// be wildly different for these two datatypes. 1234.5678 will be 9 bytes as
		// a char string but will be 8 or 16 bytes as a true numeric.
		// Jay Nance 2006-09-22
		registerFunction(
				"bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "octet_length(cast(?1 as char))*4" )
		);

		registerFunction( "current_timestamp", new SQLFunctionTemplate( StandardBasicTypes.TIMESTAMP, "current_timestamp" ) );
		registerFunction( "current_time", new SQLFunctionTemplate( StandardBasicTypes.TIME, "current_time" ) );
		registerFunction( "current_date", new SQLFunctionTemplate( StandardBasicTypes.DATE, "current_date" ) );

		registerKeyword( "password" );
		registerKeyword( "type" );
		registerKeyword( "title" );
		registerKeyword( "year" );
		registerKeyword( "month" );
		registerKeyword( "summary" );
		registerKeyword( "alias" );
		registerKeyword( "value" );
		registerKeyword( "first" );
		registerKeyword( "role" );
		registerKeyword( "account" );
		registerKeyword( "class" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE,DEFAULT_BATCH_SIZE );
	}

	/**
	 * Teradata does not support <tt>FOR UPDATE</tt> syntax
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "Add";
	}

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String getCreateTemporaryTableString() {
		return "create global temporary table";
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return " on commit preserve rows";
	}

	@Override
	public Boolean performTemporaryTableDDLInIsolation() {
		return Boolean.TRUE;
	}

	@Override
	public boolean dropTemporaryTableAfterUse() {
		return false;
	}

	/**
	 * Get the name of the database type associated with the given
	 * <tt>java.sql.Types</tt> typecode.
	 *
	 * @param code <tt>java.sql.Types</tt> typecode
	 * @param length the length or precision of the column
	 * @param precision the precision of the column
	 * @param scale the scale of the column
	 *
	 * @return the database type name
	 *
	 * @throws HibernateException
	 */
	 public String getTypeName(int code, int length, int precision, int scale) throws HibernateException {
		/*
		 * We might want a special case for 19,2. This is very common for money types
		 * and here it is converted to 18,1
		 */
		float f = precision > 0 ? ( float ) scale / ( float ) precision : 0;
		int p = ( precision > 38 ? 38 : precision );
		int s = ( precision > 38 ? ( int ) ( 38.0 * f ) : ( scale > 38 ? 38 : scale ) );
		return super.getTypeName( code, length, p, s );
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return false;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
		String v = "null";

		switch ( sqlType ) {
			case Types.BIT:
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.DECIMAL:
				v = "cast(null as decimal)";
				break;
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				v = "cast(null as varchar(255))";
				break;
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				v = "cast(null as timestamp)";
				break;
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.NULL:
			case Types.OTHER:
			case Types.JAVA_OBJECT:
			case Types.DISTINCT:
			case Types.STRUCT:
			case Types.ARRAY:
			case Types.BLOB:
			case Types.CLOB:
			case Types.REF:
			case Types.DATALINK:
			case Types.BOOLEAN:
				break;
			default:
				break;
		}
		return v;
	}

	@Override
	public String getCreateMultisetTableString() {
		return "create multiset table ";
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}


	@Override
	public boolean supportsBindAsCallableArgument() {
		return false;
	}


	/* (non-Javadoc)
		 * @see org.hibernate.dialect.Dialect#getInExpressionCountLimit()
		 */

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
    public boolean constraintRequiresFunction() {
        return true;
    }

	@Override
	public String[] generateConstraintFunctionSql(String alter, String tableName, String constraintName) {
		String[] results = new String[2];
		if (tableName.length() > 28) {
			tableName = tableName.substring(0, 28);
		}
		results[0] =
				"REPLACE PROCEDURE dropConstraintTable " +
						"(IN alterCmd VARCHAR(300), IN tblName VARCHAR(300), IN constraintName VARCHAR(50)) " +
						"BEGIN " +
						"DECLARE lclRes VARCHAR(300); " +
						"DECLARE SqlStr VARCHAR(300); " +
						"DECLARE C1 CURSOR FOR S1; " +

						"CALL DBC.SYSEXECSQL( alterCmd);" +
						"CALL DBC.SYSEXECSQL( 'commit work');" +

						"SET SqlStr = 'SELECT trim(IndexID) as t1 from dbc.RI_Distinct_ChildrenV where IndexName=?';" +
						"PREPARE S1 FROM SqlStr;" +
						"OPEN C1 USING constraintName;" +

						"FETCH C1 into lclRes;" +
						"Set SQLStr = 'DROP Table ' || tblname || '_' || lclRes;" +
						"CALL DBC.SYSEXECSQL( SQLStr);" +
						"CLOSE C1;" +
						"END;";

		results[1] = "{CALL dropConstraintTable(" +
				"'" + alter + "' ," +
				"'" + tableName + "' ," +
				"'" + constraintName + "' " +
				")}";
		return results;
	}

	@Override
	public String getIdentityColumnString() {
		return "generated by default as identity not null";
	}

	@Override
	public String getIdentityInsertString() {
		return "null";
	}

	@Override
	public String getDropTemporaryTableString() {
		return "drop temporary table";
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}


	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}


	@Override
	public boolean doesIndexNameRequireColumnNames() {
		return true;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}


	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;

	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter(col, Types.REF);
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement cs) throws SQLException {
		boolean isResultSet = cs.execute();
		while (!isResultSet && cs.getUpdateCount() != -1) {
			isResultSet = cs.getMoreResults();
		}
		return cs.getResultSet();
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		@Override
		public String extractConstraintName(SQLException sqle) {
			String constraintName = null;

			int errorCode = sqle.getErrorCode();
			if (errorCode == 27003) {
				constraintName = extractUsingTemplate("Unique constraint (", ") violated.", sqle.getMessage());
			} else if (errorCode == 2700) {
				constraintName = extractUsingTemplate("Referential constraint", "violation:", sqle.getMessage());
			} else if (errorCode == 5317) {
				constraintName = extractUsingTemplate("Check constraint (", ") violated.", sqle.getMessage());
			}

			if (constraintName != null) {
				int i = constraintName.indexOf('.');
				if (i != -1) {
					constraintName = constraintName.substring(i + 1);
				}
			}
			return constraintName;
		}
	};
}

