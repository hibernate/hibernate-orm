/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;


import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ElementColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.metamodel.spi.IndexColumnReferenceQualifier;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * A TableSpecificationGroup for a collection reference
 *
 * @author Steve Ebersole
 */
public class CollectionTableGroup implements TableGroup {
	private final PersistentCollectionDescriptor persister;

	private final TableSpace tableSpace;
	private final String uniqueIdentifier;
	private final TableReference elementTableReference;
	private final TableReference indexTableReference;

	public CollectionTableGroup(
			PersistentCollectionDescriptor persister,
			TableSpace tableSpace,
			String uniqueIdentifier,
			TableReference elementTableReference,
			TableReference indexTableReference) {
		this.persister = persister;
		this.tableSpace = tableSpace;
		this.uniqueIdentifier = uniqueIdentifier;
		this.elementTableReference = elementTableReference;
		this.indexTableReference = indexTableReference;
	}

	public PersistentCollectionDescriptor getPersister() {
		return persister;
	}

	@Override
	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	@Override
	public TableSpace getTableSpace() {
		return tableSpace;
	}

	@Override
	public NavigableReference asExpression() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public TableReference locateTableReference(Table table) {
		if ( table == elementTableReference.getTable() ) {
			return elementTableReference;
		}

		return null;
	}

	@Override
	public void render(SqlAppender sqlAppender, SqlAstWalker walker) {
		// todo (6.0) : need to determine which table (if 2) to render first
		//		(think many-to-many).  does the order of the joins matter given the serial join?

		throw new NotYetImplementedFor6Exception(  );

//		renderTableReference( rootTableReference, sqlAppender, walker );
//
//		for ( TableReferenceJoin tableJoin : tableReferenceJoins ) {
//			sqlAppender.appendSql( " " );
//			sqlAppender.appendSql( tableJoin.getJoinType().getText() );
//			sqlAppender.appendSql( " join " );
//			renderTableReference( tableJoin.getJoinedTableBinding(), sqlAppender, walker );
//			if ( tableJoin.getJoinPredicate() != null && !tableJoin.getJoinPredicate().isEmpty() ) {
//				sqlAppender.appendSql( " on " );
//				tableJoin.getJoinPredicate().accept( walker );
//			}
//		}
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
							") to ColumnBinding via TableGroup [" + this + "]"
			);
		}
		final ColumnReference columnBinding = new ColumnReference( this, column );
		columnBindingMap.put( column, columnBinding );
		return columnBinding;
	}

	@Override
	public Expression qualify(QualifiableSqlExpressable sqlSelectable) {
		throw new NotYetImplementedFor6Exception(  );
	}
}
