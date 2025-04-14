/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.SqlParameterInfo;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.community.dialect.DB2iLegacyDialect.DB2_LUW_VERSION9;


/**
 * A SQL AST translator for DB2i.
 *
 * @author Christian Beikov
 */
public class DB2iLegacySqlAstTranslator<T extends JdbcOperation> extends DB2LegacySqlAstTranslator<T> {

	private final DatabaseVersion version;

	@Deprecated(forRemoval = true, since = "7.1")
	public DB2iLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, DatabaseVersion version) {
		super( sessionFactory, statement );
		this.version = version;
	}

	public DB2iLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, @Nullable SqlParameterInfo parameterInfo, DatabaseVersion version) {
		super( sessionFactory, statement, parameterInfo );
		this.version = version;
	}

	@Override
	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		if ( getQueryPartForRowNumbering() == queryPart ) {
			return false;
		}
		// Percent fetches or ties fetches aren't supported in DB2
		if ( useOffsetFetchClause( queryPart ) && !isRowsOnlyFetchClauseType( queryPart ) ) {
			return true;
		}
		// According to LegacyDB2LimitHandler, variable limit also isn't supported before 7.1
		return version.isBefore(7, 1)
				&& queryPart.getFetchClauseExpression() != null
				&& !( queryPart.getFetchClauseExpression() instanceof Literal );
	}

	@Override
	protected boolean supportsOffsetClause() {
		return version.isSameOrAfter(7, 1);
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonStandard( lhs, operator, rhs );
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION9;
	}
}
