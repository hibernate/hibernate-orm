/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.spi;

import org.hibernate.Incubating;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Support for {@linkplain org.hibernate.type basic-typed} value conversions.
 * <p>
 * Conversions might be determined by:
 * <ul>
 * <li>a custom JPA {@link jakarta.persistence.AttributeConverter}, or
 * <li>implicitly, based on the Java type, for example, for Java {@code enum}s.
 * </ul>
 * @param <D> The Java type we use to represent the domain (object) type
 * @param <R> The Java type we use to represent the relational type
 *
 * @author Steve Ebersole
 */
@Incubating
public interface BasicValueConverter<D,R> {
	/**
	 * Convert the relational form just retrieved from JDBC ResultSet into
	 * the domain form.
	 */
	@Nullable D toDomainValue(@Nullable R relationalForm);

	/**
	 * Convert the domain form into the relational form in preparation for
	 * storage into JDBC
	 */
	@Nullable R toRelationalValue(@Nullable D domainForm);

	/**
	 * Descriptor for the Java type for the domain portion of this converter
	 */
	JavaType<D> getDomainJavaType();

	/**
	 * Descriptor for the Java type for the relational portion of this converter
	 */
	JavaType<R> getRelationalJavaType();
}
