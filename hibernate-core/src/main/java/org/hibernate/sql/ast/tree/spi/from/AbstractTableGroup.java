/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import org.hibernate.persister.queryable.spi.AbstractColumnReferenceSource;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup
		extends AbstractColumnReferenceSource
		implements TableGroup, ColumnReferenceSource {

	private final TableSpace tableSpace;

	public AbstractTableGroup(TableSpace tableSpace, String uid) {
		super( uid );
		this.tableSpace = tableSpace;
	}

	@Override
	public TableSpace getTableSpace() {
		return tableSpace;
	}

	protected void renderTableReference(TableReference tableBinding, SqlAppender sqlAppender, SqlSelectAstWalker walker) {
		sqlAppender.appendSql( tableBinding.getTable().getTableExpression() + " as " + tableBinding.getIdentificationVariable() );
	}
}
