/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.sql.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;

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
	public TableReference resolveTableReference(String tableExpression, Supplier<TableReference> creator) {
		final TableReference existing = resolveTableReference( tableExpression );
		if ( existing != null ) {
			return existing;
		}

		return creator.get();
	}

	@Override
	public TableReference resolveTableReference(String tableExpression) {
		assert tableExpression != null;

		if ( getPrimaryTableReference().getTableExpression().equals( tableExpression ) ) {
			return getPrimaryTableReference();
		}

		for ( TableReferenceJoin tableJoin : getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableReference().getTableExpression() == tableExpression ) {
				return tableJoin.getJoinedTableReference();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + tableExpression + "`" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ColumnReference handling

	private final SortedMap<String, ColumnReference> columnReferenceMap = new TreeMap<>();

	@Override
	public ColumnReference resolveColumnReference(
			String tableExpression,
			String columnExpression,
			Supplier<ColumnReference> creator) {
		return columnReferenceMap.computeIfAbsent(
				SqlExpressionResolver.createColumnReferenceKey( tableExpression, columnExpression ),
				s -> creator.get()
		);
	}

	@Override
	public ColumnReference resolveColumnReference(String tableExpression, String columnExpression) {
		return columnReferenceMap.get(
				SqlExpressionResolver.createColumnReferenceKey( tableExpression, columnExpression )
		);
	}
}
