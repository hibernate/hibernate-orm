/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.MappingException;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.unique.InformixUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.StandardBasicTypes;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/**
 * Informix dialect.<br>
 * <br>
 * Seems to work with Informix Dynamic Server Version 7.31.UD3,  Informix JDBC driver version 2.21JC3.
 *
 * @author Steve Molitor
 */
public class InformixDialect extends Dialect {
	
	private final UniqueDelegate uniqueDelegate;

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
		
		uniqueDelegate = new InformixUniqueDelegate( this );
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
	public LimitHandler getLimitHandler() {
		return FirstLimitHandler.INSTANCE;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public String getLimitString(String querySelect, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return new StringBuilder( querySelect.length() + 8 )
				.append( querySelect )
				.insert( querySelect.toLowerCase(Locale.ROOT).indexOf( "select" ) + 6, " first " + limit )
				.toString();
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
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
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new LocalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String getCreateIdTableCommand() {
						return "create temp table";
					}

					@Override
					public String getCreateIdTableStatementOptions() {
						return "with no log";
					}
				},
				AfterUseAction.CLEAN,
				null
		);
	}
	
	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}
}
