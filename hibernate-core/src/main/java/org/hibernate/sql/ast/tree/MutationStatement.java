/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree;

import java.util.List;

import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;

/**
 * Specialization of Statement for mutation (DML) statements
 *
 * @author Steve Ebersole
 */
public interface MutationStatement extends Statement {
	NamedTableReference getTargetTable();
	List<ColumnReference> getReturningColumns();
}
