/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NavigablePath;

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
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		assert tableExpression != null;

		final TableReference tableReference = getTableReferenceInternal(
				navigablePath,
				tableExpression,
				allowFkOptimization
		);
		if ( tableReference == null ) {
			throw new IllegalStateException( "Could not resolve binding for table `" + tableExpression + "`" );
		}

		return tableReference;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		return getTableReferenceInternal( navigablePath, tableExpression, allowFkOptimization );
	}

	protected TableReference getTableReferenceInternal(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		final TableReference primaryTableReference = getPrimaryTableReference().getTableReference(
				navigablePath,
				tableExpression,
				allowFkOptimization
		);
		if ( primaryTableReference != null) {
			return primaryTableReference;
		}

		for ( TableReferenceJoin tableJoin : getTableReferenceJoins() ) {
			final TableReference tableReference = tableJoin.getJoinedTableReference().getTableReference(
					navigablePath,
					tableExpression,
					allowFkOptimization
			);
			if ( tableReference != null) {
				return tableReference;
			}
		}

		return null;
	}

}
