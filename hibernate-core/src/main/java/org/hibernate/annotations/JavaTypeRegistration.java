/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.BasicJavaType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers a {@link BasicJavaType} as the default Java type descriptor for the given
 * {@link #javaType}.
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
 * @implNote {@link BasicJavaType} registrations are maintained by the
 *           {@link org.hibernate.type.descriptor.java.spi.JavaTypeRegistry}.
 *
 * @since 6.0
 */
@Target({PACKAGE, TYPE, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
@Repeatable( JavaTypeRegistrations.class )
public @interface JavaTypeRegistration {
	Class<?> javaType();

	Class<? extends BasicJavaType<?>> descriptorClass();
}
