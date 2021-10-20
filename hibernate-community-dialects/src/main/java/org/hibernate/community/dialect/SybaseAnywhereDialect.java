/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;


import org.hibernate.LockOptions;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.community.dialect.identity.SybaseAnywhereIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import java.sql.Types;
import java.util.Map;

/**
 * SQL Dialect for Sybase Anywhere
 * (Tested on ASA 8.x)
 */
public class SybaseAnywhereDialect extends SybaseDialect {

	public SybaseAnywhereDialect() {
		this( 800, false );
	}

	public SybaseAnywhereDialect(DialectResolutionInfo info){
		this(
				info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10,
				info.getDriverName() != null && info.getDriverName().contains( "jTDS" )
		);
		registerKeywords( info );
	}

	public SybaseAnywhereDialect(int version, boolean jtdsDriver) {
		super( version, jtdsDriver );

		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp with time zone" );

		final int maxStringLength = 32_767;

		registerColumnType( Types.CHAR, maxStringLength, "char($l)" );
		registerColumnType( Types.VARCHAR, maxStringLength, "varchar($l)" );
		registerColumnType( Types.VARCHAR, "long varchar)" );

		registerColumnType( Types.NCHAR, maxStringLength, "nchar($l)" );
		registerColumnType( Types.NVARCHAR, maxStringLength, "nvarchar($l)" );
		registerColumnType( Types.NVARCHAR, "long nvarchar)" );

		//note: 'binary' is actually a synonym for 'varbinary'
		registerColumnType( Types.BINARY, maxStringLength, "binary($l)" );
		registerColumnType( Types.VARBINARY, maxStringLength, "varbinary($l)" );
		registerColumnType( Types.VARBINARY, "long binary)" );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SybaseAnywhereSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	public String currentDate() {
		return "current date";
	}

	@Override
	public String currentTime() {
		return "current time";
	}

	@Override
	public String currentTimestamp() {
		return "current timestamp";
	}

	/**
	 * Sybase Anywhere syntax would require a "DEFAULT" for each column specified,
	 * but I suppose Hibernate use this syntax only with tables with just 1 column
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getNoColumnsInsertString() {
		return "values (default)";
	}

	/**
	 * ASA does not require to drop constraint before dropping tables, so disable it.
	 * <p/>
	 * NOTE : Also, the DROP statement syntax used by Hibernate to drop constraints is
	 * not compatible with ASA.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new SybaseAnywhereIdentityColumnSupport();
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return getVersion() >= 1000 ? RowLockStrategy.COLUMN : RowLockStrategy.TABLE;
	}

	@Override
	public String getForUpdateString() {
		return getVersion() < 1000 ? "" : " for update";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getVersion() < 1000
				? ""
				: getForUpdateString() + " of " + aliases;
	}

	@Override
	public String appendLockHint(LockOptions mode, String tableName) {
		return getVersion() < 1000 ? super.appendLockHint( mode, tableName ) : tableName;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return getVersion() < 1000
				? super.applyLocksToSql( sql, aliasedLockOptions, keyColumnNames )
				: sql + new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString();
	}

	@Override
	public LimitHandler getLimitHandler() {
		//TODO: support 'TOP ? START AT ?'
		//Note: Sybase Anywhere also supports LIMIT OFFSET,
		//      but it looks like this syntax is not enabled
		//      by default
		return TopLimitHandler.INSTANCE;
	}

}
