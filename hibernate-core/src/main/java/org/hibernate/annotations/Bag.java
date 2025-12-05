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
/// This annotation is not necessary, and has no effect, unless the configuration
/// property {@value org.hibernate.cfg.AvailableSettings#DEFAULT_LIST_SEMANTICS}
/// is set to {@link org.hibernate.metamodel.CollectionClassification#LIST}.
/// However, its use is still encouraged, since the explicit annotation serves
/// as useful documentation.
///
/// @apiNote This annotation causes an exception if the attribute is also annotated
/// [jakarta.persistence.OrderColumn] or [ListIndexBase].
///
/// @author Steve Ebersole
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface Bag {
}
