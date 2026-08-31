/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.spi.Discoverable;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers a custom instantiator implementation to be used
 * for all references to a particular {@link jakarta.persistence.Embeddable}.
 * <p>
 * May be overridden for a specific embedded using {@link org.hibernate.annotations.EmbeddableInstantiator}.
 * <p>
 * Registrations applied to a {@code package-info.java} or {@code module-info.java}
 * are processed before Hibernate begins to process any attributes, etc.
 * <p>
 * Registrations applied to a class are only applied once Hibernate begins to process
 * that class; it will also affect all future processing. However, it will not change
 * previous resolutions to use this newly registered one. Due to this nondeterminism,
 * it is recommended to only apply registrations to packages or modules,
 * or directly to the relevant embeddable class.
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE, MODULE} )
@Retention( RUNTIME )
@Repeatable( EmbeddableInstantiatorRegistrations.class )
@Discoverable
public @interface EmbeddableInstantiatorRegistration {
	Class<?> embeddableClass();
	Class<? extends EmbeddableInstantiator> instantiator();
}
