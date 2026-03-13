/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.binder.internal.DerivedColumnsBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Container for repeated {@link DerivedColumn} annotation.
 *
 * @since 7.4
 *
 * @author Gavin King
 */
@Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
@TypeBinderType(binder = DerivedColumnsBinder.class)
@AttributeBinderType(binder = DerivedColumnsBinder.class)
@Incubating
public @interface DerivedColumns {
	DerivedColumn[] value();
}
