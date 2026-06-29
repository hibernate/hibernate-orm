/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;


import jakarta.annotation.Nullable;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
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
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.sql.ast.DB2zSqlAstTranslator;
import org.hibernate.dialect.temptable.DB2zGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.dialect.type.IntervalType;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import jakarta.persistence.TemporalType;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.tool.schema.spi.Exporter;
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
 * A SQL dialect for DB2 for z/OS version 12.1 and above, previously known as:
 * <ul>
 * <li>"Db2 UDB for z/OS", and
 * <li>"Db2 UDB for z/OS and OS/390".
 * </ul>
 *
 * @author Christian Beikov
 */
public class DB2zDialect extends DB2Dialect {

	private static final Pattern DB2Z_VERSION_PATTERN = Pattern.compile( "V(\\d+)R(\\d+)M(\\d+)" );
	private final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 12, 1 );
	public final static DatabaseVersion DB2_LUW_VERSION = DB2Dialect.MINIMUM_VERSION;

	private final Exporter<ForeignKey> foreignKeyExporter = new DB2zForeignKeyExporter( this );

	public DB2zDialect(DialectResolutionInfo info) {
		this( determinFullDatabaseVersion( info ) );
		registerKeywords( info );
	}

	public DB2zDialect() {
		this( MINIMUM_VERSION );
	}

	public DB2zDialect(DatabaseVersion version) {
		super( version );
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
		return databaseVersion != null ? databaseVersion : info.makeCopyOrDefault( MINIMUM_VERSION );
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
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		final var functionFactory = new CommonFunctionFactory( functionContributions );

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
		return switch ( sqlTypeCode ) {
			// DB2 z/OS does not have a boolean type
			case BOOLEAN -> "smallint";
			// DB2 z/OS does not support nationalized types, so normalize them
			case NCHAR -> columnType( CHAR );
			case NVARCHAR -> columnType( VARCHAR );
			case NCLOB -> columnType( CLOB );
			// See https://www.ibm.com/support/knowledgecenter/SSEPEK_10.0.0/wnew/src/tpc/db2z_10_timestamptimezone.html
			case TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "timestamp with time zone";
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
		// todo: no support for OffsetDateTime
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
	protected void registerJsonFunctions(CommonFunctionFactory functionFactory) {
		// No JSON support
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return false;
	}

	@Override
	public String getCreateIndexString(boolean unique) {
		// we only create unique indexes, as opposed to unique constraints,
		// when the column is nullable, so safe to infer unique => nullable
		return unique ? "create unique where not null index" : "create index";
	}

	@Override
	public Exporter<ForeignKey> getForeignKeyExporter() {
		return foreignKeyExporter;
	}

	@Override
	public boolean canDisableConstraints() {
		return false;
	}

	@Override
	public String getDisableConstraintStatement(String tableName, String name) {
		return null;
	}

	@Override
	public String getEnableConstraintStatement(String tableName, String name) {
		return null;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.SMALLINT;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool ? '1' : '0' );
	}

	@Override
	public String getCreateIndexTail(boolean unique, List<Column> columns) {
		return "";
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return DB2zSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select case when seqtype='A' then seqschema else schema end as seqschema, case when seqtype='A' then seqname else name end as seqname, start, minvalue, maxvalue, increment from sysibm.syssequences";
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
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		return new DB2zSqlAstTranslator<>( factory, optionalTableUpdate, getVersion() )
				.createMergeOperation( optionalTableUpdate );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DB2zSqlAstTranslator<>( sessionFactory, statement, getVersion() );
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
