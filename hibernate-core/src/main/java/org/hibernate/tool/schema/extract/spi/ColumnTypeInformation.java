/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import java.sql.Types;

import org.hibernate.Incubating;
import org.hibernate.boot.model.TruthValue;

/**
 * Provides access to information about existing table columns
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
@Incubating
public interface ColumnTypeInformation {

	ColumnTypeInformation EMPTY = new ColumnTypeInformation() {
		@Override
		public TruthValue getNullable() {
			return TruthValue.UNKNOWN;
		}

		@Override
		public int getTypeCode() {
			return Types.OTHER;
		}

		@Override
		public String getTypeName() {
			return null;
		}

		@Override
		public int getColumnSize() {
			return 0;
		}

		@Override
		public int getDecimalDigits() {
			return 0;
		}
	};

	/**
	 * Is the column nullable.  The database is allowed to report unknown, hence the use of TruthValue
	 *
	 * @return nullability.
	 */
	TruthValue getNullable();

	/**
	 * The JDBC type-code.
	 *
	 * @return JDBC type-code
	 */
	int getTypeCode();

	/**
	 * The database specific type name.
	 *
	 * @return Type name
	 */
	public String getTypeName();

	// todo : wrap these in org.hibernate.metamodel.spi.relational.Size ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The column size (length).
	 *
	 * @return The column length
	 */
	int getColumnSize();

	/**
	 * The precision, for numeric types
	 *
	 * @return The numeric precision
	 */
	int getDecimalDigits();
}
