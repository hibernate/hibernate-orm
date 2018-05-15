/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractColumnReferenceQualifier implements ColumnReferenceQualifier {
	private final String uniqueIdentifier;

	public AbstractColumnReferenceQualifier(String uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	@Override
	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	protected abstract TableReference getPrimaryTableReference();

	protected abstract List<TableReferenceJoin> getTableReferenceJoins();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TableReference handling

	@Override
	public TableReference locateTableReference(Table table) {
		if ( table == getPrimaryTableReference().getTable() ) {
			return getPrimaryTableReference();
		}

//		if ( getPrimaryTableReference().getTable() instanceof UnionSubclassTable ) {
//			if ( ( (UnionSubclassTable) getPrimaryTableReference().getTable() ).includes( table ) ) {
//				return getPrimaryTableReference();
//			}
//		}

		for ( TableReferenceJoin tableJoin : getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableReference().getTable() == table ) {
				return tableJoin.getJoinedTableReference();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table : " + table );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ColumnReference handling

	private final SortedMap<Column,ColumnReference> columnBindingMap = new TreeMap<>(
			(column1, column2) -> {
				// Sort primarily on table expression
				final int tableSort = column1.getSourceTable().getTableExpression().compareTo( column2.getSourceTable().getTableExpression() );
				if ( tableSort != 0 ) {
					return tableSort;
				}

				// and secondarily on column expression
				return column1.getExpression().compareTo( column2.getExpression() );
			}
	);

	@Override
	public ColumnReference resolveColumnReference(Column column) {
		final ColumnReference existing = columnBindingMap.get( column );
		if ( existing != null ) {
			return existing;
		}

		final TableReference tableBinding = locateTableReference( column.getSourceTable() );
		if ( tableBinding == null ) {
			throw new HibernateException(
					"Problem resolving Column(" + column.toLoggableString() +
							") to ColumnBinding via TableGroup [" + loggableString() + "]"
			);
		}
		final ColumnReference columnBinding = new ColumnReference( tableBinding, column );
		columnBindingMap.put( column, columnBinding );
		return columnBinding;
	}

	private String loggableString() {
		return "primary table = ` " + getPrimaryTableReference().getTable() + '`';
	}

	@Override
	public ColumnReference qualify(QualifiableSqlExpressable sqlSelectable) {
		assert sqlSelectable instanceof Column;
		return resolveColumnReference( (Column) sqlSelectable );
	}
}
