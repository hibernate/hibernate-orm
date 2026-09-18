/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.internal.MutableSelectableMapping;

import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * Consumer used to visit selectable (column/formula) mappings
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
@FunctionalInterface
public interface SelectableConsumer {
	/**
	 * Accept the selectable mapping.  `selectIndex` is its position,
	 * the meaning of which depends on the impl and whether
	 * {@link SelectableMappings#forEachSelectable(SelectableConsumer)} or
	 * {@link SelectableMappings#forEachSelectable(int, SelectableConsumer)}
	 * was used
	 */
	void accept(int selectionIndex, @Nonnull SelectableMapping selectableMapping);

	/**
	 * Simple form of visitation over a number of columns by name, using
	 * a separate {@link JdbcMappingContainer} as a base for additional details.
	 * <p>
	 * Intended for use in visiting table keys, where we know JdbcMappings, etc.
	 * from the identifier.
	 * <p>
	 * The expectation here is for the following details to be available:<ul>
	 *     <li>{@link SelectableMapping#getContainingTableExpression()}</li>
	 *     <li>{@link SelectableMapping#getSelectionExpression()} (the column name)</li>
	 *     <li>{@link SelectableMapping#getWriteExpression()}</li>
	 *     <li>{@link SelectableMapping#getJdbcMapping()}</li>
	 * </ul>
	 */
	default void accept(@Nonnull String tableName, @Nonnull JdbcMappingContainer base, @Nonnull String[] columnNames) {
		assert base.getJdbcTypeCount() == columnNames.length;

		final MutableSelectableMapping mutableSelectableMapping = new MutableSelectableMapping( tableName, base, columnNames );
		mutableSelectableMapping.forEach( this::accept );
	}

	/**
	 * Simple form of visitation over a number of columns by name, using
	 * a separate {@link SelectableMappings} as a base for additional details.
	 * <p>
	 * Intended for use in visiting table keys, where we know JdbcMappings, etc.
	 * from the identifier.
	 * <p>
	 * The expectation here is for the following details to be available:<ul>
	 *     <li>{@link SelectableMapping#getContainingTableExpression()}</li>
	 *     <li>{@link SelectableMapping#getSelectionExpression()} (the column name)</li>
	 *     <li>{@link SelectableMapping#getWriteExpression()}</li>
	 *     <li>{@link SelectableMapping#getJdbcMapping()}</li>
	 * </ul>
	 */
	default void accept(@Nonnull SelectableMappings base, @Nonnull String tableName, @Nonnull String[] columnNames) {
		class SelectableMappingIterator implements SelectableMapping {
			private final String tableName;
			private final SelectableMappings delegate;
			private final String[] columnNames;

			private int index;

			public SelectableMappingIterator(@Nonnull String tableName, @Nonnull SelectableMappings delegate, @Nonnull String[] columnNames) {
				this.tableName = tableName;
				this.delegate = delegate;
				this.columnNames = columnNames;
				assert delegate.getJdbcTypeCount() == columnNames.length;
			}

			private void forEach(@Nonnull BiConsumer<Integer,SelectableMapping> consumer) {
				for ( index = 0; index < columnNames.length; index++ ) {
					consumer.accept( index, this );
				}
			}

			@Nonnull
			@Override
			public String getContainingTableExpression() {
				return tableName;
			}

			@Nonnull
			@Override
			public String getSelectionExpression() {
				return columnNames[index];
			}

			@Override
			public @Nullable String getCustomReadExpression() {
				return null;
			}

			@Override
			public @Nullable String getCustomWriteExpression() {
				return null;
			}

			@Nonnull
			private SelectableMapping getDelegate() {
				return delegate.getSelectable( index );
			}

			@Nonnull
			@Override
			public String getSelectableName() {
				return getDelegate().getSelectableName();
			}

			@Nonnull
			@Override
			public SelectablePath getSelectablePath() {
				return getDelegate().getSelectablePath();
			}

			@Override
			public boolean isFormula() {
				return getDelegate().isFormula();
			}

			@Override
			public boolean isNullable() {
				return getDelegate().isNullable();
			}

			@Override
			public boolean isInsertable() {
				return getDelegate().isInsertable();
			}

			@Override
			public boolean isUpdateable() {
				return getDelegate().isUpdateable();
			}

			@Override
			public boolean isPartitioned() {
				return getDelegate().isPartitioned();
			}

			@Override
			public @Nullable Long getLength() {
				return getDelegate().getLength();
			}

			@Override
			public @Nullable Integer getArrayLength() {
				return getDelegate().getArrayLength();
			}

			@Override
			public @Nullable Integer getPrecision() {
				return getDelegate().getPrecision();
			}

			@Override
			public @Nullable Integer getScale() {
				return getDelegate().getScale();
			}

			@Override
			public @Nullable Integer getTemporalPrecision() {
				return getDelegate().getTemporalPrecision();
			}

			@Override
			public boolean isLob() {
				return getDelegate().isLob();
			}

			@Nonnull
			@Override
			public JdbcMapping getJdbcMapping() {
				return getDelegate().getJdbcMapping();
			}

			@Nonnull
			@Override
			public Size toSize() {
				return getDelegate().toSize();
			}
		}

		final SelectableMappingIterator mutableSelectableMapping = new SelectableMappingIterator( tableName, base, columnNames );
		mutableSelectableMapping.forEach( this::accept );
	}


	/**
	 * Simple form allowing visitation over a number of column names within a
	 * table.
	 *
	 * Very limited functionality in terms of the visited SelectableMappings
	 * will not have any defined JdbcMapping, etc
	 */
	default void accept(@Nonnull String tableName, @Nonnull String[] columnNames, @Nonnull IntFunction<JdbcMapping> jdbcMappings) {
		class SelectableMappingIterator implements SelectableMapping {

			private int index;

			@Nonnull
			@Override
			public String getContainingTableExpression() {
				return tableName;
			}

			@Nonnull
			@Override
			public String getSelectionExpression() {
				return columnNames[index];
			}

			@Override
			public @Nullable String getCustomReadExpression() {
				return null;
			}

			@Override
			public @Nullable String getCustomWriteExpression() {
				return null;
			}

			@Override
			public @Nullable Long getLength() {
				return null;
			}

			@Override
			public @Nullable Integer getArrayLength() {
				return null;
			}

			@Override
			public @Nullable Integer getPrecision() {
				return null;
			}

			@Override
			public @Nullable Integer getScale() {
				return null;
			}

			@Override
			public @Nullable Integer getTemporalPrecision() {
				return null;
			}

			@Override
			public boolean isFormula() {
				return false;
			}

			@Override
			public boolean isNullable() {
				return true;
			}

			@Override
			public boolean isInsertable() {
				return true;
			}

			@Override
			public boolean isUpdateable() {
				return true;
			}

			@Override
			public boolean isPartitioned() {
				return false;
			}

			@Nonnull
			@Override
			public JdbcMapping getJdbcMapping() {
				return jdbcMappings.apply( index );
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
