/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import jakarta.persistence.spi.Discoverable;
import org.hibernate.usertype.UserType;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers a custom {@linkplain UserType user type} implementation
 * to be used by default for all references to a particular class of
 * {@linkplain jakarta.persistence.Basic basic type}.
 * <p>
 * May be overridden for a specific entity field or property using
 * {@link Type @Type}.
 * <p>
 * Registrations applied to a {@code package-info.java} or {@code module-info.java}
 * are processed before Hibernate begins to process any attributes, etc.
 * <p>
 * Registrations applied to a class are only applied once Hibernate begins to process
 * that class; it will also affect all future processing. However, it will not change
 * previous resolutions to use this newly registered one. Due to this nondeterminism,
 * it is recommended to only apply registrations to packages or modules, or to use a
 * {@link org.hibernate.boot.model.TypeContributor}.
 *
 * @see UserType
 * @see Type
 * @see CompositeTypeRegistration
 *
 * @author Gavin King
 *
 * @since 6.2
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE, MODULE} )
@Retention( RUNTIME )
@Repeatable( TypeRegistrations.class )
@Discoverable
public @interface TypeRegistration {
	/**
	 * The basic type described by the {@link #userType}.
	 */
	Class<?> basicClass();

	/**
	 * The {@link UserType}.
	 */
	Class<? extends UserType<?>> userType();
}
