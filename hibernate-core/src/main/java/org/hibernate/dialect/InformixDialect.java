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
import org.hibernate.dialect.constraint.InformixUniqueKeyExporter;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Constraint;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;

/**
 * Informix dialect.<br>
 * <br>
 * Seems to work with Informix Dynamic Server Version 7.31.UD3,  Informix JDBC driver version 2.21JC3.
 *
 * @author Steve Molitor
 */
public class InformixDialect extends Dialect {
	
	private final InformixUniqueKeyExporter uniqueKeyExporter = new InformixUniqueKeyExporter( this );

	/**
	 * Creates new <code>InformixDialect</code> instance. Sets up the JDBC /
	 * Informix type mappings.
	 */
	public InformixDialect() {
		super();

		registerColumnType( Types.BIGINT, "int8" );
		registerColumnType( Types.BINARY, "byte" );
		// Informix doesn't have a bit type
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.CHAR, "char($l)" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "decimal" );
		registerColumnType( Types.DOUBLE, "float" );
		registerColumnType( Types.FLOAT, "smallfloat" );
		registerColumnType( Types.INTEGER, "integer" );
		// or BYTE
		registerColumnType( Types.LONGVARBINARY, "blob" );
		// or TEXT?
		registerColumnType( Types.LONGVARCHAR, "clob" );
		// or MONEY
		registerColumnType( Types.NUMERIC, "decimal" );
		registerColumnType( Types.REAL, "smallfloat" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TIMESTAMP, "datetime year to fraction(5)" );
		registerColumnType( Types.TIME, "datetime hour to second" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.VARBINARY, "byte" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.VARCHAR, 255, "varchar($l)" );
		registerColumnType( Types.VARCHAR, 32739, "lvarchar($l)" );

		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type)
			throws MappingException {
		return type == Types.BIGINT
				? "select dbinfo('serial8') from informix.systables where tabid=1"
				: "select dbinfo('sqlca.sqlerrd1') from informix.systables where tabid=1";
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		return type == Types.BIGINT ?
				"serial8 not null" :
				"serial not null";
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	/**
	 * Informix constraint name must be at the end.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final StringBuilder result = new StringBuilder( 30 )
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
	 * Informix constraint name must be at the end.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " add constraint primary key constraint " + constraintName + " ";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from informix.systables where tabid=1";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
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
	public String getQuerySequencesString() {
		return "select tabname from informix.systables where tabtype='Q'";
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new FirstLimitHandler(sql, selection);
    }

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		public String extractConstraintName(SQLException sqle) {
			String constraintName = null;
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );

			if ( errorCode == -268 ) {
				constraintName = extractUsingTemplate( "Unique constraint (", ") violated.", sqle.getMessage() );
			}
			else if ( errorCode == -691 ) {
				constraintName = extractUsingTemplate(
						"Missing key in referenced table for referential constraint (",
						").",
						sqle.getMessage()
				);
			}
			else if ( errorCode == -692 ) {
				constraintName = extractUsingTemplate(
						"Key value for constraint (",
						") is still being referenced.",
						sqle.getMessage()
				);
			}

			if ( constraintName != null ) {
				// strip table-owner because Informix always returns constraint names as "<table-owner>.<constraint-name>"
				final int i = constraintName.indexOf( '.' );
				if ( i != -1 ) {
					constraintName = constraintName.substring( i + 1 );
				}
			}

			return constraintName;
		}

	};

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
		return "select distinct current timestamp from informix.systables";
	}

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String getCreateTemporaryTableString() {
		return "create temp table";
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return "with no log";
	}
	
	@Override
	public Exporter<Constraint> getUniqueKeyExporter() {
		return uniqueKeyExporter;
	}
}
