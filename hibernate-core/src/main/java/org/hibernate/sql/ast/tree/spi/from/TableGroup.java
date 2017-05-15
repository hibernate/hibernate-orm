/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * Group together related {@link TableReference} references (generally related by EntityPersister or CollectionPersister),
 *
 * @author Steve Ebersole
 */
public interface TableGroup extends ColumnReferenceSource {
	/**
	 * Get the TableSpace that contains this group.  Allows walking "up"
	 * the tree.
	 */
	TableSpace getTableSpace();

	/**
	 * Retrieve the Expression representation of this TableGroup.  This
	 * Expression can then be used in other clauses to refer to this
	 * TableGroup
	 */
	NavigableReference asExpression();

	/**
	 * Perform rendering of this group into the passed SQL appender.
	 */
	void render(SqlAppender sqlAppender, SqlSelectAstWalker walker);
}
