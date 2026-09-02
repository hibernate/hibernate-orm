/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SelectableMapping;

/// Mutable adapter used while visiting a sequence of selectable names.
///
/// @author Steve Ebersole
@Internal
public final class MutableSelectableMapping implements SelectableMapping {
	private final String tableName;
	private final JdbcMappingContainer base;
	private final String[] columnNames;

	private int index;

	public MutableSelectableMapping(String tableName, JdbcMappingContainer base, String[] columnNames) {
		this.tableName = tableName;
		this.base = base;
		this.columnNames = columnNames;

		assert base.getJdbcTypeCount() == columnNames.length;
	}

	public void forEach(BiConsumer<Integer, SelectableMapping> consumer) {
		for ( index = 0; index < columnNames.length; index++ ) {
			consumer.accept( index, this );
		}
	}

	@Override
	public String getContainingTableExpression() {
		return tableName;
	}

	@Override
	public String getSelectionExpression() {
		return columnNames[index];
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return base.getJdbcMapping( index );
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isInsertable() {
		return true;
	}

	@Override
	public boolean isUpdateable() {
		return false;
	}

	@Override
	public boolean isPartitioned() {
		return false;
	}

	@Override
	public @Nullable Long getLength() {
		throw new UnsupportedOperationException();
	}

	@Override
	public @Nullable Integer getArrayLength() {
		throw new UnsupportedOperationException();
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
	public @Nullable String getCustomReadExpression() {
		return null;
	}

	@Override
	public @Nullable String getCustomWriteExpression() {
		return null;
	}
}
