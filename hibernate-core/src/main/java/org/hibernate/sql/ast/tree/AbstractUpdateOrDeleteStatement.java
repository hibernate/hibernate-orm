/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree;

import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;

public abstract class AbstractUpdateOrDeleteStatement extends AbstractMutationStatement {
	private final FromClause fromClause;
	private final Predicate restriction;

	public AbstractUpdateOrDeleteStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction) {
		this( null, targetTable, fromClause, restriction, Collections.emptyList() );
	}

	public AbstractUpdateOrDeleteStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, fromClause, restriction, returningColumns );
	}

	public AbstractUpdateOrDeleteStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( cteContainer, targetTable, returningColumns );
		this.fromClause = fromClause;
		this.restriction = restriction;
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	public Predicate getRestriction() {
		return restriction;
	}
}
