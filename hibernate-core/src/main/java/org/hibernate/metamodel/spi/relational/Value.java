/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.dialect.Dialect;

/**
 * Models a value within a {@link ValueContainer}.  Concretely, either a {@link Column column} or a
 * {@link DerivedValue derived value}.
 *
 * @author Steve Ebersole
 */
public interface Value {

	public enum ValueType {
		COLUMN,
		DERIVED_VALUE
	}

	/**
	 * Return the value type.
	 *
	 * @return The value type
	 */
	public ValueType getValueType();

	/**
	 * Retrieve the JDBC data type of this value.
	 *
	 * @return The value's JDBC data type
	 */
	public JdbcDataType getJdbcDataType();

	/**
	 * Obtain the string representation of this value usable in log statements.
	 *
	 * @return The loggable representation
	 */
	public String toLoggableString();

	/**
	 * For any column name, generate an alias that is unique
	 * to that column name, and (optionally) unique across tables, and
	 * within alias size constraints determined by
	 * {@link org.hibernate.dialect.Dialect#getMaxAliasLength()}.
	 *
	 * todo : not sure this contract is the best place for this method
	 *
	 * @param dialect the dialect.
	 * @param tableSpecification, if non-null, the table specification to use
	 * to make the alias unambiguous across tables; if null, there is no need to need
	 * to use the table to make the alias unambiguous across tables.
	 * @return the alias.
	 */
	public String getAlias(Dialect dialect, TableSpecification tableSpecification);

	/**
	 * Validate the value against the incoming JDBC type code array, both in terms of number of types
	 * and compatibility of types.
	 *
	 * @param typeCodes The type codes.
	 *
	 * @throws org.hibernate.metamodel.ValidationException if validaton fails.
	 */
	public void validateJdbcTypes(JdbcCodes typeCodes);

	/**
	 * Used to track JDBC type usage throughout a series of potential recursive calls to component
	 * values since we do not know ahead of time which values correspond to which indexes of the
	 * jdbc type array.
	 */
	public static class JdbcCodes {
		private final int[] typeCodes;
		private int index = 0;

		public JdbcCodes(int[] typeCodes) {
			this.typeCodes = typeCodes;
		}

		public int nextJdbcCde() {
			return typeCodes[index++];
		}

		public int getIndex() {
			return index;
		}
	}

}
