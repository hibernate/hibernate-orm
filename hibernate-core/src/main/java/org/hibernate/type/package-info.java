/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * A Hibernate {@link org.hibernate.type.Type} is a strategy for mapping a Java
 * property type to a JDBC type or types. In modern Hibernate, {@code Type} itself
 * is now of receding importance, and we prefer to work directly with the combination
 * of:
 * <ul>
 * <li>a {@link org.hibernate.type.descriptor.java.JavaType}, with
 * <li>a {@link org.hibernate.type.descriptor.jdbc.JdbcType}.
 * </ul>
 * <p>
 * A {@code JdbcType} is able to read and write a single Java type to one, or
 * sometimes several, {@linkplain org.hibernate.type.SqlTypes JDBC types}.
 * <p>
 * A {@code JavaType} is able to determine if an attribute of a given Java type is
 * dirty, and then convert it to one of several other equivalent Java representations
 * at the request of its partner {@code JdbcType}.
 * <p>
 * For example, if an entity attribute of Java type {@code BigInteger} is mapped to
 * the JDBC type {@link java.sql.Types#VARCHAR}, the
 * {@link org.hibernate.type.descriptor.jdbc.VarcharJdbcType} will ask its
 * {@link org.hibernate.type.descriptor.java.BigIntegerJavaType} to convert instances
 * of {@code BigInteger} to and from {@code String} when writing to and reading from
 * JDBC statements and result sets.
 * <p>
 * This approach provides quite some flexibility in allowing a given Java type to
 * map to a range of JDBC types. However, when the built-in conversions don't handle
 * a particular mapping, a
 * {@link org.hibernate.metamodel.model.convert.spi.BasicValueConverter} may assist
 * in the conversion process. For example, a JPA
 * {@link jakarta.persistence.AttributeConverter} might be provided.
 *
 * @see org.hibernate.type.Type
 * @see org.hibernate.type.SqlTypes
 * @see org.hibernate.type.FormatMapper
 */
package org.hibernate.type;
