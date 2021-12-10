/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Defines a list of useful constant values that may be used
 * to specify long column lengths in the JPA
 * {@link jakarta.persistence.Column} annotation.
 * <p>
 * For example, {@code @Column(length=LONG16)} would specify
 * that Hibernate should generate DDL with a column type
 * capable of holding strings with 16-bit lengths.
 *
 * @see jakarta.persistence.Column#length()
 *
 * @author Gavin King
 */
public final class Length {
	/**
	 * The default length for a column in JPA.
	 *
	 * @see jakarta.persistence.Column#length()
	 * @see org.hibernate.type.descriptor.java.JavaType#getDefaultSqlLength
	 */
	public static final int DEFAULT = 255;
	/**
	 * Used to select a variable-length SQL type large
	 * enough to contain values of maximum length 32600.
	 * This arbitrary-looking number was chosen because
	 * some databases support variable-length types
	 * right up to a limit that is just slightly below
	 * 32767. (For some, the limit is 32672 characters.)
	 * <p>
	 * This is also the default length for a column
	 * declared using
	 * {@code @JdbcTypeCode(Types.LONGVARCHAR)} or
	 * {@code @JdbcTypeCode(Types.LONGVARBINARY)}.
	 *
	 * @see org.hibernate.type.descriptor.java.JavaType#getLongSqlLength
	 */
	public static final int LONG = 32_600;
	/**
	 * The maximum length that fits in 16 bits.
	 * Used to select a variable-length SQL type large
	 * enough to contain values of maximum length 32767.
	 */
	public static final int LONG16 = Short.MAX_VALUE;
	/**
	 * The maximum length of a Java string, that is,
	 * the maximum length that fits in 32 bits.
	 * Used to select a variable-length SQL type large
	 * enough to contain any Java string.
	 */
	public static final int LONG32 = Integer.MAX_VALUE;
	/**
	 * The default length for a LOB column.
	 *
	 * @see org.hibernate.dialect.Dialect#getDefaultLobLength
	 */
	public static final int LOB_DEFAULT = 1_048_576;

	private Length() {}
}
