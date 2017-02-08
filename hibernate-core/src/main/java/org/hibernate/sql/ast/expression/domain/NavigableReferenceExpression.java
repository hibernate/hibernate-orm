/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.expression.domain;

import java.util.List;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.from.ColumnBinding;

/**
 * An expression that is a reference to some part of the application domain model.
 *
 * @author Steve Ebersole
 */
public interface NavigableReferenceExpression extends Expression {
	/**
	 * Get the Navigable referenced by this expression
	 *
	 * @return The Navigable
	 */
	Navigable getNavigable();

	NavigablePath getNavigablePath();

	List<ColumnBinding> getColumnBindings();
}
