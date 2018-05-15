/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class DeleteStatement implements MutationStatement {
	private final TableReference targetTable;
	private final Predicate restriction;

	public DeleteStatement(TableReference targetTable, Predicate restriction) {
		this.targetTable = targetTable;
		this.restriction = restriction;
	}

	public TableReference getTargetTable() {
		return targetTable;
	}

	public Predicate getRestriction() {
		return restriction;
	}

	public static class DeleteStatementBuilder {
		private final TableReference targetTable;
		private Predicate restriction;

		public DeleteStatementBuilder(TableReference targetTable) {
			this.targetTable = targetTable;
		}

		public DeleteStatementBuilder addRestriction(Predicate restriction) {
			this.restriction = SqlAstHelper.combinePredicates( this.restriction, restriction );
			return this;
		}

		public DeleteStatementBuilder setRestriction(Predicate restriction) {
			this.restriction = restriction;
			return this;
		}

		public DeleteStatement createDeleteStatement() {
			return new DeleteStatement(
					targetTable,
					restriction != null ? restriction : new Junction( Junction.Nature.CONJUNCTION )
			);
		}
	}
}
