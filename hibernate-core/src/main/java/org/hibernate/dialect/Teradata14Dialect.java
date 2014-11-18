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
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.Teradata14IdentityColumnSupport;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.tool.schema.internal.StandardIndexExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;

/**
 * A dialect for the Teradata database
 */
public class Teradata14Dialect extends TeradataDialect {
	/**
	 * Constructor
	 */
	StandardIndexExporter TeraIndexExporter = null;

	public Teradata14Dialect() {
		super();
		//registerColumnType data types
		registerColumnType( Types.BIGINT, "BIGINT" );
		registerColumnType( Types.BINARY, "VARBYTE(100)" );
		registerColumnType( Types.LONGVARBINARY, "VARBYTE(32000)" );
		registerColumnType( Types.LONGVARCHAR, "VARCHAR(32000)" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		registerFunction( "current_time", new SQLFunctionTemplate( StandardBasicTypes.TIME, "current_time" ) );
		registerFunction( "current_date", new SQLFunctionTemplate( StandardBasicTypes.DATE, "current_date" ) );

		TeraIndexExporter =  new TeradataIndexExporter( this );
	}

	@Override
	public String getAddColumnString() {
		return "Add";
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
		float f = precision > 0 ? (float) scale / (float) precision : 0;
		int p = ( precision > 38 ? 38 : precision );
		int s = ( precision > 38 ? (int) ( 38.0 * f ) : ( scale > 38 ? 38 : scale ) );
		return super.getTypeName( code, length, p, s );
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return false;
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
	public boolean supportsExistsInSelect() {
		return false;
	}


	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}


	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter( col, Types.REF );
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement cs) throws SQLException {
		boolean isResultSet = cs.execute();
		while ( !isResultSet && cs.getUpdateCount() != -1 ) {
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
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			String constraintName = null;

			int errorCode = sqle.getErrorCode();
			if ( errorCode == 27003 ) {
				constraintName = extractUsingTemplate( "Unique constraint (", ") violated.", sqle.getMessage() );
			}
			else if ( errorCode == 2700 ) {
				constraintName = extractUsingTemplate( "Referential constraint", "violation:", sqle.getMessage() );
			}
			else if ( errorCode == 5317 ) {
				constraintName = extractUsingTemplate( "Check constraint (", ") violated.", sqle.getMessage() );
			}

			if ( constraintName != null ) {
				int i = constraintName.indexOf( '.' );
				if ( i != -1 ) {
					constraintName = constraintName.substring( i + 1 );
				}
			}
			return constraintName;
		}
	};

	@Override
	public String getWriteLockString(int timeout) {
		String sMsg = " Locking row for write ";
		if ( timeout == LockOptions.NO_WAIT ) {
			return sMsg + " nowait ";
		}
		return sMsg;
	}

	@Override
	public String getReadLockString(int timeout) {
		String sMsg = " Locking row for read  ";
		if ( timeout == LockOptions.NO_WAIT ) {
			return sMsg + " nowait ";
		}
		return sMsg;
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map keyColumnNames) {
		return new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString() + " " + sql;
	}

	@Override
	public boolean useFollowOnLocking(QueryParameters parameters) {
		return true;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public Exporter<Index> getIndexExporter() {
		return TeraIndexExporter;
	}


	private class TeradataIndexExporter extends StandardIndexExporter implements Exporter<Index> {

		public TeradataIndexExporter(Dialect dialect) {
			super(dialect);
		}

		@Override
		public String[] getSqlCreateStrings(Index index, Metadata metadata) {
			final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();
			final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
					index.getTable().getQualifiedTableName(),
					jdbcEnvironment.getDialect()
			);

			final String indexNameForCreation;
			if ( getDialect().qualifyIndexName() ) {
				indexNameForCreation = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
						new QualifiedNameImpl(
								index.getTable().getQualifiedTableName().getCatalogName(),
								index.getTable().getQualifiedTableName().getSchemaName(),
								jdbcEnvironment.getIdentifierHelper().toIdentifier( index.getName() )
						),
						jdbcEnvironment.getDialect()
				);
			}
			else {
				indexNameForCreation = index.getName();
			}

			StringBuilder colBuf = new StringBuilder("");
			boolean first = true;
			Iterator<Column> columnItr = index.getColumnIterator();
			while ( columnItr.hasNext() ) {
				final Column column = columnItr.next();
				if ( first ) {
					first = false;
				}
				else {
					colBuf.append( ", " );
				}
				colBuf.append( ( column.getQuotedName( jdbcEnvironment.getDialect() )) );
			}
			colBuf.append( ")" );

			final StringBuilder buf = new StringBuilder()
					.append( "create index " )
					.append( indexNameForCreation )
					.append(  "(" + colBuf  )
					.append( " on " )
					.append( tableName );

			return new String[] { buf.toString() };
		}
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new Teradata14IdentityColumnSupport();
	}
}
