/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.spi.Discoverable;
import org.hibernate.usertype.CompositeUserType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers a custom {@linkplain CompositeUserType composite user type}
 * implementation to be used by default for all references to a particular
 * {@linkplain jakarta.persistence.Embeddable embeddable} class.
 * <p>
 * May be overridden for a specific entity field or property using
 * {@link CompositeType @CompositeType}.
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
 * @see CompositeUserType
 * @see CompositeType
 * @see TypeRegistration
 * @see org.hibernate.boot.model.TypeContributor
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE, MODULE} )
@Retention( RUNTIME )
@Repeatable( CompositeTypeRegistrations.class )
@Discoverable
public @interface CompositeTypeRegistration {
	/**
	 * The embeddable type described by the {@link #userType}.
	 */
	Class<?> embeddableClass();

	/**
	 * The {@link CompositeUserType}.
	 */
	Class<? extends CompositeUserType<?>> userType();
}
