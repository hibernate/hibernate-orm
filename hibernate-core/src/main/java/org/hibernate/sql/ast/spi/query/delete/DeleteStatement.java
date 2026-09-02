/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.delete;

import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.spi.query.cte.CteContainer;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.spi.mutation.MutationTarget;

/**
 * @author Steve Ebersole
 */
public class DeleteStatement extends AbstractUpdateOrDeleteStatement {

	public static final String DEFAULT_ALIAS = "to_delete_";

	public DeleteStatement(NamedTableReference targetTable, Predicate restriction) {
		this( null, targetTable, null, new FromClause(), restriction, Collections.emptyList() );
	}

	public DeleteStatement(
			NamedTableReference targetTable,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, null, new FromClause(), restriction, returningColumns );
	}

	public DeleteStatement(NamedTableReference targetTable, FromClause fromClause, Predicate restriction) {
		this( null, targetTable, null, fromClause, restriction, Collections.emptyList() );
	}

	public DeleteStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, null, fromClause, restriction, returningColumns );
	}

	public DeleteStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			MutationTarget mutationTarget,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( cteContainer, targetTable, mutationTarget, fromClause, restriction, returningColumns );
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitDeleteStatement( this );
	}
}
