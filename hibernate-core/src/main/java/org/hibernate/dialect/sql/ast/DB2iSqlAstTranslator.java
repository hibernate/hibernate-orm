/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.exec.spi.JdbcOperation;

import java.util.List;

import static org.hibernate.dialect.DB2iDialect.DB2_LUW_VERSION;

/**
 * A SQL AST translator for DB2i.
 *
 * @author Christian Beikov
 */
public class DB2iSqlAstTranslator<T extends JdbcOperation> extends DB2SqlAstTranslator<T> {

	private final DatabaseVersion version;

	public DB2iSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, DatabaseVersion version) {
		super( sessionFactory, statement );
		this.version = version;
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
		return DB2_LUW_VERSION;
	}

}
