/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Access to all of the externalized JDBC parameter bindings
 *
 * @apiNote "Externalized" because some JDBC parameter values are
 * intrinsically part of the parameter itself and we do not need to
 * locate a JdbcParameterBinding.  E.g., consider a
 * {@link org.hibernate.sql.ast.tree.spi.expression.LiteralParameter}
 * which actually encapsulates the actually literal value inside
 * itself - to create the binder and actually perform the binding
 * is only dependent on the LiteralParameter
 *
 * @author Steve Ebersole
 */
public interface JdbcParameterBindings {
	void addBinding(JdbcParameter parameter, JdbcParameterBinding binding);

	Collection<JdbcParameterBinding> getBindings();

	JdbcParameterBinding getBinding(JdbcParameter parameter);

	void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action);
}
