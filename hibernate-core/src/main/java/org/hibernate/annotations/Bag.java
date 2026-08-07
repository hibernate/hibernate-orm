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
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Specifies that an attribute of type [java.util.List] is semantically a
/// [bag][org.hibernate.metamodel.CollectionClassification#BAG] - that the order of
/// the list elements is not significant, and should not be persistent.
///
/// This annotation may be applied to a field or property, or to a type or
/// package descriptor. When applied at the type or package level, it acts as a
/// default for every {@code List}-typed persistent attribute with no explicit
/// list index details.
///
/// @apiNote This annotation causes an exception if the attribute is also annotated
/// [jakarta.persistence.OrderColumn] or [ListIndexBase].
///
/// @author Steve Ebersole
@Target({METHOD, FIELD, TYPE, PACKAGE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface Bag {
}
