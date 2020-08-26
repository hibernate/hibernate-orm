/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * Consumer used to visit columns for a given model part
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ColumnConsumer {
	void accept(
			String containingTableExpression,
			String columnExpression,
			boolean isFormula,
			String customReadExpression,
			String customWriteExpression,
			JdbcMapping jdbcMapping);
}
