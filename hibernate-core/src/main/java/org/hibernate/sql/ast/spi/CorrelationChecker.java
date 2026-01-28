/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * A walker that checks if a SQL AST node contains a subquery that references a specific table alias.
 *
 * @author Yoobin Yoon
 */
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
