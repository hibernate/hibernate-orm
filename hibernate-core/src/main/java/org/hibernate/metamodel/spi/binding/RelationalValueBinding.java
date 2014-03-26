/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.binding;

import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * Represents the binding information of a column/formula.
 *
 * Different from a {@link Value} because, while the {@link Value} exists only once in the relational model,
 * that {@link Value} may be bound to multiple attributes.  A {@link RelationalValueBinding} then tracks the
 * information that is specific to each attribute's binding to that {@link Value}.
 *
 * @author Steve Ebersole
 */
public class RelationalValueBinding {
	private final TableSpecification table;
	private final Value value;
	private final boolean includeInInsert;
	private final boolean includeInUpdate;
	private final boolean isDerived;

	public RelationalValueBinding(final TableSpecification table, final DerivedValue value) {
		this.table = table;
		this.value = value;
		this.includeInInsert = false;
		this.includeInUpdate = false;
		this.isDerived = true;
	}

	public RelationalValueBinding(final TableSpecification table, final Column value, final boolean includeInInsert, final boolean includeInUpdate) {
		this.table = table;
		this.value = value;
		this.includeInInsert = includeInInsert;
		this.includeInUpdate = includeInUpdate;
		this.isDerived = false;
	}

	public TableSpecification getTable() {
		return table;
	}

	/**
	 * Retrieve the relational value bound here.
	 *
	 * @return The relational value.
	 */
	public Value getValue() {
		return value;
	}

	/**
	 * Is the value bound here derived?  Same as checking {@link #getValue()} as a {@link DerivedValue}
	 *
	 * @return {@code true} indicates the bound value is derived.
	 */
	public boolean isDerived() {
		return isDerived;
	}

	/**
	 * Is the value bound here nullable?
	 *
	 * @return {@code true} indicates the bound value is derived or a column not marked as non-null.
	 */
	public boolean isNullable() {
		return isDerived() || ( (Column) value ).isNullable();
	}

	/**
	 * Is the value to be inserted as part of its binding here?
	 *
	 * @return {@code true} indicates the value should be included; {@code false} indicates it should not
	 */
	public boolean isIncludeInInsert() {
		return includeInInsert;
	}

	/**
	 * Is the value to be updated as part of its binding here?
	 *
	 * @return {@code true} indicates the value should be included; {@code false} indicates it should not
	 */
	public boolean isIncludeInUpdate() {
		return includeInUpdate;
	}

	@Override
	public String toString() {
		return "RelationalValueBinding{table=" + table.toLoggableString() +
				", value=" + value.toLoggableString() +
				'}';
	}
}
