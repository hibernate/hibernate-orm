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
import org.hibernate.dialect.function.SybaseLocateEmulationFunctionTemplate;
import org.hibernate.dialect.identity.AbstractTransactSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.spi.ConcatFunctionTemplate;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An abstract base class for Sybase and MS SQL Server dialects.
 *
 * @author Gavin King
 */
abstract class AbstractTransactSQLDialect extends Dialect {
	public AbstractTransactSQLDialect() {
		super();
		registerColumnType( Types.BINARY, "binary($l)" );
		registerColumnType( Types.BIT, "tinyint" );
		registerColumnType( Types.BIGINT, "numeric(19,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.INTEGER, "int" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "datetime" );
		registerColumnType( Types.TIME, "datetime" );
		registerColumnType( Types.TIMESTAMP, "datetime" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "image" );
		registerColumnType( Types.CLOB, "text" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.namedTemplateBuilder( "ascii" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "char" )
				.setInvariantType( StandardSpiBasicTypes.CHARACTER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "len" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.lower( registry );
		CommonFunctionFactory.upper( registry );
		registry.namedTemplateBuilder( "str" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 3 )
				.register();
		registry.namedTemplateBuilder( "ltrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "rtrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "reverse" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "space" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();

		registry.registerNoArgs( "user", StandardSpiBasicTypes.STRING );

		registry.registerNoArgs( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );

		registry.noArgsBuilder( "getdate" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		registry.noArgsBuilder( "getutcdate" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		registry.namedTemplateBuilder( "day" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "month" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "year" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "datename" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();

		CommonFunctionFactory.abs( registry );
		CommonFunctionFactory.sign( registry );

		CommonFunctionFactory.acos( registry );
		CommonFunctionFactory.asin( registry );
		CommonFunctionFactory.atan( registry );
		CommonFunctionFactory.cos( registry );
		CommonFunctionFactory.cot( registry );
		CommonFunctionFactory.exp( registry );
		CommonFunctionFactory.log( registry );
		CommonFunctionFactory.log10( registry );
		CommonFunctionFactory.sin( registry );
		CommonFunctionFactory.sqrt( registry );
		CommonFunctionFactory.tan( registry );
		CommonFunctionFactory.sin( registry );
		registry.noArgsBuilder( "pi" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		registry.namedTemplateBuilder( "square" )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.rand( registry );

		CommonFunctionFactory.radians( registry );
		CommonFunctionFactory.degrees( registry );

		CommonFunctionFactory.round( registry );
		CommonFunctionFactory.ceiling( registry );
		CommonFunctionFactory.floor( registry );

		registry.namedTemplateBuilder( "isnull" )
				.setExactArgumentCount( 2 )
				.register();

		registry.register( "concat", new ConcatFunctionTemplate( "(", "+", ")" ) );

		registry.registerAlternateKey( "length", "len" );
		registry.registerPattern( "trim", "ltrim(rtrim(?1))", StandardSpiBasicTypes.STRING );
		registry.register( "locate", new SybaseLocateEmulationFunctionTemplate() );
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
	public IdTableStrategy getDefaultIdTableStrategy() {
		final StandardIdTableSupport idTableSupport = new StandardIdTableSupport( getIdTableExporter() ) {
			@Override
			protected String determineIdTableName(String baseName) {
				return "#" + baseName;
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
