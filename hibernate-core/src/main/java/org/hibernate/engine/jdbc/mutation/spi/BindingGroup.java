/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * Group of all parameter {@linkplain #getBindings() bindings} for a table.
 *
 * @author Steve Ebersole
 */
public class BindingGroup {
	private final String tableName;
	private final BindingSet bindings;

	public BindingGroup(String tableName) {
		this.tableName = tableName;
		this.bindings = new BindingSet();
	}

	/**
	 * The table for which we are grouping parameter bindings
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * The parameter bindings
	 */
	public Set<Binding> getBindings() {
		return bindings;
	}

	/**
	 * Visit each parameter binding
	 */
	public void forEachBinding(Consumer<Binding> action) {
		bindings.forEach( action );
	}

	/**
	 * Create a binding
	 */
	public void bindValue(String columnName, Object value, JdbcValueDescriptor valueDescriptor) {
		assert Objects.equals( columnName, valueDescriptor.getColumnName() );
		bindings.add( new Binding( columnName, value, valueDescriptor ) );
	}

	/**
	 * Clear the {@linkplain #getBindings() bindings}
	 */
	public void clear() {
		bindings.clear();
	}

	private static class BindingSet extends AbstractSet<Binding> {
		private final List<Binding> bindings = new ArrayList<>();

		@Override
		public boolean add(Binding binding) {
			final int position = binding.getPosition();
			for ( int i = 0; i < bindings.size(); i++ ) {
				final int existingPosition = bindings.get( i ).getPosition();
				if ( position == existingPosition ) {
					return false;
				}
				else if ( position < existingPosition ) {
					bindings.add( i, binding );
					return true;
				}
			}
			return bindings.add( binding );
		}

		@Override
		public Iterator<Binding> iterator() {
			return bindings.iterator();
		}

		@Override
		public int size() {
			return bindings.size();
		}

		@Override
		public void clear() {
			bindings.clear();
		}
	}
}
