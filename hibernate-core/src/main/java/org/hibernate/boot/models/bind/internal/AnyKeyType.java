/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/// Temporary source marker for an `@Any` discriminator key type name.
///
/// The current annotation binder still has placeholder support for `@Any` and
/// `@ManyToAny`.  This annotation carries the legacy string type-name concept
/// through the categorized model until the new binder has a proper typed source
/// descriptor for any-valued associations.
///
/// @since 9.0
/// @author Steve Ebersole
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AnyKeyType {
	String value();
}
