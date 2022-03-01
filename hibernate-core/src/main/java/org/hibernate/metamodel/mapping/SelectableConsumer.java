/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * Consumer used to visit selectable (column/formula) mappings
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface SelectableConsumer {
	/**
	 * Accept the selectable mapping.  `selectIndex` is its position,
	 * the meaning of which depends on the impl and whether
	 * {@link SelectableMappings#forEachSelectable(SelectableConsumer)} or
	 * {@link SelectableMappings#forEachSelectable(int, SelectableConsumer)}
	 * was used
	 */
	void accept(int selectionIndex, SelectableMapping selectableMapping);

	/**
	 * Simple form allowing visitation over a number of column names within a
	 * table.
	 *
	 * Very limited functionality in terms of the visited SelectableMappings
	 * will not have any defined JdbcMapping, etc
	 */
	default void accept(String tableName, String[] columnNames) {
		class SelectableMappingIterator implements SelectableMapping {

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
			public String getColumnDefinition() {
				return null;
			}

			@Override
			public Long getLength() {
				return null;
			}

			@Override
			public Integer getPrecision() {
				return null;
			}

			@Override
			public Integer getScale() {
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
				SelectableMappingIterator iterator = new SelectableMappingIterator();
				iterator.index < columnNames.length;
				iterator.index++
		) {
			accept( iterator.index, iterator );
		}
	}
}
