/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.internal.util.Loggable;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * Group together related {@link TableReference} references (generally related by EntityPersister or CollectionPersister),
 *
 * @author Steve Ebersole
 */
public interface TableGroup extends ColumnReferenceQualifier, SqlAstNode, Loggable {
	/**
	 * Get the TableSpace that contains this group.  Allows walking "up"
	 * the tree.
	 */
	TableSpace getTableSpace();

	NavigableReference getNavigableReference();

	/**
	 * Perform rendering of this group into the passed SQL appender.
	 */
	void render(SqlAppender sqlAppender, SqlAstWalker walker);

	@Override
	default void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitTableGroup( this );
	}

	default void applyAffectedTableNames(Consumer<String> nameCollector) {
		throw new NotYetImplementedFor6Exception();
	}

	default ColumnReference locateColumnReferenceByName(String name) {
		throw new UnsupportedOperationException(
				"Cannot call #locateColumnReferenceByName on this type of TableGroup"
		);
	}
}
