/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers a custom instantiator implementation to be used
 * for all references to a particular {@link jakarta.persistence.Embeddable}.
 * <p>
 * May be overridden for a specific embedded using {@link org.hibernate.annotations.EmbeddableInstantiator}
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE} )
@Retention( RUNTIME )
@Repeatable( EmbeddableInstantiatorRegistrations.class )
public @interface EmbeddableInstantiatorRegistration {
	Class<?> embeddableClass();
	Class<? extends EmbeddableInstantiator> instantiator();
}
