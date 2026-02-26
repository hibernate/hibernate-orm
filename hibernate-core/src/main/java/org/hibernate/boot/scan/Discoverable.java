/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/// @deprecated Here just as a placeholder for `@Discoverable` being added to JPA 4.
/// See https://github.com/jakartaee/persistence/pull/940.
///
/// @author Steve Ebersole
@Deprecated
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Discoverable {
}
