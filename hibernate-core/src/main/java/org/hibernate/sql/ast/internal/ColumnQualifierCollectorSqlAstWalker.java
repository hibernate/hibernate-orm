/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;

public class ColumnQualifierCollectorSqlAstWalker extends AbstractSqlAstWalker {

	private final Set<String> columnQualifiers = new HashSet<>();

	public static Set<String> determineColumnQualifiers(SqlAstNode node) {
		final ColumnQualifierCollectorSqlAstWalker walker = new ColumnQualifierCollectorSqlAstWalker();
		node.accept( walker );
		return walker.columnQualifiers;
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		if ( columnReference.getQualifier() != null ) {
			columnQualifiers.add( columnReference.getQualifier() );
		}
	}
}
