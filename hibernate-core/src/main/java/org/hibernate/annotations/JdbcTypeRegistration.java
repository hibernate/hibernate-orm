/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.jdbc.JdbcType;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers a {@link JdbcType} as the default JDBC type descriptor for a certain
 * {@linkplain org.hibernate.type.SqlTypes type code}. The type code is determined by:
 * <ol>
 * <li>{@link #registrationCode}, if specified, or
 * <li>{@link JdbcType#getJdbcTypeCode()}.
 * </ol>
 * <p>
 * Registrations applied to a package are processed before Hibernate begins to process
 * any attributes, etc.
 * <p>
 * Registrations applied to a class are only applied once Hibernate begins to process
 * that class; it will also affect all future processing. However, it will not change
 * previous resolutions to use this newly registered one. Due to this nondeterminism,
 * it is recommended to only apply registrations to packages or to use a
 * {@link org.hibernate.boot.model.TypeContributor}.
 *
 * @see org.hibernate.boot.model.TypeContributor
 *
 * @implNote {@link JdbcType} registrations are maintained by the
 *           {@link org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry}.
 *
 * @since 6.0
 */
@Target({PACKAGE, TYPE})
@Inherited
@Retention(RUNTIME)
@Repeatable( JdbcTypeRegistrations.class )
public @interface JdbcTypeRegistration {
	/**
	 * The descriptor to register
	 */
	Class<? extends JdbcType> value();

	/**
	 * The type-code under which to register this descriptor.  Can either add a new descriptor
	 * or override an existing one.
	 *
	 * By default we will use {@link JdbcType#getDefaultSqlTypeCode()}
	 */
	int registrationCode() default Integer.MIN_VALUE;
}
