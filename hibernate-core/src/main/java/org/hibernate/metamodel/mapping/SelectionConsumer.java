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
public interface SelectionConsumer {

	void accept(int selectionIndex, SelectionMapping selectionMapping);

	default void accept(String tableName, String[] columnNames) {
		class SelectionMappingIterator implements SelectionMapping {

			private int index;

			@Override
			public String getContainingTableExpression() {
				return tableName;
			}

			@Override
			public String getSelectionExpression() {
				return columnNames[index];
			}

			@Override
			public String getCustomReadExpression() {
				return null;
			}

			@Override
			public String getCustomWriteExpression() {
				return null;
			}

			@Override
			public boolean isFormula() {
				return false;
			}

			@Override
			public JdbcMapping getJdbcMapping() {
				return null;
			}
		}
		for (
				SelectionMappingIterator iterator = new SelectionMappingIterator();
				iterator.index < columnNames.length;
				iterator.index++
		) {
			accept( iterator.index, iterator );
		}
	}
}
