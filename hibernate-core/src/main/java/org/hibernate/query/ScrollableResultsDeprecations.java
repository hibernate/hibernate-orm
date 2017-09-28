/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Isolate deprecations for the {@link org.hibernate.ScrollableResults} contract
 *
 * @apiNote Extended by {@link org.hibernate.ScrollableResults} contract to
 * temporarily continue to expose these deprecated methods.
 *
 * @author Steve Ebersole
 */
public interface ScrollableResultsDeprecations {
	/**
	 * Get the <tt>i</tt>th object in the current row of results, without
	 * initializing any other results in the row. This method may be used
	 * safely, regardless of the type of the column (ie. even for scalar
	 * results).
	 *
	 * @param i the column, numbered from zero
	 *
	 * @return The requested result object; may return {@code null}
	 *
	 * @throws IndexOutOfBoundsException If i is an invalid index.
	 *
	 * @deprecated if access is desired to individual elements in a multi-result result,
	 * use {@link javax.persistence.Tuple} as the return type (`<R>`)
	 */
	@Deprecated
	Object get(int i);

	/**
	 * Get the type of the <tt>i</tt>th column of results.
	 *
	 * @param i the column, numbered from zero
	 *
	 * @return the Hibernate type
	 *
	 * @throws IndexOutOfBoundsException If i is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	JavaTypeDescriptor getType(int i);

	/**
	 * Convenience method to read an integer.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as an integer
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Integer getInteger(int col);

	/**
	 * Convenience method to read a long.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a long
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Long getLong(int col);

	/**
	 * Convenience method to read a float.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a float
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Float getFloat(int col);

	/**
	 * Convenience method to read a boolean.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a boolean
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Boolean getBoolean(int col);

	/**
	 * Convenience method to read a double.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a double
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Double getDouble(int col);

	/**
	 * Convenience method to read a short.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a short
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Short getShort(int col);

	/**
	 * Convenience method to read a byte.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a byte
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Byte getByte(int col);

	/**
	 * Convenience method to read a char.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a char
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Character getCharacter(int col);

	/**
	 * Convenience method to read a binary (byte[]).
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a binary (byte[])
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	byte[] getBinary(int col);

	/**
	 * Convenience method to read a String using streaming.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a String
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	String getText(int col);

	/**
	 * Convenience method to read a blob.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Blob
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Blob getBlob(int col);

	/**
	 * Convenience method to read a clob.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Clob
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Clob getClob(int col);

	/**
	 * Convenience method to read a string.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a String
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	String getString(int col);

	/**
	 * Convenience method to read a BigDecimal.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a BigDecimal
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	BigDecimal getBigDecimal(int col);

	/**
	 * Convenience method to read a BigInteger.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a BigInteger
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	BigInteger getBigInteger(int col);

	/**
	 * Convenience method to read a Date.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Date
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Date getDate(int col);

	/**
	 * Convenience method to read a Locale.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Locale
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Locale getLocale(int col);

	/**
	 * Convenience method to read a Calendar.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Calendar
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	Calendar getCalendar(int col);

	/**
	 * Convenience method to read a TimeZone.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a TimeZone
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	TimeZone getTimeZone(int col);
}
