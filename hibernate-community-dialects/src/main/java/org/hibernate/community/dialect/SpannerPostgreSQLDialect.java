/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.aggregate.SpannerPostgreSQLAggregateSupport;
import org.hibernate.community.dialect.sequence.SpannerPostgreSQLSequenceSupport;
import org.hibernate.community.dialect.sql.ast.SpannerPostgreSQLSqlAstTranslator;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.PersistentTemporaryTableStrategy;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;

import static org.hibernate.sql.ast.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARCHAR;

public class SpannerPostgreSQLDialect extends PostgreSQLDialect {

	private final UniqueDelegate SPANNER_UNIQUE_DELEGATE = new AlterTableUniqueIndexDelegate( this );

	private final StandardTableExporter SPANNER_TABLE_EXPORTER = new SpannerPostgreSQLTableExporter( this );

	private final LockingSupport SPANNER_LOCKING_SUPPORT =
			new LockingSupportSimple(
					PessimisticLockStyle.CLAUSE,
					RowLockStrategy.NONE,
					LockTimeoutType.NONE,
					OuterJoinLockingType.FULL,
					ConnectionLockTimeoutStrategy.NONE );

	protected final static DatabaseVersion MINIMUM_POSTGRES_VERSION = DatabaseVersion.make( 15 );

	public SpannerPostgreSQLDialect() {
		super();
	}

	public SpannerPostgreSQLDialect(DialectResolutionInfo info) {
		super( info );
	}

	public SpannerPostgreSQLDialect(DatabaseVersion version) {
		super( MINIMUM_POSTGRES_VERSION );
	}

	@Override
	protected void registerArrayFunctions(CommonFunctionFactory functionFactory) {
	}

	@Override
	protected void registerXmlFunctions(CommonFunctionFactory functionFactory) {
	}

	@Override
	public StandardTableExporter getTableExporter() {
		return SPANNER_TABLE_EXPORTER;
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return SPANNER_UNIQUE_DELEGATE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return SpannerPostgreSQLSequenceSupport.INSTANCE;
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return SpannerPostgreSQLAggregateSupport.INSTANCE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return SPANNER_LOCKING_SUPPORT;
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString();
	}

	@Override
	public String getWriteLockString(int timeout) {
		validateSpannerLockTimeout( timeout );
		return getForUpdateString();
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getWriteLockString(Timeout timeout) {
		return getWriteLockString( timeout.milliseconds() );
	}

	@Override
	public String getWriteLockString(String aliases, Timeout timeout) {
		return getWriteLockString( aliases, timeout.milliseconds() );
	}

	@Override
	public String getReadLockString(int timeout) {
		validateSpannerLockTimeout( timeout );
		return getForUpdateString();
	}

	@Override
	public String getReadLockString(Timeout timeout) {
		return getWriteLockString( timeout.milliseconds() );
	}

	@Override
	public String getReadLockString(String aliases, Timeout timeout) {
		return getWriteLockString( timeout.milliseconds() );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getForUpdateNowaitString() {
		throw new UnsupportedOperationException(
				"Spanner doesn't support for-update with no-wait timeout" );
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateNowaitString();
	}

	@Override
	public String getForUpdateSkipLockedString() {
		throw new UnsupportedOperationException(
				"Spanner doesn't support for-update with skip locked timeout" );
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateSkipLockedString();
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(
			QuerySpec querySpec, LockOptions lockOptions) {
		if ( lockOptions == null ) {
			return NON_CLAUSE_STRATEGY;
		}
		validateSpannerLockTimeout( lockOptions.getTimeOut() );
		return super.getLockingClauseStrategy( querySpec, lockOptions );
	}

	private static void validateSpannerLockTimeout(int millis) {
		if ( Timeouts.isRealTimeout( millis ) ) {
			throw new UnsupportedOperationException( "Spanner does not support lock timeout." );
		}
		if ( millis == Timeouts.SKIP_LOCKED_MILLI ) {
			throw new UnsupportedOperationException( "Spanner does not support skip locked." );
		}
		if ( millis == Timeouts.NO_WAIT_MILLI ) {
			throw new UnsupportedOperationException( "Spanner does not support no wait." );
		}
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SpannerPostgreSQLSqlAstTranslator<T>( sessionFactory, statement );
			}
		};
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );

		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( FLOAT, columnType( FLOAT ), castType( FLOAT ), this )
						.withTypeCapacity( 24, "real" )
						.withTypeCapacity( 53, "double precision" )
						.build()
		);
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// Spanner doesn't support precision with the timestamp
			case TIME, TIMESTAMP, TIMESTAMP_UTC, TIMESTAMP_WITH_TIMEZONE -> "timestamp with time zone";
			case BLOB -> "bytea";
			case CLOB, NCLOB -> "character varying";
			// Spanner doesn't support NUMERIC with precision and scale
			case NUMERIC ->  "numeric";
			case DECIMAL ->  "decimal";
			// Spanner doesn't support CHAR so we should use VARCHAR
			case CHAR -> columnType( VARCHAR );
			case SMALLINT, INTEGER, TINYINT ->  columnType( BIGINT );
			default -> super.columnType(sqlTypeCode);
		};
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	public boolean supportsUserDefinedTypes() {
		return false;
	}

	@Override
	public boolean supportsFilterClause() {
		return false;
	}

	@Override
	public boolean supportsRecursiveCycleUsingClause() {
		return false;
	}

	@Override
	public boolean supportsRecursiveSearchClause() {
		return false;
	}

	@Override
	public boolean supportsUniqueConstraints() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorGtLtSyntax() {
		return false;
	}

	// ALL subqueries with operators other than <>/!= are not supported
	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInSubQuery() {
		return false;
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return false;
	}

	@Override
	public String currentTimestamp() {
		return currentTimestampWithTimeZone();
	}

	@Override
	public String currentTime() {
		return currentTimestampWithTimeZone();
	}

	@Override
	public boolean supportsLateral() {
		return false;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return false;
	}

	@Override
	public int getMaxVarcharLength() {
		return 2_621_440;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//max is equivalent 10 MiB
		return 10_485_760;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "";
	}

	@Override
	public boolean supportsCommentOn() {
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return false;
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		// Cloud Spanner requires the referenced columns to specified in all cases, including
		// if the foreign key is referencing the primary key of the referenced table. Setting referencesPrimaryKey to
		// false will add all the referenced columns.
		return super.getAddForeignKeyConstraintString( constraintName, foreignKey, referencedTable, primaryKey, false );
	}

	@Override
	public boolean canBatchTruncate() {
		return false;
	}

	@Override
	public String rowId(String rowId) {
		return null;
	}

	@Override
	public boolean supportsRowConstructor() {
		return false;
	}

	@Override
	public String getTruncateTableStatement(String tableName) {
		return "delete from " + tableName;
	}

	@Override
	public String getBeforeDropStatement() {
		return null;
	}

	@Override
	public String getCascadeConstraintsString() {
		return "";
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return false;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return false;
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return false;
	}

	@Override
	public boolean supportsPartitionBy() {
		return false;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return false;
	}

	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	public boolean supportsNestedWithClause() {
		return false;
	}

	public boolean supportsCteHeaderColumnList() {
		return false;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new PersistentTableMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new PersistentTableInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return null;
	}

	@Override
	public @Nullable TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return null;
	}

	@Override
	public TemporaryTableStrategy getPersistentTemporaryTableStrategy() {
		return new PersistentTemporaryTableStrategy( this );
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return StandardCallableStatementSupport.NO_REF_CURSOR_INSTANCE;
	}
}
