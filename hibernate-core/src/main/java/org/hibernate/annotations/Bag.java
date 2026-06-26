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
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Specifies that an attribute of type [java.util.List] is semantically a
/// [bag][org.hibernate.metamodel.CollectionClassification#BAG] - that the order of
/// the list elements is not significant, and should not be persistent.
///
/// Use this annotation when a Java [java.util.List] should be mapped as a bag
/// instead of the default list semantics.
///
/// @apiNote This annotation causes an exception if the attribute is also annotated
/// [jakarta.persistence.OrderColumn] or [ListIndexBase].
///
/// @author Steve Ebersole
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface Bag {
}
