/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Container for multiple {@link CollectionTableOverride} annotations.
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface CollectionTableOverrides {
	CollectionTableOverride[] value();
}
