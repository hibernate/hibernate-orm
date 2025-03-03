/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
 * @since 6.0
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
	 * <p>
	 * For example, {@code @Column(length=LONG)} results
	 * in the column type:
	 * <table>
	 * <tr><td>{@code varchar(32600)}</td><td>on h2, Db2, and PostgreSQL</td>
	 * <tr><td>{@code text}</td><td>on MySQL</td>
	 * <tr><td>{@code clob}</td><td>on Oracle</td>
	 * <tr><td>{@code varchar(max)}</td><td>on SQL Server</td></tr>
	 * </table>
	 *
	 * @see org.hibernate.type.descriptor.java.JavaType#getLongSqlLength
	 *
	 * @see org.hibernate.type.SqlTypes#LONGVARCHAR
	 * @see org.hibernate.type.SqlTypes#LONGVARBINARY
	 */
	public static final int LONG = 32_600;
	/**
	 * The maximum length that fits in 16 bits.
	 * Used to select a variable-length SQL type large
	 * enough to accommodate values of maximum length
	 * {@value Short#MAX_VALUE}.
	 * <p>
	 * For example, {@code @Column(length=LONG16)} results
	 * in the column type:
	 * <table>
	 * <tr><td>{@code varchar(32767)}</td><td>on h2 and PostgreSQL</td>
	 * <tr><td>{@code text}</td><td>on MySQL</td>
	 * <tr><td>{@code clob}</td><td>on Oracle and Db2</td>
	 * <tr><td>{@code varchar(max)}</td><td>on SQL Server</td></tr>
	 * </table>
	 */
	public static final int LONG16 = Short.MAX_VALUE;
	/**
	 * The maximum length of a Java string or array,
	 * that is, the maximum length that fits in 32 bits.
	 * Used to select a variable-length SQL type large
	 * enough to accommodate any Java string up to the
	 * maximum possible length {@value Integer#MAX_VALUE}.
	 * <p>
	 * This is also the default length for a column
	 * declared using
	 * {@code @JdbcTypeCode(SqlTypes.LONG32VARCHAR)} or
	 * {@code @JdbcTypeCode(SqlTypes.LONG32VARBINARY)}.
	 * <p>
	 * For example, {@code @Column(length=LONG32)} results
	 * in the column type:
	 * <table>
	 * <tr><td>{@code text}</td><td>on PostgreSQL</td>
	 * <tr><td>{@code longtext}</td><td>on MySQL</td>
	 * <tr><td>{@code clob}</td><td>on h2, Oracle, and Db2</td>
	 * <tr><td>{@code varchar(max)}</td><td>on SQL Server</td></tr>
	 * </table>
	 *
	 * @see org.hibernate.type.SqlTypes#LONG32VARCHAR
	 * @see org.hibernate.type.SqlTypes#LONG32VARBINARY
	 */
	public static final int LONG32 = Integer.MAX_VALUE;
	/**
	 * The default length for a LOB column, on databases
	 * where LOB columns have a length.
	 *
	 * @see org.hibernate.dialect.Dialect#getDefaultLobLength
	 *
	 * @see org.hibernate.type.SqlTypes#CLOB
	 * @see org.hibernate.type.SqlTypes#BLOB
	 */
	public static final int LOB_DEFAULT = 1_048_576;

	private Length() {}
}
