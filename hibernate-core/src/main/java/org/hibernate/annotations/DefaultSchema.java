/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Specifies the default schema and/or catalog for entities in the annotated
/// scope (class, package, or module).
///
/// When placed on a class, `package-info.java`, or `module-info.java`,
/// the specified [#schema()] and [#catalog()] are applied to all entities
/// within that scope that do not explicitly set them via
/// [jakarta.persistence.Table#schema()] or [jakarta.persistence.Table#catalog()].
///
/// @since 7.0
@Target({TYPE, PACKAGE, MODULE})
@Retention(RUNTIME)
public @interface DefaultSchema {
	/// The default schema name.
	String schema() default "";

	/// The default catalog name.
	String catalog() default "";
}
