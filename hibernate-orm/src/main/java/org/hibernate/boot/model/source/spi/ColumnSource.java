/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Set;

import org.hibernate.boot.model.TruthValue;

/**
 * Contract for source information pertaining to a physical column definition specific to a particular attribute
 * context.
 * <p/>
 * Conceptual note: this really describes a column from the perspective of its binding to an attribute, not
 * necessarily the column itself.
 *
 * @author Steve Ebersole
 */
public interface ColumnSource extends RelationalValueSource {
	/**
	 * Obtain the name of the column.
	 *
	 * @return The name of the column.  Can be {@code null}, in which case a naming strategy is applied.
	 */
	String getName();

	/**
	 * A SQL fragment to apply to the column value on read.
	 *
	 * @return The SQL read fragment
	 */
	String getReadFragment();

	/**
	 * A SQL fragment to apply to the column value on write.
	 *
	 * @return The SQL write fragment
	 */
	String getWriteFragment();

	/**
	 * Is this column nullable?
	 *
	 * @return {@code true} indicates it is nullable; {@code false} non-nullable.
	 */
	TruthValue isNullable();

	/**
	 * Obtain a specified default value for the column
	 *
	 * @return THe column default
	 */
	String getDefaultValue();

	/**
	 * Obtain the free-hand definition of the column's type.
	 *
	 * @return The free-hand column type
	 */
	String getSqlType();

	/**
	 * The deduced (and dialect convertible) type for this column
	 *
	 * @return The column's SQL data type.
	 */
	JdbcDataType getDatatype();

	/**
	 * Obtain the source for the specified column size.
	 *
	 * @return The source for the column size.
	 */
	SizeSource getSizeSource();

	/**
	 * Is this column unique?
	 *
	 * @return {@code true} indicates it is unique; {@code false} non-unique.
	 */
	boolean isUnique();

	/**
	 * Obtain the specified check constraint condition
	 *
	 * @return Check constraint condition
	 */
	String getCheckCondition();

	/**
	 * Obtain the specified SQL comment
	 *
	 * @return SQL comment
	 */
	String getComment();

	Set<String> getIndexConstraintNames();

	Set<String> getUniqueKeyConstraintNames();
}
