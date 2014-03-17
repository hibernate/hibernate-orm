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
package org.hibernate.metamodel.source.spi;

import org.hibernate.TruthValue;
import org.hibernate.metamodel.spi.relational.JdbcDataType;

/**
 * Contract for source information pertaining to a physical column definition specific to a particular attribute
 * context.
 * <p/>
 * Conceptual note: this really describes a column from the perspective of its binding to an attribute.
 * This is especially important for {@link #isIncludedInInsert} and {@link #isIncludedInUpdate}.  There it is
 * not the column itself being described.
 *
 * @author Steve Ebersole
 */
public interface ColumnSource extends RelationalValueSource {
	/**
	 * Obtain the name of the column.
	 *
	 * @return The name of the column.  Can be {@code null}, in which case a naming strategy is applied.
	 */
	public String getName();

	/**
	 * A SQL fragment to apply to the column value on read.
	 *
	 * @return The SQL read fragment
	 */
	public String getReadFragment();

	/**
	 * A SQL fragment to apply to the column value on write.
	 *
	 * @return The SQL write fragment
	 */
	public String getWriteFragment();

	/**
	 * Is this column nullable?
	 *
	 * @return {@code true} indicates it is nullable; {@code false} non-nullable.
	 */
	public TruthValue isNullable();

	/**
	 * Obtain a specified default value for the column
	 *
	 * @return THe column default
	 */
	public String getDefaultValue();

	/**
	 * Obtain the free-hand definition of the column's type.
	 *
	 * @return The free-hand column type
	 */
	public String getSqlType();

	/**
	 * The deduced (and dialect convertible) type for this column
	 *
	 * @return The column's SQL data type.
	 */
	public JdbcDataType getDatatype();

	/**
	 * Obtain the source for the specified column size.
	 *
	 * @return The source for the column size.
	 */
	public SizeSource getSizeSource();

	/**
	 * Is this column unique?
	 *
	 * @return {@code true} indicates it is unique; {@code false} non-unique.
	 */
	public boolean isUnique();

	/**
	 * Obtain the specified check constraint condition
	 *
	 * @return Check constraint condition
	 */
	public String getCheckCondition();

	/**
	 * Obtain the specified SQL comment
	 *
	 * @return SQL comment
	 */
	public String getComment();

	/**
	 * Is the described column written to as part of inserts for the attribute being mapped.
	 *
	 * @return {@code true} indicates the column is written on insert from this attribute
	 */
	public TruthValue isIncludedInInsert();

	/**
	 * Is the described column written to as part of updates for the attribute being mapped.
	 *
	 * @return {@code true} indicates the column is written on update from this attribute
	 */
	public TruthValue isIncludedInUpdate();
}
