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
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.MappingException;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.StandardBasicTypes;

/**
 * Informix dialect.<br>
 * <br>
 * Seems to work with Informix Dynamic Server Version 7.31.UD3,  Informix JDBC driver version 2.21JC3.
 *
 * @author Steve Molitor
 */
public class InformixDialect extends Dialect {

	/**
	 * Creates new <code>InformixDialect</code> instance. Sets up the JDBC /
	 * Informix type mappings.
	 */
	public InformixDialect() {
		super();

		registerColumnType(Types.BIGINT, "int8");
		registerColumnType(Types.BINARY, "byte");
		registerColumnType(Types.BIT, "smallint"); // Informix doesn't have a bit type
		registerColumnType(Types.CHAR, "char($l)");
		registerColumnType(Types.DATE, "date");
		registerColumnType(Types.DECIMAL, "decimal");
        registerColumnType(Types.DOUBLE, "float");
        registerColumnType(Types.FLOAT, "smallfloat");
		registerColumnType(Types.INTEGER, "integer");
		registerColumnType(Types.LONGVARBINARY, "blob"); // or BYTE
		registerColumnType(Types.LONGVARCHAR, "clob"); // or TEXT?
		registerColumnType(Types.NUMERIC, "decimal"); // or MONEY
		registerColumnType(Types.REAL, "smallfloat");
		registerColumnType(Types.SMALLINT, "smallint");
		registerColumnType(Types.TIMESTAMP, "datetime year to fraction(5)");
		registerColumnType(Types.TIME, "datetime hour to second");
		registerColumnType(Types.TINYINT, "smallint");
		registerColumnType(Types.VARBINARY, "byte");
		registerColumnType(Types.VARCHAR, "varchar($l)");
		registerColumnType(Types.VARCHAR, 255, "varchar($l)");
		registerColumnType(Types.VARCHAR, 32739, "lvarchar($l)");

		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
	}

	public String getAddColumnString() {
		return "add";
	}

	public boolean supportsIdentityColumns() {
		return true;
	}

	public String getIdentitySelectString(String table, String column, int type) 
	throws MappingException {
		return type==Types.BIGINT ?
			"select dbinfo('serial8') from informix.systables where tabid=1" :
			"select dbinfo('sqlca.sqlerrd1') from informix.systables where tabid=1";
	}

	public String getIdentityColumnString(int type) throws MappingException {
		return type==Types.BIGINT ?
			"serial8 not null" :
			"serial not null";
	}

	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	/**
	 * The syntax used to add a foreign key constraint to a table.
	 * Informix constraint name must be at the end.
	 * @return String
	 */
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		StringBuilder result = new StringBuilder( 30 )
				.append( " add constraint " )
				.append( " foreign key (" )
				.append( StringHelper.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			result.append( " (" )
					.append( StringHelper.join( ", ", primaryKey ) )
					.append( ')' );
		}

		result.append( " constraint " ).append( constraintName );

		return result.toString();
	}

	/**
	 * The syntax used to add a primary key constraint to a table.
	 * Informix constraint name must be at the end.
	 * @return String
	 */
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " add constraint primary key constraint " + constraintName + " ";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from informix.systables where tabid=1";
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	public boolean supportsSequences() {
		return true;
	}

	public boolean supportsPooledSequences() {
		return true;
	}

	public String getQuerySequencesString() {
		return "select tabname from informix.systables where tabtype='Q'";
	}

	public boolean supportsLimit() {
		return true;
	}

	public boolean useMaxForLimit() {
		return true;
	}

	public boolean supportsLimitOffset() {
		return false;
	}

	public String getLimitString(String querySelect, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return new StringBuilder( querySelect.length() + 8 )
				.append( querySelect )
				.insert( querySelect.toLowerCase().indexOf( "select" ) + 6, " first " + limit )
				.toString();
	}

	public boolean supportsVariableLimit() {
		return false;
	}

	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        return EXTRACTER;
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {

		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		public String extractConstraintName(SQLException sqle) {
			String constraintName = null;
			
			int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
			if ( errorCode == -268 ) {
				constraintName = extractUsingTemplate( "Unique constraint (", ") violated.", sqle.getMessage() );
			}
			else if ( errorCode == -691 ) {
				constraintName = extractUsingTemplate( "Missing key in referenced table for referential constraint (", ").", sqle.getMessage() );
			}
			else if ( errorCode == -692 ) {
				constraintName = extractUsingTemplate( "Key value for constraint (", ") is still being referenced.", sqle.getMessage() );
			}
			
			if (constraintName != null) {
				// strip table-owner because Informix always returns constraint names as "<table-owner>.<constraint-name>"
				int i = constraintName.indexOf('.');
				if (i != -1) {
					constraintName = constraintName.substring(i + 1);
				}
			}

			return constraintName;
		}

	};

	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	public String getCurrentTimestampSelectString() {
		return "select distinct current timestamp from informix.systables";
	}

	/**
	 * Overrides {@link Dialect#supportsTemporaryTables()} to return
	 * {@code true} when invoked.
	 *
	 * @return {@code true} when invoked
	 */
	public boolean supportsTemporaryTables() {
		return true;
	}

	/**
	 * Overrides {@link Dialect#getCreateTemporaryTableString()} to
	 * return "{@code create temp table}" when invoked.
	 *
	 * @return "{@code create temp table}" when invoked
	 */
	public String getCreateTemporaryTableString() {
		return "create temp table";
	}

	/**
	 * Overrides {@link Dialect#getCreateTemporaryTablePostfix()} to
	 * return "{@code with no log}" when invoked.
	 *
	 * @return "{@code with no log}" when invoked
	 */
	public String getCreateTemporaryTablePostfix() {
		return "with no log";
	}

}
