/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public interface ColumnTypeInformation {

	/**
	 * Is the column nullable.  The database is allowed to report unknown, hence the use of TruthValue
	 *
	 * @return nullability.
	 */
	public TruthValue getNullable();

	/**
	 * The JDBC type-code.
	 *
	 * @return JDBC type-code
	 */
	public int getTypeCode();

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
	public int getColumnSize();

	/**
	 * The precision, for numeric types
	 *
	 * @return The numeric precision
	 */
	public int getDecimalDigits();
}
