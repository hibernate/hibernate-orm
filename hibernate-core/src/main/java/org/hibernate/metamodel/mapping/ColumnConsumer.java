/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ColumnConsumer {
	// todo (6.0) : pass values `updateable`, `checkable`, etc
	void accept(
			String containingTableExpression,
			String columnExpression,
			JdbcMapping jdbcMapping);
}
