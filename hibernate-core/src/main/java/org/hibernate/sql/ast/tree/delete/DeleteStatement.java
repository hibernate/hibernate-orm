/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.delete;

import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class DeleteStatement extends AbstractUpdateOrDeleteStatement {

	public static final String DEFAULT_ALIAS = "to_delete_";

	public DeleteStatement(NamedTableReference targetTable, Predicate restriction) {
		this( null, targetTable, new FromClause(), restriction, Collections.emptyList() );
	}

	public DeleteStatement(
			NamedTableReference targetTable,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, new FromClause(), restriction, returningColumns );
	}

	public DeleteStatement(NamedTableReference targetTable, FromClause fromClause, Predicate restriction) {
		this( null, targetTable, fromClause, restriction, Collections.emptyList() );
	}

	public DeleteStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, fromClause, restriction, returningColumns );
	}

	public DeleteStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( cteContainer, targetTable, fromClause, restriction, returningColumns );
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitDeleteStatement( this );
	}
}
