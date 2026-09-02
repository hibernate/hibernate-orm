/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingRequest;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTupleContainer;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.community.dialect.DB2iLegacyDialect.DB2_LUW_VERSION9;


/**
 * A SQL AST translator for DB2i.
 *
 * @author Christian Beikov
 */
public class DB2iLegacySqlAstTranslator<T extends JdbcOperation> extends DB2LegacySqlAstTranslator<T> {

	private final DatabaseVersion version;

	public DB2iLegacySqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request, DatabaseVersion version) {
		super( request );
		this.version = version;
	}

	@Override
	protected boolean requiresWindowPaginationForVariableLimit(PaginationRenderingRequest request) {
		final var queryPart = request.queryPart();
		// According to LegacyDB2LimitHandler, variable limit also isn't supported before 7.1
		return version.isBefore( 7, 1 )
				&& ( request.usesQueryOptionsLimit()
						|| queryPart.getFetchClauseExpression() != null
								&& !( queryPart.getFetchClauseExpression() instanceof Literal ) );
	}

	@Override
	protected boolean supportsOffsetClause() {
		return version.isSameOrAfter( 7, 1 );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonStandard( lhs, operator, rhs );
	}

	@Override
	protected void renderExpressionsAsValuesSubquery(int tupleSize, List<Expression> listExpressions) {
		// DB2 for i supports type-inference in this special VALUES expression, but not if it's wrapped as SELECT
		appendSql( "values" );
		char separator = ' ';
		for ( Expression expression : listExpressions ) {
			appendSql( separator );
			appendSql( OPEN_PARENTHESIS );
			renderCommaSeparated( SqlTupleContainer.getSqlTuple( expression ).getExpressions() );
			appendSql( CLOSE_PARENTHESIS );
			separator = ',';
		}
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION9;
	}
}
