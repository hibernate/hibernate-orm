/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.dialect.DB2zDialect.DB2_LUW_VERSION;

/**
 * A SQL AST translator for DB2z.
 *
 * @author Christian Beikov
 */
public class DB2zSqlAstTranslator<T extends JdbcOperation> extends DB2SqlAstTranslator<T> {

	private final DatabaseVersion version;

	public DB2zSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, DatabaseVersion version) {
		super( sessionFactory, statement );
		this.version = version;
	}

	@Override
	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Percent fetches or ties fetches aren't supported in DB2 z/OS
		// Also, variable limit isn't supported before 12.0
		return getQueryPartForRowNumbering() != queryPart && ( useOffsetFetchClause( queryPart ) && !isRowsOnlyFetchClauseType( queryPart ) );
	}

	@Override
	protected boolean supportsOffsetClause() {
		return version.isSameOrAfter(12);
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		// Supported at least since DB2 z/OS 9.0
		renderComparisonStandard( lhs, operator, rhs );
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		// DB2 z/OS we need the "table" qualifier for table valued functions or lateral sub-queries
		append( "table " );
		super.visitQueryPartTableReference( tableReference );
	}

	@Override
	protected String getNewTableChangeModifier() {
		// On DB2 zOS, `final` also sees the trigger data
		return "final";
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION;
	}
}
