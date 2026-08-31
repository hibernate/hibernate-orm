/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.usertype.UserCollectionType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows to register a {@link org.hibernate.usertype.UserCollectionType}
 * to use as the default for the specified classification of collection.
 * <p>
 * Registrations applied to a {@code package-info.java} or {@code module-info.java}
 * are processed before Hibernate begins to process any attributes, etc.
 * <p>
 * Registrations applied to a class are only applied once Hibernate begins to process
 * that class; it will also affect all future processing. However, it will not change
 * previous resolutions to use this newly registered one. Due to this nondeterminism,
 * it is recommended to only apply registrations to packages or modules.
 *
 * @see CollectionType
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Target({TYPE, PACKAGE, MODULE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Repeatable( CollectionTypeRegistrations.class )
public @interface CollectionTypeRegistration {
	/**
	 * The collection classification for which the supplied type applies
	 */
	CollectionClassification classification();

	/**
	 * Specifies the UserCollectionType to use when we encounter
	 * a plural attribute classified as {@link #classification()}
	 */
	Class<? extends UserCollectionType> type();

	/**
	 * Specifies configuration information for the type.  Note that if the named type is a
	 * {@link org.hibernate.usertype.UserCollectionType}, it must also implement
	 * {@link org.hibernate.usertype.ParameterizedType} in order to receive these values.
	 */
	Parameter[] parameters() default {};
}
