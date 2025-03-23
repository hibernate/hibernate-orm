/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;
import org.hibernate.type.descriptor.ValueBinder;

/**
 * Binding of a {@linkplain #getValue() value} for a {@link java.sql.PreparedStatement} parameter
 * by {@linkplain #getPosition() position}.
 *
 * @author Steve Ebersole
 */
public class Binding {
	private final String columnName;
	private final Object value;
	private final JdbcValueDescriptor valueDescriptor;

	public Binding(String columnName, Object value, JdbcValueDescriptor valueDescriptor) {
		this.columnName = columnName;
		this.value = value;
		this.valueDescriptor = valueDescriptor;
	}

	/**
	 * The name of the column to which this value is "bound"
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * The value to be bound to the parameter
	 */
	public Object getValue() {
		return value;
	}

	public JdbcValueDescriptor getValueDescriptor() {
		return valueDescriptor;
	}

	/**
	 * The binder to be used in binding this value
	 */
	@SuppressWarnings("unchecked")
	public <T> ValueBinder<T> getValueBinder() {
		return getValueDescriptor().getJdbcMapping().getJdbcValueBinder();
	}

	/**
	 * The JDBC parameter position
	 */
	public int getPosition() {
		return getValueDescriptor().getJdbcPosition();
	}

	@Override
	public int hashCode() {
		return getPosition();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final Binding other = (Binding) o;
		return getPosition() == other.getPosition();
	}

	@Override
	public String toString() {
		return "Binding(" + columnName + ")";
	}
}
