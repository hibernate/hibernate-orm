/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import org.hibernate.Incubating;

import java.lang.annotation.Annotation;


/**
 * A {@link UserType} which receives parameters from a custom annotation.
 * <p>
 * Implementing this interface is the same as providing a constructor with the same
 * signature as the {@link #initialize} method. But implementing this interface is
 * slightly more typesafe.
 * <p>
 * For example, implementing {@code AnnotationBasedUserType<AnnotationType>} is the
 * same as providing a constructor with this signature:
 * <pre>
 * public CustomTypeClass(AnnotationType config,
 *                        UserTypeCreationContext creationContext)
 * </pre>
 * <p>
 * where {@code CustomTypeClass} is the class that implements {@code UserType}, and
 * {@code AnnotationType} is the custom annotation type used to configure the custom
 * type. That is, it is an annotation type annotated
 * {@link org.hibernate.annotations.Type @Type}.
 * <pre>
 * &#64;Type(CustomTypeClass.class)
 * public &#64;interface AnnotationType { ... }
 * </pre>
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
