/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractColumnReferenceQualifier implements ColumnReferenceQualifier {
	protected abstract TableReference getPrimaryTableReference();

	protected abstract List<TableReferenceJoin> getTableReferenceJoins();

	protected abstract SessionFactoryImplementor getSessionFactory();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TableReference handling

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		final TableReference primaryTableReference = getPrimaryTableReference().getTableReference(
				navigablePath,
				tableExpression,
				resolve
		);
		if ( primaryTableReference != null) {
			return primaryTableReference;
		}

		for ( TableReferenceJoin tableJoin : getTableReferenceJoins() ) {
			final TableReference tableReference = tableJoin.getJoinedTableReference().getTableReference(
					navigablePath,
					tableExpression,
					resolve
			);
			if ( tableReference != null) {
				return tableReference;
			}
		}

		return null;
	}

}
