/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.AbstractColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup
		extends AbstractColumnReferenceQualifier
		implements TableGroup, ColumnReferenceQualifier {

	private final TableSpace tableSpace;

	public AbstractTableGroup(TableSpace tableSpace, String uid) {
		super( uid );
		this.tableSpace = tableSpace;
	}

	@Override
	public TableSpace getTableSpace() {
		return tableSpace;
	}

	protected void renderTableReference(TableReference tableBinding, SqlAppender sqlAppender, SqlAstWalker walker) {
		sqlAppender.appendSql( tableBinding.getTable().getTableExpression() + " as " + tableBinding.getIdentificationVariable() );
	}

	@Override
	public ColumnReference locateColumnReferenceByName(String name) {
		Column column = getPrimaryTableReference().getTable().getColumn( name );
		if ( column == null ) {
			for ( TableReferenceJoin join : getTableReferenceJoins() ) {
				column = join.getJoinedTableBinding().getTable().getColumn( name );
				if ( column != null ) {
					break;
				}
			}
		}

		if ( column == null ) {
			return null;
		}

		return resolveColumnReference( column );
	}
}
