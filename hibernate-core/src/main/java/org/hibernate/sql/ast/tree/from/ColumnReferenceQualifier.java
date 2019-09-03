/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.function.Supplier;

import org.hibernate.sql.ast.tree.expression.ColumnReference;

/**
 * @author Steve Ebersole
 */
public interface ColumnReferenceQualifier {
	TableReference resolveTableReference(String tableExpression, Supplier<TableReference> creator);
	TableReference resolveTableReference(String tableExpression);

	ColumnReference resolveColumnReference(String tableExpression, String columnExpression, Supplier<ColumnReference> creator);
	ColumnReference resolveColumnReference(String tableExpression, String columnExpression);
}
