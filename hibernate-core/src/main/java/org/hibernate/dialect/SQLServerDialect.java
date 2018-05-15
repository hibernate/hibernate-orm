/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;
import java.util.Locale;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.SQLServerIdentityColumnSupport;
import org.hibernate.dialect.pagination.LegacyLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.spi.AnsiTrimEmulationFunctionTemplate;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * A dialect for Microsoft SQL Server 2000
 *
 * @author Gavin King
 */
@SuppressWarnings("deprecation")
public class SQLServerDialect extends AbstractTransactSQLDialect {
	private static final int PARAM_LIST_SIZE_LIMIT = 2100;

	private final LimitHandler limitHandler;

	/**
	 * Constructs a SQLServerDialect
	 */
	public SQLServerDialect() {
		registerColumnType( Types.VARBINARY, "image" );
		registerColumnType( Types.VARBINARY, 8000, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, "image" );
		registerColumnType( Types.LONGVARCHAR, "text" );
		registerColumnType( Types.BOOLEAN, "bit" );

		registerKeyword( "top" );
		registerKeyword( "key" );

		this.limitHandler = new TopLimitHandler( false, false );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.registerPattern( "second", "datepart(second, ?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "minute", "datepart(minute, ?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "hour", "datepart(hour, ?1)", StandardSpiBasicTypes.INTEGER );
		registry.namedTemplateBuilder( "locate", "charindex" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 3 )
				.register();

		registry.registerPattern( "extract", "datepart(?1, ?3)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "mod", "?1 % ?2", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "bit_length", "datalength(?1) * 8", StandardSpiBasicTypes.INTEGER );

		registry.register( "trim", new AnsiTrimEmulationFunctionTemplate() );
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	static int getAfterSelectInsertPoint(String sql) {
		final int selectIndex = sql.toLowerCase(Locale.ROOT).indexOf( "select" );
		final int selectDistinctIndex = sql.toLowerCase(Locale.ROOT).indexOf( "select distinct" );
		return selectIndex + (selectDistinctIndex == selectIndex ? 15 : 6);
	}

	@Override
	public String getLimitString(String querySelect, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return new StringBuilder( querySelect.length() + 8 )
				.append( querySelect )
				.insert( getAfterSelectInsertPoint( querySelect ), " top " + limit )
				.toString();
	}

	@Override
	public LimitHandler getLimitHandler() {
		if ( isLegacyLimitHandlerBehaviorEnabled() ) {
			return new LegacyLimitHandler( this );
		}
		return getDefaultLimitHandler();
	}

	protected LimitHandler getDefaultLimitHandler() {
		return limitHandler;
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
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	public char closeQuote() {
		return ']';
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "SELECT SCHEMA_NAME()";
	}

	@Override
	public char openQuote() {
		return '[';
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		final LockMode mode = lockOptions.getLockMode();
		switch ( mode ) {
			case UPGRADE:
			case UPGRADE_NOWAIT:
			case PESSIMISTIC_WRITE:
			case WRITE:
				return tableName + " with (updlock, rowlock)";
			case PESSIMISTIC_READ:
				return tableName + " with (holdlock, rowlock)";
			case UPGRADE_SKIPLOCKED:
				return tableName + " with (updlock, rowlock, readpast)";
			default:
				return tableName;
		}
	}


	/**
	 * The current_timestamp is more accurate, but only known to be supported in SQL Server 7.0 and later and
	 * Sybase not known to support it at all
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		// SQL Server (at least up through 2005) does not support defining
		// cascade delete constraints which can circle back to the mutating
		// table
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		// note: at least my local SQL Server 2005 Express shows this not working...
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// here assume SQLServer2005 using snapshot isolation, which does not have this problem
		return false;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		// here assume SQLServer2005 using snapshot isolation, which does not have this problem
		return false;
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.TINYINT ?
				SmallIntSqlDescriptor.INSTANCE :
				super.getSqlTypeDescriptorOverride( sqlCode );
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new SQLServerIdentityColumnSupport();
	}
}
