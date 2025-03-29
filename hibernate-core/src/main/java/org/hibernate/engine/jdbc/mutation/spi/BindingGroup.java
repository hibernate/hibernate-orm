/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * Group of all parameter {@linkplain #getBindings() bindings} for a table.
 *
 * @author Steve Ebersole
 */
public class BindingGroup {
	private final String tableName;
	private final Set<Binding> bindings;

	public BindingGroup(String tableName) {
		this.tableName = tableName;
		// todo (6.2) : TreeSet to log the parameter binding sequentially
		//		- if we don't care, this can be another type of Set for perf
		this.bindings = new TreeSet<>( Comparator.comparing( Binding::getPosition ) );
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
}
