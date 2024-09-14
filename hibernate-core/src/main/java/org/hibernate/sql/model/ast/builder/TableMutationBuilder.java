/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableMutation;

/**
 * Generalized contract for building {@link TableMutation} instances
 *
 * @author Steve Ebersole
 */
public interface TableMutationBuilder<M extends TableMutation<?>> {
	/**
	 * Constant for `null`
	 */
	String NULL = "null";

	/**
	 * Reference (in the SQL AST sense) to the mutating table
	 */
	MutatingTableReference getMutatingTable();

	/**
	 * Build the mutation descriptor
	 */
	M buildMutation();
}
