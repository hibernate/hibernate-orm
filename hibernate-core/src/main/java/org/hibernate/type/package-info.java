/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * A Hibernate {@link org.hibernate.type.Type} is a strategy for mapping a Java
 * property type to a JDBC type or types. Every persistent attribute of an entity
 * or embeddable object has a {@code Type}, even attributes which represent
 * associations or hold references to embedded objects.
 * <p>
 * On the other hand, in modern Hibernate, {@code Type} itself is of receding
 * importance to application developers, though it remains a very important
 * internal abstraction.
 *
 * <h3 id="basic">Basic types</h3>
 *
 * For {@linkplain jakarta.persistence.Basic basic} types, we prefer to model the
 * type mapping in terms the combination of:
 * <ul>
 * <li>a {@link org.hibernate.type.descriptor.java.JavaType}, with
 * <li>a {@link org.hibernate.type.descriptor.jdbc.JdbcType}, and,
 * <li>possibly, a {@linkplain org.hibernate.type.descriptor.converter.spi.BasicValueConverter
 *     converter}, though this is not usually needed.
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
 * An important point is that the set of available {@code JavaType}s and of available
 * {@code JdbcType}s is not fixed&mdash;a {@link org.hibernate.type.spi.TypeConfiguration}
 * is {@linkplain org.hibernate.boot.model.TypeContributions customizable during the
 * bootstrap process}.
 * <p>
 * This approach provides quite some flexibility in allowing a given Java type to
 * map to a range of JDBC types. However, when the built-in conversions don't handle
 * a particular mapping, a
 * {@linkplain org.hibernate.type.descriptor.converter.spi.BasicValueConverter converter}
 * may assist in the conversion process. For example, a JPA
 * {@link jakarta.persistence.AttributeConverter} might be provided.
 * <p>
 * A {@code JavaType} comes with a built-in
 * {@link org.hibernate.type.descriptor.java.MutabilityPlan}, but this may be
 * overridden when types are composed.
 * <p>
 * See {@link org.hibernate.annotations} for information on how to influence basic
 * type mappings using annotations.
 *
 * <h3 id="custom">Custom types</h3>
 *
 * The package {@link org.hibernate.usertype} provides a way for application developers
 * to define new types without being exposed to the full complexity of the {@code Type}
 * framework defined in this package.
 * <ul>
 * <li>A {@link org.hibernate.usertype.UserType} may be used to define single-column
 *     type mappings, and thus competes with the "compositional" approach to basic type
 *     mappings described above.
 * <li>On the other hand, a {@link org.hibernate.usertype.CompositeUserType} defines a
 *     way to handle multi-column type mappings, and is a much more flexible form of
 *     {@link jakarta.persistence.Embeddable} object mapping.
 * </ul>
 *
 * <h3>Built-in converters for boolean mappings</h3>
 *
 * In older versions of Hibernate there were dedicated {@code Type}s mapping Java
 * {@code boolean} to {@code char(1)} or {@code integer} database columns. These
 * have now been replaced by the converters:
 * <ul>
 * <li>{@link org.hibernate.type.TrueFalseConverter}, which encodes a boolean value
 *     as {@code 'T'} or {@code 'F'},
 * <li>{@link org.hibernate.type.YesNoConverter}, which encodes a boolean value
 *     as {@code 'Y'} or {@code 'N'}, and
 * <li>{@link org.hibernate.type.NumericBooleanConverter}, which encodes a boolean
 *     value as {@code 1} or {@code 0}.
 * </ul>
 * <p>
 * These converters may be applied, as usual, using the JPA-defined
 * {@link jakarta.persistence.Converter} annotation.
 *
 * @see org.hibernate.type.Type
 * @see org.hibernate.type.SqlTypes
 */
package org.hibernate.type;
