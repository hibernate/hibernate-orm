/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.TransactSQLStrFunction;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.dialect.temptable.TransactSQLLocalTemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.AbstractTransactSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.internal.TransactSQLLockingClauseStrategy;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;

/**
 * An abstract base class for Sybase and MS SQL Server dialects.
 *
 * @author Gavin King
 */
public abstract class AbstractTransactSQLDialect extends Dialect {

	public AbstractTransactSQLDialect(DatabaseVersion version) {
		super(version);
	}

	public AbstractTransactSQLDialect(DialectResolutionInfo info) {
		super(info);
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		// note that 'real' is double precision on SQL Server, single precision on Sybase
		// but 'float' is single precision on Sybase, double precision on SQL Server
		return switch (sqlTypeCode) {
			case BOOLEAN -> "bit";

			// 'tinyint' is an unsigned type in Sybase and
			// SQL Server, holding values in the range 0-255
			// see HHH-6779
			case TINYINT -> "smallint";

			//it's called 'int' not 'integer'
			case INTEGER -> "int";

			case DATE, TIME, TIMESTAMP, TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "datetime";

			case BLOB -> "image";
			case CLOB -> "text";
			case NCLOB -> "ntext";

			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 0;
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
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var functionFactory = new CommonFunctionFactory( functionContributions );

		functionFactory.cot();
		functionFactory.ln_log();
		functionFactory.log_loglog();
		functionFactory.log10();
		functionFactory.atan2_atn2();
		functionFactory.mod_operator();
		functionFactory.square();
		functionFactory.rand();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.pi();
		functionFactory.reverse();
		functionFactory.space();
		functionFactory.pad_replicate();
		functionFactory.yearMonthDay();
		functionFactory.ascii();
		functionFactory.chr_char();
		functionFactory.trim1();
		functionFactory.repeat_replicate();
		functionFactory.characterLength_len();
		functionFactory.substring_substringLen();
		functionFactory.datepartDatename();
		functionFactory.lastDay_eomonth();

		functionFactory.bitandorxornot_operator();

		functionContributions.getFunctionRegistry().register( "least", new CaseLeastGreatestEmulation( true ) );
		functionContributions.getFunctionRegistry().register( "greatest", new CaseLeastGreatestEmulation( false ) );
		functionContributions.getFunctionRegistry().register( "str", new TransactSQLStrFunction( functionContributions.getTypeConfiguration() ) );
		functionContributions.getFunctionRegistry().register(
				"concat",
				new CastingConcatFunction(
						this,
						"+",
						false,
						SqlAstNodeRenderingMode.DEFAULT,
						functionContributions.getTypeConfiguration()
				)
		);
	}

	@Override
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return replaceLtrimRtrim( specification, isWhitespace );
	}

	public static String replaceLtrimRtrim(TrimSpec specification, boolean isWhitespace) {
		return switch (specification) {
			case LEADING -> isWhitespace
					? "ltrim(?1)"
					: "substring(?1,patindex('%[^'+?2+']%',?1),len(?1+'x')-1-patindex('%[^'+?2+']%',?1)+1)";
			case TRAILING -> isWhitespace
					? "rtrim(?1)"
					: "substring(?1,1,len(?1+'x')-1-patindex('%[^'+?2+']%',reverse(?1))+1)";
			default -> isWhitespace
					? "ltrim(rtrim(?1))"
					: "substring(?1,patindex('%[^'+?2+']%',?1),len(?1+'x')-1-patindex('%[^'+?2+']%',?1)-patindex('%[^'+?2+']%',reverse(?1))+2)";
		};
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
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		return new TransactSQLLockingClauseStrategy( lockOptions.getScope(), querySpec.getRootPathsForLocking() );
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
	public String applyLocksToSql(String sql, LockOptions lockOptions, Map<String, String[]> keyColumnNameMap) {
		if ( lockOptions.getLockMode() == LockMode.NONE || keyColumnNameMap == null ) {
			return sql;
		}

		// TODO:  merge additional lock options support in Dialect.applyLocksToSql
		final StringBuilder buffer = new StringBuilder( sql );
		keyColumnNameMap.forEach( (tableAlias, keyColumnNames) -> {
			int start = -1;
			int end = -1;
			if ( sql.endsWith( " " + tableAlias ) ) {
				start = ( buffer.length() - tableAlias.length() );
				end = start + tableAlias.length();
			}
			else {
				int position = buffer.indexOf( " " + tableAlias + " " );
				if ( position <= -1 ) {
					position = buffer.indexOf( " " + tableAlias + "," );
				}
				if ( position > -1 ) {
					start = position + 1;
					end = start + tableAlias.length();
				}
			}

			if ( start > -1 ) {
				final String lockHint = appendLockHint( lockOptions, tableAlias );
				buffer.replace( start, end, lockHint );
			}

		} );

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
	public boolean requiresCastForConcatenatingNonStrings() {
		return true;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return TransactSQLLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.LOCAL;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return TransactSQLLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableCreateCommand();
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return TransactSQLLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableAfterUseAction();
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return TransactSQLLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableBeforeUseAction();
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
		return AbstractTransactSQLIdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "0x" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
	}
}
