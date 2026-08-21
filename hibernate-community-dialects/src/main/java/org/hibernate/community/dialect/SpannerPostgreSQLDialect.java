/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.community.dialect.aggregate.SpannerPostgreSQLAggregateSupport;
import org.hibernate.community.dialect.sequence.SpannerPostgreSQLSequenceSupport;
import org.hibernate.community.dialect.sql.ast.SpannerPostgreSQLSqlAstTranslator;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.internal.StandardTableExporter;

public class SpannerPostgreSQLDialect extends PostgreSQLDialect {

	private final UniqueDelegate SPANNER_UNIQUE_DELEGATE = new AlterTableUniqueIndexDelegate( this );
	private final StandardTableExporter SPANNER_TABLE_EXPORTER = new SpannerPostgreSQLTableExporter( this );

	public SpannerPostgreSQLDialect() {
		super();
	}

	public SpannerPostgreSQLDialect(DialectResolutionInfo info) {
		super( info );
	}

	public SpannerPostgreSQLDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return SimpleDatabaseVersion.ZERO_VERSION;
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
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SpannerPostgreSQLSqlAstTranslator<T>( sessionFactory, statement );
			}
		};
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
	public boolean supportsNonQueryWithCTE() {
		return false;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return false;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return StandardCallableStatementSupport.NO_REF_CURSOR_INSTANCE;
	}
}
