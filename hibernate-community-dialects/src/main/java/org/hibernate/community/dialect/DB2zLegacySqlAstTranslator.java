/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.Internal;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardReturningRenderingSupport;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.community.dialect.DB2zLegacyDialect.DB2_LUW_VERSION9;


/**
 * A SQL AST translator for DB2z.
 *
 * @author Christian Beikov
 */
public class DB2zLegacySqlAstTranslator<T extends JdbcOperation> extends DB2LegacySqlAstTranslator<T> {

	private final DatabaseVersion version;

	public DB2zLegacySqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request, DatabaseVersion version) {
		super( request );
		this.version = version;
	}

	@Override
	protected boolean requiresWindowPaginationForVariableLimit(PaginationRenderingRequest request) {
		final var queryPart = request.queryPart();
		return version.isBefore( 12 )
				&& ( request.usesQueryOptionsLimit()
						|| queryPart.getFetchClauseExpression() != null
								&& !( queryPart.getFetchClauseExpression() instanceof Literal ) );
	}

	@Override
	protected boolean supportsOffsetClause() {
		return version.isSameOrAfter( 12 );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		// Supported at least since DB2 z/OS 9.0
		renderComparisonStandard( lhs, operator, rhs );
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.DB2_ZOS;
	}

	@Override
	@Internal
	protected ReturningRenderingSupport getReturningRenderingSupport() {
		return StandardReturningRenderingSupport.DB2_ZOS;
	}

	@Override
	protected boolean preferUnionQueryForTupleInListPredicate() {
		// DB2 z/OS can't use an index when rendering a union query
		return false;
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION9;
	}
}
