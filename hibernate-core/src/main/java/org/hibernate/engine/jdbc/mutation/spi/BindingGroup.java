/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * Group of all parameter {@linkplain #getBindings() bindings} for a table.
 *
 * @author Steve Ebersole
 */
public class BindingGroup {
	private final String tableName;
	private final List<Binding> bindings;
	private final Set<Binding> bindingsView;

	public BindingGroup(String tableName) {
		this.tableName = tableName;
		this.bindings = new ArrayList<>();
		this.bindingsView = new AbstractSet<>() {
			@Override
			public Iterator<Binding> iterator() {
				return bindings.iterator();
			}

			@Override
			public int size() {
				return bindings.size();
			}
		};
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
		return bindingsView;
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
		final int position = valueDescriptor.getJdbcPosition();
		for ( int i = 0; i < bindings.size(); i++ ) {
			final int existingPosition = bindings.get( i ).getPosition();
			if ( position == existingPosition ) {
				return;
			}
			if ( position < existingPosition ) {
				bindings.add( i, new Binding( columnName, value, valueDescriptor ) );
				return;
			}
		}
		bindings.add( new Binding( columnName, value, valueDescriptor ) );
	}

	/**
	 * Clear the {@linkplain #getBindings() bindings}
	 */
	public void clear() {
		bindings.clear();
	}

	@Nullable public Binding findBinding(String columnName, ParameterUsage usage) {
		for ( Binding binding : bindings ) {
			if ( binding.getValueDescriptor().getUsage() == usage
				&& binding.getColumnName().equals( columnName ) ) {
				return binding;
			}
		}
		return null;
	}

	public Binding getBinding(String columnName, ParameterUsage usage) {
		final Binding binding = findBinding( columnName, usage );
		if ( binding != null ) {
			return binding;
		}
		throw new IllegalArgumentException( String.format( Locale.ROOT,
				"Could not locate binding [%s : %s]",
				usage.toString(),
				columnName
		) );
	}
}
