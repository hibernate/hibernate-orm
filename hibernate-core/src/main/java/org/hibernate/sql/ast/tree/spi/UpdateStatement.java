/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.produce.internal.SqlAstUpdateDescriptorImpl;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class UpdateStatement implements MutationStatement {
	private final TableReference targetTable;
	private final List<Assignment> assignments;
	private final Predicate restriction;

	public UpdateStatement(
			TableReference targetTable,
			List<Assignment> assignments,
			Predicate restriction) {
		this.targetTable = targetTable;
		this.assignments = assignments;
		this.restriction = restriction;
	}

	public TableReference getTargetTable() {
		return targetTable;
	}

	public List<Assignment> getAssignments() {
		return assignments;
	}

	public Predicate getRestriction() {
		return restriction;
	}


	public static class UpdateStatementBuilder {
		private final TableReference targetTable;
		private List<Assignment> assignments;
		private Predicate restriction;

		public UpdateStatementBuilder(TableReference targetTable) {
			this.targetTable = targetTable;
		}

		public UpdateStatementBuilder addAssignment(Assignment assignment) {
			if ( assignments == null ) {
				assignments = new ArrayList<>();
			}
			assignments.add( assignment );

			return this;
		}

		public UpdateStatementBuilder addRestriction(Predicate restriction) {
			this.restriction = SqlAstHelper.combinePredicates( this.restriction, restriction );
			return this;
		}

		public UpdateStatementBuilder setRestriction(Predicate restriction) {
			this.restriction = restriction;
			return this;
		}

		public SqlAstUpdateDescriptor createUpdateDescriptor() {
			final UpdateStatement sqlAst = new UpdateStatement(
					targetTable,
					assignments != null ? assignments : Collections.emptyList(),
					restriction != null ? restriction : new Junction( Junction.Nature.CONJUNCTION )
			);
			return new SqlAstUpdateDescriptorImpl(
					sqlAst,
					Collections.singleton( targetTable.getTable().getTableExpression() )
			);
		}
	}
}
