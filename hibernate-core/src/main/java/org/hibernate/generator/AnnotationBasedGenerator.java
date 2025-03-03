/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;


/**
 * A {@link Generator} which receives parameters from a custom
 * {@linkplain org.hibernate.annotations.ValueGenerationType generator annotation} or
 * {@linkplain org.hibernate.annotations.IdGeneratorType id generator annotation}.
 * <p>
 * Implementing this interface is the same as providing a constructor with the same
 * signature as the {@link #initialize} method. But implementing this interface is
 * slightly more typesafe.
 * <p>
 * For example, implementing {@code AnnotationBasedGenerator<AnnotationType>} is the
 * same as providing a constructor with this signature:
 * <pre>
 * public GeneratorClass(AnnotationType config, Member idMember,
 *                      GeneratorCreationContext creationContext)
 * </pre>
 * <p>
 * where {@code GeneratorClass} is the class that implements {@code Generator}, and
 * {@code AnnotationType} is the generator annotation type used to configure the
 * generator.
 * <p>
 * Every instance of this class must implement either {@link BeforeExecutionGenerator} or
 * {@link OnExecutionGenerator}.
 *
 * @param <A> The generator annotation type supported by an implementation
 *
 * @see org.hibernate.annotations.ValueGenerationType
 * @see org.hibernate.annotations.IdGeneratorType
 *
 * @author Gavin King
 *
 * @since 6.2
 */
public interface AnnotationBasedGenerator<A extends Annotation> extends Generator {
	/**
	 * Initializes this generation strategy for the given annotation instance.
	 *
	 * @param annotation an instance of the strategy's annotation type. Typically,
	 *                   implementations will retrieve the annotation's attribute
	 *                   values and store them in fields.
	 * @param member the Java member annotated with the generator annotation.
	 * @param context a {@link GeneratorCreationContext}
	 * @throws org.hibernate.HibernateException in case an error occurred during initialization, e.g. if
	 *                                          an implementation can't create a value for the given property type.
	 */
	void initialize(A annotation, Member member, GeneratorCreationContext context);
}
