/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;


import java.sql.Types;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.community.dialect.identity.SybaseAnywhereIdentityColumnSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
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

import static org.hibernate.type.SqlTypes.*;

/**
 * SQL Dialect for Sybase Anywhere
 * (Tested on ASA 8.x)
 */
public class SybaseAnywhereDialect extends SybaseDialect {

	public SybaseAnywhereDialect() {
		this( DatabaseVersion.make( 8 ) );
	}

	public SybaseAnywhereDialect(DatabaseVersion version) {
		this(version, null);
	}

	public SybaseAnywhereDialect(DialectResolutionInfo info){
		this( info.makeCopy(), info );
		registerKeywords( info );
	}

	public SybaseAnywhereDialect(DatabaseVersion version, DialectResolutionInfo info) {
		super( version, info );
	}

	@Override
	protected String columnType(int jdbcTypeCode) {
		switch (jdbcTypeCode) {
			case DATE:
				return "date";
			case TIME:
				return "time";
			case TIMESTAMP:
				return "timestamp";
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp with time zone";

			//these types hold up to 2 GB
			case LONGVARCHAR:
				return "long varchar";
			case LONGNVARCHAR:
				return "long nvarchar";
			case LONGVARBINARY:
				return "long binary";

			case NCLOB:
				return "ntext";

			default:
				return super.columnType(jdbcTypeCode);
		}
	}

	@Override
	public int getMaxVarcharLength() {
		return 32_767;
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
		return getVersion().isSameOrAfter( 10 ) ? RowLockStrategy.COLUMN : RowLockStrategy.TABLE;
	}

	@Override
	public String getForUpdateString() {
		return getVersion().isBefore( 10 ) ? "" : " for update";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getVersion().isBefore( 10 )
				? ""
				: getForUpdateString() + " of " + aliases;
	}

	@Override
	public String appendLockHint(LockOptions mode, String tableName) {
		return getVersion().isBefore( 10 ) ? super.appendLockHint( mode, tableName ) : tableName;
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return getVersion().isBefore( 10 )
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
