/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.usertype.CompositeUserType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
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
 *
 * @see CompositeUserType
 * @see CompositeType
 * @see TypeRegistration
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE} )
@Retention( RUNTIME )
@Repeatable( CompositeTypeRegistrations.class )
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
