/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.naming.Identifier;

/**
 * Provides access to information about existing table columns
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public interface ColumnInformation extends ColumnTypeInformation {
	/**
	 * Access to the containing table.
	 *
	 * @return The containing table information
	 */
	TableInformation getContainingTableInformation();

	/**
	 * The simple (not qualified) column name.
	 *
	 * @return The column simple identifier.
	 */
	Identifier getColumnIdentifier();

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
	String getTypeName();

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
