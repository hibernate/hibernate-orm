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

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.AbstractTransactSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.naming.Identifier;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * An abstract base class for Sybase and MS SQL Server dialects.
 *
 * @author Gavin King
 */
abstract class AbstractTransactSQLDialect extends Dialect {
	public AbstractTransactSQLDialect() {
		super();
		registerColumnType( Types.BOOLEAN, "bit" );
		registerColumnType( Types.BIT, 1, "bit" );
		registerColumnType( Types.BIT, "smallint" );

		//'tinyint' is an unsigned type in Sybase and
		//SQL Server, holding values in the range 0-255
		//see HHH-6779
		registerColumnType( Types.TINYINT, "smallint" );

		//it's called 'int' not 'integer'
		registerColumnType( Types.INTEGER, "int" );

		//note that 'real' is double precision on SQL Server, single precision on Sybase
		//but 'float' is single precision on Sybase, double precision on SQL Server

		registerColumnType( Types.LONGVARBINARY, "image" );
		registerColumnType( Types.LONGVARCHAR, "text" );

		registerColumnType( Types.DATE, "datetime" );
		registerColumnType( Types.TIME, "datetime" );
		registerColumnType( Types.TIMESTAMP, "datetime" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "datetime" );

		registerColumnType( Types.BLOB, "image" );
		registerColumnType( Types.CLOB, "text" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		switch (sqlCode) {
			case Types.BOOLEAN:
				return BitSqlDescriptor.INSTANCE;
			case Types.TINYINT:
				return SmallIntSqlDescriptor.INSTANCE;
			default:
				return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}


	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.ln_log( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.atan2_atn2( queryEngine );
		CommonFunctionFactory.mod_operator( queryEngine );
		CommonFunctionFactory.square( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.pad_replicate( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.ascii(queryEngine);
		CommonFunctionFactory.chr_char( queryEngine );
		CommonFunctionFactory.concat_plusOperator( queryEngine );
		CommonFunctionFactory.trim1( queryEngine );
		CommonFunctionFactory.repeat_replicate( queryEngine );
		CommonFunctionFactory.characterLength_len( queryEngine );
		CommonFunctionFactory.extract_datepart( queryEngine );
		CommonFunctionFactory.lastDay_eomonth( queryEngine );
	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		sqlAppender.append("dateadd(");
		//TODO: SQL Server supports nanosecond, but what about Sybase?
		sqlAppender.append( unit.toString() );
		sqlAppender.append(", ");
		magnitude.render();
		sqlAppender.append(", ");
		to.render();
		sqlAppender.append(")");
	}

	@Override
	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		sqlAppender.append("datediff(");
		//TODO: SQL Server supports nanosecond, but what about Sybase?
		sqlAppender.append( unit.toString() );
		sqlAppender.append(", ");
		from.render();
		sqlAppender.append(", ");
		to.render();
		sqlAppender.append(")");
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getNullColumnString() {
		return "";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		return lockOptions.getLockMode().greaterThan( LockMode.READ ) ? tableName + " holdlock" : tableName;
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		// TODO:  merge additional lockoptions support in Dialect.applyLocksToSql
		final Iterator itr = aliasedLockOptions.getAliasLockIterator();
		final StringBuilder buffer = new StringBuilder( sql );

		while ( itr.hasNext() ) {
			final Map.Entry entry = (Map.Entry) itr.next();
			final LockMode lockMode = (LockMode) entry.getValue();
			if ( lockMode.greaterThan( LockMode.READ ) ) {
				final String alias = (String) entry.getKey();
				int start = -1;
				int end = -1;
				if ( sql.endsWith( " " + alias ) ) {
					start = ( buffer.length() - alias.length() );
					end = start + alias.length();
				}
				else {
					int position = buffer.indexOf( " " + alias + " " );
					if ( position <= -1 ) {
						position = buffer.indexOf( " " + alias + "," );
					}
					if ( position > -1 ) {
						start = position + 1;
						end = start + alias.length();
					}
				}

				if ( start > -1 ) {
					final String lockHint = appendLockHint( aliasedLockOptions, alias );
					buffer.replace( start, end, lockHint );
				}
			}
		}
		return buffer.toString();
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		// sql server just returns automatically
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		// This assumes you will want to ignore any update counts
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}

		// You may still have other ResultSets or update counts left to process here
		// but you can't do it now or the ResultSet you just got will be closed
		return ps.getResultSet();
	}

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
		return "select getdate()";
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		final StandardIdTableSupport idTableSupport = new StandardIdTableSupport( getIdTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				return new Identifier( "#" + baseName.getText(), false );
			}
		};

		return new LocalTemporaryTableStrategy( idTableSupport );
	}

	@Override
	public String getSelectGUIDString() {
		return "select newid()";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsExistsInSelect() {
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
	public boolean supportsTupleDistinctCounts() {
		return false;
	}
	
	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new AbstractTransactSQLIdentityColumnSupport();
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}
}
