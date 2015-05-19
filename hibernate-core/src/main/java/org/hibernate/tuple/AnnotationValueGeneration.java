/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.lang.annotation.Annotation;


/**
 * A {@link ValueGeneration} based on a custom Java generator annotation type.
 *
 * @param <A> The generator annotation type supported by an implementation
 *
 * @author Gunnar Morling
 */
public interface AnnotationValueGeneration<A extends Annotation> extends ValueGeneration {

	/**
	 * Initializes this generation strategy for the given annotation instance.
	 *
	 * @param annotation an instance of the strategy's annotation type. Typically implementations will retrieve the
	 * annotation's attribute values and store them in fields.
	 * @param propertyType the type of the property annotated with the generator annotation. Implementations may use
	 * the type to determine the right {@link ValueGenerator} to be applied.
	 *
	 * @throws org.hibernate.HibernateException in case an error occurred during initialization, e.g. if
	 * an implementation can't create a value for the given property type.
	 */
	void initialize(A annotation, Class<?> propertyType);
}
