/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.internal;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.DB2SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardReturningRenderingSupport;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.dialect.DB2zDialect.DB2_LUW_VERSION;

/**
 * A SQL AST translator for DB2z.
 *
 * @author Christian Beikov
 */
public class DB2zSqlAstTranslator<T extends JdbcOperation> extends DB2SqlAstTranslator<T> {

	private final DatabaseVersion version;

	public DB2zSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request, DatabaseVersion version) {
		super( request );
		this.version = version;
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
		return DB2_LUW_VERSION;
	}
}
