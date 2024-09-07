/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;


import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.DB2390IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.FetchLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.DB2zSequenceSupport;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import jakarta.persistence.TemporalType;

import java.util.List;

import static org.hibernate.type.SqlTypes.ROWID;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;

/**
 * An SQL dialect for DB2 for z/OS, previously known as known as Db2 UDB for z/OS and Db2 UDB for z/OS and OS/390.
 *
 * @author Christian Beikov
 */
public class DB2zLegacyDialect extends DB2LegacyDialect {

	final static DatabaseVersion DB2_LUW_VERSION9 = DatabaseVersion.make( 9, 0);

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 7 );

	public DB2zLegacyDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
		registerKeywords( info );
	}

	public DB2zLegacyDialect() {
		this( DEFAULT_VERSION );
	}

	public DB2zLegacyDialect(DatabaseVersion version) {
		super(version);
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		if ( getVersion().isSameOrAfter( 12 ) ) {
			CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
			functionFactory.listagg( null );
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		}
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		if ( getVersion().isAfter( 10 ) ) {
			switch ( sqlTypeCode ) {
				case TIME_WITH_TIMEZONE:
				case TIMESTAMP_WITH_TIMEZONE:
					// See https://www.ibm.com/support/knowledgecenter/SSEPEK_10.0.0/wnew/src/tpc/db2z_10_timestamptimezone.html
					return "timestamp with time zone";
			}
		}
		return super.columnType( sqlTypeCode );
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION9;
	}

	@Override
	protected UniqueDelegate createUniqueDelegate() {
		//TODO: when was 'create unique where not null index' really first introduced?
		return getVersion().isSameOrAfter(11)
				//use 'create unique where not null index'
				? new AlterTableUniqueIndexDelegate(this)
				//ignore unique keys on nullable columns in earlier versions
				: new SkipNullableUniqueDelegate(this);
	}

	@Override
	public String getCreateIndexString(boolean unique) {
		// we only create unique indexes, as opposed to unique constraints,
		// when the column is nullable, so safe to infer unique => nullable
		return unique ? "create unique where not null index" : "create index";
	}

	@Override
	public String getCreateIndexTail(boolean unique, List<Column> columns) {
		return "";
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		// Supported at least since DB2 z/OS 9.0
		return true;
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return getVersion().isAfter(10) ? TimeZoneSupport.NATIVE : TimeZoneSupport.NONE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion().isBefore(8)
				? NoSequenceSupport.INSTANCE
				: DB2zSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return getVersion().isBefore(8) ? null : "select * from sysibm.syssequences";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return getVersion().isBefore(12)
				? FetchLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return DB2390IdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean supportsSkipLocked() {
		return true;
	}

	@Override
	public boolean supportsLateral() {
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return getVersion().isSameOrAfter( 11 );
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final StringBuilder pattern = new StringBuilder();
		pattern.append("add_");
		switch (unit) {
			case NATIVE:
			case NANOSECOND:
				pattern.append("second");
				break;
			case WEEK:
				//note: DB2 does not have add_weeks()
				pattern.append("day");
				break;
			case QUARTER:
				pattern.append("month");
				break;
			default:
				pattern.append("?1");
		}
		pattern.append("s(");
		final String timestampExpression;
		if ( unit.isDateUnit() ) {
			if ( temporalType == TemporalType.TIME ) {
				timestampExpression = "timestamp('1970-01-01',?3)";
			}
			else {
				timestampExpression = "?3";
			}
		}
		else {
			if ( temporalType == TemporalType.DATE ) {
				timestampExpression = "cast(?3 as timestamp)";
			}
			else {
				timestampExpression = "?3";
			}
		}
		pattern.append(timestampExpression);
		pattern.append(",");
		switch (unit) {
			case NANOSECOND:
				pattern.append("(?2)/1e9");
				break;
			case WEEK:
				pattern.append("(?2)*7");
				break;
			case QUARTER:
				pattern.append("(?2)*3");
				break;
			default:
				pattern.append("?2");
		}
		pattern.append(")");
		return pattern.toString();
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DB2zLegacySqlAstTranslator<>( sessionFactory, statement, getVersion() );
			}
		};
	}

	// I speculate that this is a correct implementation of rowids for DB2 for z/OS,
	// just on the basis of the DB2 docs, but I currently have no way to test it
	// Note that the implementation inherited from DB2Dialect for LUW will not work!

	@Override
	public String rowId(String rowId) {
		return rowId == null || rowId.isEmpty() ? "rowid_" : rowId;
	}

	@Override
	public int rowIdSqlType() {
		return ROWID;
	}

	@Override
	public String getRowIdColumnString(String rowId) {
		return rowId( rowId ) + " rowid not null generated always";
	}
}
