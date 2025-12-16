/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import org.hibernate.Incubating;

import java.lang.annotation.Annotation;


/**
 * A {@link UserType} which receives parameters from a custom annotation.
 *
 * @param <A> The user type annotation type supported by an implementation
 * @param <J> The java type
 *
 * @author Yanming Zhou
 *
 * @since 7.3
 */
@Incubating
public interface AnnotationBasedUserType<A extends Annotation, J> extends UserType<J> {
	/**
	 * Initializes this generation strategy for the given annotation instance.
	 *
	 * @param annotation an instance of the user type annotation type. Typically,
	 *                   implementations will retrieve the annotation's attribute
	 *                   values and store them in fields.
	 * @param context a {@link UserTypeCreationContext}.
	 * @throws org.hibernate.HibernateException in case an error occurred during initialization, e.g. if
	 *                                          an implementation can't create a value for the given property type.
	 */
	void initialize(A annotation, UserTypeCreationContext context);
}
