/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;


import jakarta.annotation.Nullable;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.DB2zAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.DB2zIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.internal.DB2LockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.FetchLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.DB2zSequenceSupport;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.DB2zGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.dialect.type.IntervalType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import jakarta.persistence.TemporalType;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeAsTimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsJdbcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.ROWID;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * An SQL dialect for DB2 for z/OS, previously known as known as Db2 UDB for z/OS and Db2 UDB for z/OS and OS/390.
 *
 * @author Christian Beikov
 */
public class DB2zLegacyDialect extends DB2LegacyDialect {

	private static final Pattern DB2Z_VERSION_PATTERN = Pattern.compile( "V(\\d+)R(\\d+)M(\\d+)" );
	final static DatabaseVersion DB2_LUW_VERSION9 = DatabaseVersion.make( 9, 0);

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 7 );

	public DB2zLegacyDialect(DialectResolutionInfo info) {
		this( determinFullDatabaseVersion( info ) );
		registerKeywords( info );
	}

	public DB2zLegacyDialect() {
		this( DEFAULT_VERSION );
	}

	public DB2zLegacyDialect(DatabaseVersion version) {
		super(version);
	}

	@Override
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return determinFullDatabaseVersion( info );
	}

	public static DatabaseVersion determinFullDatabaseVersion(DialectResolutionInfo info) {
		String versionString = null;
		final var databaseMetadata = info.getDatabaseMetadata();
		if ( databaseMetadata != null ) {
			try {
				versionString = databaseMetadata.getDatabaseProductVersion();
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		final var databaseVersion = versionString == null ? null : parseVersion( versionString );
		return databaseVersion != null ? databaseVersion : info.makeCopyOrDefault( DEFAULT_VERSION );
	}

	public static @Nullable DatabaseVersion parseVersion(String versionString) {
		final var matcher = DB2Z_VERSION_PATTERN.matcher( versionString );
		if ( matcher.find() ) {
			final int majorVersion = parseInt( matcher.group( 1 ) );
			final int minorVersion = parseInt( matcher.group( 2 ) );
			final int microVersion = parseInt( matcher.group( 3 ) );
			return new SimpleDatabaseVersion( majorVersion, minorVersion, microVersion );
		}
		else {
			return null;
		}
	}

	@Override
	protected LockingSupport buildLockingSupport() {
		return DB2LockingSupport.forDB2z();
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		final var functionFactory = new CommonFunctionFactory( functionContributions );

		if ( getVersion().isSameOrAfter( 12 ) ) {
			functionFactory.listagg( null );
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
			functionFactory.regexpLike();
		}

		functionFactory.position_function( true );
		functionFactory.trunc_truncTimestamp();
		functionFactory.truncTimestamp();
		functionFactory.octetLength_pattern( "length(?1,octets)" );
		functionFactory.power_db2z();
	}

	@Override
	protected void registerListagg(CommonFunctionFactory functionFactory) {
		if ( getVersion().isSameOrAfter( 12, 1, 501 ) ) {
			super.registerListagg( functionFactory );
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
		return switch ( sqlTypeCode ) {
			// DB2 z/OS does not have a boolean type
			case BOOLEAN -> "smallint";
			// DB2 z/OS does not support nationalized types, so normalize them
			case NCHAR -> columnType( CHAR );
			case NVARCHAR -> columnType( VARCHAR );
			case NCLOB -> columnType( CLOB );
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, SmallIntJdbcType.INSTANCE );

		// No support for time with time zone through JDBC
		jdbcTypeRegistry.addDescriptor( TimeAsTimestampWithTimeZoneJdbcType.INSTANCE );
		// No support for Java Time types through JDBC
		jdbcTypeRegistry.addDescriptor( TimeUtcAsJdbcTimeJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( TimestampUtcAsJdbcTimestampJdbcType.INSTANCE );
	}

	@Override
	protected boolean supportsPredicateAsExpression() {
		// Not sure, but let's be conservative
		return false;
	}

	@Override
	public boolean supportsIsTrue() {
		// No mention in the docs
		return false;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		// UPDATE statement syntax doesn't seem to allow this
		return false;
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return DB2zAggregateSupport.INSTANCE;
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
		return getVersion().isBefore( 12, 1, 500 )
				? FetchLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return DB2zIdentityColumnSupport.INSTANCE;
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
		final var pattern = new StringBuilder();
		final String timestampExpression;
		if ( unit.isDateUnit() ) {
			timestampExpression =
					temporalType == TemporalType.TIME
							? "timestamp('1970-01-01',?3)"
							: "?3";
		}
		else {
			timestampExpression =
					temporalType == TemporalType.DATE
							? "cast(?3 as timestamp)"
							: "?3";
		}
		pattern.append( '(' );
		pattern.append( timestampExpression );
		pattern.append( "+(?2" );
		switch (unit) {
			case NATIVE:
			case NANOSECOND:
				pattern.append("/1e9) second");
				break;
			case WEEK:
				//note: DB2 does not have add_weeks()
				pattern.append("*7) day");
				break;
			case QUARTER:
				pattern.append("*3) month");
				break;
			default:
				pattern.append(") ?1");
		}
		pattern.append("s)");
		return pattern.toString();
	}

	@Override @SuppressWarnings("deprecation")
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final var pattern = new StringBuilder();
		final String fromExpression;
		final String toExpression;
		if ( unit.isDateUnit() ) {
			fromExpression = "?2";
			toExpression = "?3";
		}
		else {
			fromExpression = switch (fromTemporalType) {
				case DATE -> "cast(?2 as timestamp)";
				case TIME -> "timestamp('1970-01-01',?2)";
				default -> "?2";
			};
			toExpression = switch (toTemporalType) {
				case DATE -> "cast(?3 as timestamp)";
				case TIME -> "timestamp('1970-01-01',?3)";
				default -> "?3";
			};
		}
		switch (unit) {
			case NANOSECOND:
				pattern.append( "(1e9*");
			case NATIVE:
				pattern.append( '(');
				pattern.append( toExpression );
				pattern.append( '-' );
				pattern.append( fromExpression );
				pattern.append( ')');
				break;
			default:
				pattern.append( "timestampdiff(" );
				pattern.append( switch ( unit ) {
					case SECOND -> "2";
					case MINUTE -> "4";
					case HOUR -> "8";
					case DAY -> "16";
					case WEEK -> "32";
					case MONTH -> "64";
					case QUARTER -> "128";
					case YEAR -> "256";
					default -> throw new SemanticException( "Illegal unit for timestamp_diff(): " + unit );
				} );
				pattern.append( ",char(");
				pattern.append( toExpression );
				pattern.append( '-' );
				pattern.append( fromExpression );
				pattern.append( "))");
				break;
		}
		if ( unit == TemporalUnit.NANOSECOND ) {
			pattern.append( ')');
		}
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

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return DB2zGlobalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	// I speculate that this is a correct implementation of rowids for DB2 for z/OS,
	// just on the basis of the DB2 docs, but I currently have no way to test it
	// Note that the implementation inherited from DB2Dialect for LUW will not work!

	@Override
	public String rowId(String rowId) {
		return rowId == null || !rowId.isEmpty() ? rowId : "rowid_";
	}

	@Override
	public int rowIdSqlType() {
		return ROWID;
	}

	@Override
	public String getRowIdColumnString(String rowId) {
		return rowId( rowId ) + " rowid not null generated always";
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current timestamp from sysibm.sysdummy1";
	}

	@Override
	public boolean supportsValuesList() {
		// DB2 z/OS has a VALUES statement, but that doesn't support multiple values
		return false;
	}

	@Override
	public boolean supportsValuesListForInsert() {
		return getVersion().isSameOrAfter( 13, 1, 506 );
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return false;
	}
}
