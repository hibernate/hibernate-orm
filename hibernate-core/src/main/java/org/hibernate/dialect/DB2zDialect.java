/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.identity.DB2390IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.FetchLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.DB2zSequenceSupport;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IntervalType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;

/**
 * An SQL dialect for DB2 for z/OS, previously known as known as Db2 UDB for z/OS and Db2 UDB for z/OS and OS/390.
 *
 * @author Christian Beikov
 */
public class DB2zDialect extends DB2Dialect {

	private final DatabaseVersion version;

	public DB2zDialect(DialectResolutionInfo info) {
		this( info.makeCopy() );
		registerKeywords( info );
	}

	public DB2zDialect() {
		this( DatabaseVersion.make( 7 ) );
	}

	public DB2zDialect(DatabaseVersion version) {
		super();
		this.version = version;
	}

	@Override
	protected String columnType(int jdbcTypeCode) {
		// See https://www.ibm.com/support/knowledgecenter/SSEPEK_10.0.0/wnew/src/tpc/db2z_10_timestamptimezone.html
		if ( jdbcTypeCode==TIMESTAMP_WITH_TIMEZONE && version.isAfter(10) ) {
			return "timestamp with time zone";
		}
		return super.columnType(jdbcTypeCode);
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return getZVersion().isAfter(10) ? TimeZoneSupport.NATIVE : TimeZoneSupport.NONE;
	}

	DatabaseVersion getZVersion() {
		return version;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getZVersion().isBefore(8)
				? NoSequenceSupport.INSTANCE
				: DB2zSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return getZVersion().isBefore(8) ? null : "select * from sysibm.syssequences";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return getZVersion().isBefore(12)
				? FetchLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new DB2390IdentityColumnSupport();
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
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		StringBuilder pattern = new StringBuilder();
		final boolean castTo;
		if ( unit.isDateUnit() ) {
			castTo = temporalType == TemporalType.TIME;
		}
		else {
			castTo = temporalType == TemporalType.DATE;
		}
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
		if (castTo) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
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
				return new DB2zSqlAstTranslator<>( sessionFactory, statement, version );
			}
		};
	}
}
