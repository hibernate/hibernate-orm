/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;


/**
 * A {@link Generator} based on a custom Java generator annotation type.
 * Every instance must implement either {@link InMemoryGenerator} or
 * {@link InDatabaseGenerator}. Implementing this interface is just a
 * slightly more typesafe alternative to providing a constructor with
 * the same signature as the method
 * {@link #initialize(Annotation, Member, GeneratorCreationContext)}.
 *
 * @param <A> The generator annotation type supported by an implementation
 *
 * @see org.hibernate.annotations.ValueGenerationType
 *
 * @author Gunnar Morling
 * @author Gavin King
 *
 * @since 6.2
 */
public interface AnnotationBasedGenerator<A extends Annotation> extends Generator {
	/**
	 * Initializes this generation strategy for the given annotation instance.
	 *
	 * @param annotation an instance of the strategy's annotation type. Typically, implementations will retrieve the
	 *                   annotation's attribute values and store them in fields.
	 * @param member the Java member annotated with the generator annotation.
	 * @param context a {@link GeneratorCreationContext}
	 * @throws org.hibernate.HibernateException in case an error occurred during initialization, e.g. if
	 *                                          an implementation can't create a value for the given property type.
	 */
	void initialize(A annotation, Member member, GeneratorCreationContext context);
}
