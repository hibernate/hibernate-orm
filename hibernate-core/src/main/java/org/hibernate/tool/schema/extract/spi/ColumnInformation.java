/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.TruthValue;
import org.hibernate.metamodel.spi.relational.Identifier;

/**
 * Provides access to information about existing table columns
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public interface ColumnInformation {
	/**
	 * Access to the containing table.
	 *
	 * @return The containing table information
	 */
	public TableInformation getContainingTableInformation();

	/**
	 * The simple (not qualified) column name.
	 *
	 * @return The column simple identifier.
	 */
	public Identifier getColumnIdentifier();

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






