/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.insert;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;

/**
 * Specialization of MutationStatement for inserts
 *
 * @author Steve Ebersole
 */
public interface InsertStatement extends MutationStatement {
	/**
	 * Get all target columns
	 */
	List<ColumnReference> getTargetColumns();

	/**
	 * The number of target columns associated with this insert.
	 *
	 * @implNote By default, returns the size of {@link #getTargetColumns()}
	 * which may be appropriate or not
	 */
	default int getNumberOfTargetColumns() {
		return getTargetColumns().size();
	}

	/**
	 * Iterates each target column
	 */
	void forEachTargetColumn(BiConsumer<Integer, ColumnReference> consumer);
}
