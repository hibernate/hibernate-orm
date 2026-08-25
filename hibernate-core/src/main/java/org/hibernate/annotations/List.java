/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Specifies that an attribute of type [java.util.List] is semantically a
/// [list][org.hibernate.metamodel.CollectionClassification#LIST], that is, that
/// the order of the list elements is significant and should be persistent.
///
/// This annotation is not necessary, and has no effect, unless the configuration
/// property {@value org.hibernate.cfg.AvailableSettings#DEFAULT_LIST_SEMANTICS}
/// is set to {@link org.hibernate.metamodel.CollectionClassification#BAG}, which
/// is the default.
/// However, its use is still encouraged, since the explicit annotation serves
/// as useful documentation.
///
/// When placed on a type, {@code package-info.java}, or {@code module-info.java},
/// applies to all [java.util.List] attributes of entities in that scope.
///
/// @see Bag
///
/// @since 8.0
@Incubating
@Target({METHOD, FIELD, TYPE, PACKAGE, MODULE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface List {
}
