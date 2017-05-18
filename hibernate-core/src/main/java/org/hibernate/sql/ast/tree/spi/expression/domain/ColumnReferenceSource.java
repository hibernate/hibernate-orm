/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableReference;

/**
 * Defines a "source" for ColumnReferences related to a DomainReference (relative
 * to this source).
 *
 * @author Steve Ebersole
 */
public interface ColumnReferenceSource {
	/**
	 * Get the unique (within this query) identifier for this group of tables.
	 * Think primary-key; other code which knows this uid can locate it through
	 * {@link org.hibernate.metamodel.queryable.spi.TableGroupResolver}.
	 */
	String getUniqueIdentifier();

	TableReference locateTableReference(Table table);
	ColumnReference resolveColumnReference(Column column);
}
