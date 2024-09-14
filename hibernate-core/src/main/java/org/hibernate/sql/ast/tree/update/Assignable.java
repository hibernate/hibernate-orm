/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.update;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.sql.ast.tree.expression.ColumnReference;

/**
 * @author Steve Ebersole
 */
public interface Assignable {
	List<ColumnReference> getColumnReferences();

	default void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		getColumnReferences().forEach( columnReferenceConsumer );
	}

}
