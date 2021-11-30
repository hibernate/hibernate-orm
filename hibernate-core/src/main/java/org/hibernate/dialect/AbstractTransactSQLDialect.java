/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.TransactSQLStrFunction;
import org.hibernate.query.NullOrdering;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.identity.AbstractTransactSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.temptable.AfterUseAction;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.query.sqm.mutation.internal.temptable.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;

/**
 * An abstract base class for Sybase and MS SQL Server dialects.
 *
 * @author Gavin King
 */
public abstract class AbstractTransactSQLDialect extends Dialect {
	public AbstractTransactSQLDialect() {
		super();

		registerColumnType( Types.BOOLEAN, "bit" );

		//'tinyint' is an unsigned type in Sybase and
		//SQL Server, holding values in the range 0-255
		//see HHH-6779
		registerColumnType( Types.TINYINT, "smallint" );

		//it's called 'int' not 'integer'
		registerColumnType( Types.INTEGER, "int" );

		//note that 'real' is double precision on SQL Server, single precision on Sybase
		//but 'float' is single precision on Sybase, double precision on SQL Server

		registerColumnType( Types.DATE, "datetime" );
		registerColumnType( Types.TIME, "datetime" );
		registerColumnType( Types.TIMESTAMP, "datetime" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "datetime" );

		registerColumnType( Types.BLOB, "image" );
		registerColumnType( Types.CLOB, "text" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == Types.BIT ) {
			return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
		}
		return super.resolveSqlTypeDescriptor(
				columnTypeName,
				jdbcTypeCode,
				precision,
				scale,
				jdbcTypeRegistry
		);
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
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
		CommonFunctionFactory.substring_substringLen( queryEngine );
		CommonFunctionFactory.datepartDatename( queryEngine );
		CommonFunctionFactory.lastDay_eomonth( queryEngine );

		queryEngine.getSqmFunctionRegistry().register( "least", new CaseLeastGreatestEmulation( true ) );
		queryEngine.getSqmFunctionRegistry().register( "greatest", new CaseLeastGreatestEmulation( false ) );
		queryEngine.getSqmFunctionRegistry().register( "str", new TransactSQLStrFunction( queryEngine.getTypeConfiguration() ) );
		queryEngine.getSqmFunctionRegistry().register(
				"concat",
				new CastingConcatFunction(
						this,
						"+",
						SqlAstNodeRenderingMode.DEFAULT,
						queryEngine.getTypeConfiguration()
				)
		);
	}

	@Override
	public String trimPattern(TrimSpec specification, char character) {
		return replaceLtrimRtrim(specification, character);
	}

	public static String replaceLtrimRtrim(TrimSpec specification, char character) {
		boolean blank = character == ' ';
		switch ( specification ) {
			case LEADING:
				return blank
						? "ltrim(?1)"
						: "replace(replace(ltrim(replace(replace(?1,' ','#%#%'),'@',' ')),' ','@'),'#%#%',' ')"
								.replace('@', character);
			case TRAILING:
				return blank
						? "rtrim(?1)"
						: "replace(replace(rtrim(replace(replace(?1,' ','#%#%'),'@',' ')),' ','@'),'#%#%',' ')"
								.replace('@', character);
			default:
				return blank
						? "ltrim(rtrim(?1))"
						: "replace(replace(ltrim(rtrim(replace(replace(?1,' ','#%#%'),'@',' '))),' ','@'),'#%#%',' ')"
								.replace('@', character);
		}
	}

	@Override
	public String getAddColumnString() {
		return "add";
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
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.TABLE;
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		return lockOptions.getLockMode().greaterThan( LockMode.READ ) ? tableName + " holdlock" : tableName;
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		// TODO:  merge additional lock options support in Dialect.applyLocksToSql
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
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableMutationStrategy(
				TemporaryTable.createIdTable(
						entityDescriptor,
						basename -> '#' + TemporaryTable.ID_TABLE_PREFIX + basename,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableInsertStrategy(
				TemporaryTable.createEntityTable(
						entityDescriptor,
						name -> '#' + TemporaryTable.ENTITY_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.LOCAL;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		// sql-server, at least needed this dropped after use; strange!
		return AfterUseAction.DROP;
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}

	@Override
	public String getSelectGUIDString() {
		return "select newid()";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new AbstractTransactSQLIdentityColumnSupport();
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "0x" );
		PrimitiveByteArrayJavaTypeDescriptor.INSTANCE.appendString( appender, bytes );
	}
}
