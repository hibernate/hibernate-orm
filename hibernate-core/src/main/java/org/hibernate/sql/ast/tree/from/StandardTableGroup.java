/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.expression.ColumnReference;

/**
 * @author Steve Ebersole
 */
public class StandardTableGroup extends AbstractTableGroup {
	private final TableReference primaryTableReference;
	private final List<TableReferenceJoin> tableReferenceJoins;

	private final ColumnReferenceQualifier additionalQualifier;

	public StandardTableGroup(
			NavigablePath navigablePath,
			Navigable<?> navigable,
			LockMode lockMode,
			TableReference primaryTableReference,
			List<TableReferenceJoin> tableReferenceJoins) {
		this( navigablePath, navigable, lockMode, primaryTableReference, tableReferenceJoins, null );
	}

	public StandardTableGroup(
			NavigablePath navigablePath,
			Navigable navigable,
			LockMode lockMode,
			TableReference primaryTableReference,
			List<TableReferenceJoin> tableReferenceJoins,
			ColumnReferenceQualifier additionalQualifier) {
		super( navigablePath, navigable, lockMode );

		this.primaryTableReference = primaryTableReference;
		this.tableReferenceJoins = tableReferenceJoins;
		this.additionalQualifier = additionalQualifier;
	}

	@Override
	protected TableReference getPrimaryTableReference() {
		return primaryTableReference;
	}

	@Override
	protected List<TableReferenceJoin> getTableReferenceJoins() {
		return tableReferenceJoins;
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( primaryTableReference.getTable().getTableExpression() );
		for ( TableReferenceJoin tableReferenceJoin : tableReferenceJoins ) {
			nameCollector.accept( tableReferenceJoin.getJoinedTableReference().getTable().getTableExpression() );
		}
	}

	@Override
	public TableReference locateTableReference(Table table) {
		// todo (6.0) : here is where we could consider dynamically determining which tables references are needed.
		//		- we'd always have to add non-optional tables

		if ( table == primaryTableReference.getTable() ) {
			return primaryTableReference;
		}

//		if ( primaryTableReference.getTable() instanceof UnionSubclassTable ) {
//			if ( ( (UnionSubclassTable) primaryTableReference.getTable() ).includes( table ) ) {
//				return primaryTableReference;
//			}
//		}

		if ( tableReferenceJoins != null ) {
			for ( TableReferenceJoin tableJoin : tableReferenceJoins ) {
				if ( tableJoin.getJoinedTableReference().getTable() == table ) {
					return tableJoin.getJoinedTableReference();
				}
			}
		}

		if ( additionalQualifier != null ) {
			return additionalQualifier.locateTableReference( table );
		}

		return null;
//		throw new IllegalStateException( "Could not resolve binding for table : " + table );
	}

	@Override
	public ColumnReference resolveColumnReference(Column column) {
		return super.resolveColumnReference( column );
	}

	@Override
	public Column resolveColumn(String name) {
		// check the primary table first
		Column column = getPrimaryTableReference().getTable().getColumn( name );

		if ( column == null ) {
			// not found - check joined tables
			for ( TableReferenceJoin join : getTableReferenceJoins() ) {
				column = join.getJoinedTableReference().getTable().getColumn( name );
				if ( column != null ) {
					break;
				}
			}
		}

		if ( column == null && additionalQualifier != null ) {
			column = additionalQualifier.resolveColumn( name );
		}

		return column;
	}

	@Override
	public ColumnReference locateColumnReferenceByName(String name) {
		final Column column = resolveColumn( name );

		if ( column == null ) {
			return null;
		}

		return resolveColumnReference( column );
	}

	@Override
	public void render(SqlAppender sqlAppender, SqlAstWalker walker) {
		renderTableReference( primaryTableReference, sqlAppender, walker );

		if ( tableReferenceJoins != null ) {
			for ( TableReferenceJoin tableJoin : tableReferenceJoins ) {
				sqlAppender.appendSql( " " );
				sqlAppender.appendSql( tableJoin.getJoinType().getText() );
				sqlAppender.appendSql( " join " );
				renderTableReference( tableJoin.getJoinedTableReference(), sqlAppender, walker );
				if ( tableJoin.getJoinPredicate() != null && !tableJoin.getJoinPredicate().isEmpty() ) {
					sqlAppender.appendSql( " on " );
					tableJoin.getJoinPredicate().accept( walker );
				}
			}
		}
	}
}
