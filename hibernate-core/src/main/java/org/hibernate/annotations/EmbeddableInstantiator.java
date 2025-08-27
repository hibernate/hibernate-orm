/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a custom instantiator for a specific embedded
 */
@Target( {TYPE, FIELD, METHOD, ANNOTATION_TYPE} )
@Retention( RUNTIME )
public @interface EmbeddableInstantiator {
	Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> value();
}
