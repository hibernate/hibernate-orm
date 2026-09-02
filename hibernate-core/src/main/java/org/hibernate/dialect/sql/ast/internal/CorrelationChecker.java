/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.internal;

import org.hibernate.Internal;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;

/**
 * A walker that checks if a SQL AST node contains a subquery that references a specific table alias.
 *
 * @author Yoobin Yoon
 */
@Internal
public class CorrelationChecker extends AbstractSqlAstWalker {

	private final String targetAlias;
	private boolean inSubquery = false;

	private static class CorrelationException extends RuntimeException {
		@Override
		public Throwable fillInStackTrace() {
			return this;
		}
	}

	public static boolean hasCorrelation(SqlAstNode node, String targetAlias) {
		try {
			node.accept( new CorrelationChecker( targetAlias ) );
			return false;
		}
		catch (CorrelationException ex) {
			return true;
		}
	}

	public static boolean hasCorrelation(MutationStatement statement, String targetAlias) {
		try {
			if ( statement instanceof DeleteStatement deleteStatement ) {
				if ( deleteStatement.getRestriction() != null ) {
					deleteStatement.getRestriction().accept( new CorrelationChecker( targetAlias ) );
				}
			}
			else if ( statement instanceof UpdateStatement updateStatement ) {
				final CorrelationChecker visitor = new CorrelationChecker( targetAlias );
				if ( updateStatement.getAssignments() != null ) {
					for ( var assignment : updateStatement.getAssignments() ) {
						assignment.getAssignedValue().accept( visitor );
					}
				}
				if ( updateStatement.getRestriction() != null ) {
					updateStatement.getRestriction().accept( visitor );
				}
			}
			return false;
		}
		catch (CorrelationException ex) {
			return true;
		}
	}

	private CorrelationChecker(String targetAlias) {
		this.targetAlias = targetAlias;
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		boolean wasInSubquery = inSubquery;
		inSubquery = true;
		super.visitQuerySpec( querySpec );
		inSubquery = wasInSubquery;
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		if ( inSubquery && targetAlias.equals( columnReference.getQualifier() ) ) {
			throw new CorrelationException();
		}
		super.visitColumnReference( columnReference );
	}
}
