/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.AssertionFailure;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;


/**
 * An implementation of {@link ValueGeneration} which receives parameters from a custom
 * {@linkplain org.hibernate.annotations.ValueGenerationType generator annotation}.
 * <p>
 * This is an older API that predates {@link Generator} and {@link AnnotationBasedGenerator}.
 * It's often cleaner to implement {@code AnnotationBasedGenerator} directly.
 *
 * @param <A> The generator annotation type supported by an implementation
 *
 * @see org.hibernate.annotations.ValueGenerationType
 *
 * @author Gunnar Morling
 *
 * @see ValueGeneration
 *
 * @deprecated Replaced by {@link AnnotationBasedGenerator}
 */
@Deprecated(since = "6.2", forRemoval = true)
public interface AnnotationValueGeneration<A extends Annotation>
		extends ValueGeneration, AnnotationBasedGenerator<A> {
	/**
	 * Initializes this generation strategy for the given annotation instance.
	 *
	 * @param annotation   an instance of the strategy's annotation type. Typically,
	 *                     implementations will retrieve the annotation's attribute
	 *                     values and store them in fields.
	 * @param propertyType the type of the property annotated with the generator annotation.
	 * @throws org.hibernate.HibernateException in case an error occurred during initialization, e.g. if
	 *                                          an implementation can't create a value for the given property type.
	 */
	void initialize(A annotation, Class<?> propertyType);

	default void initialize(A annotation, Member member, GeneratorCreationContext context) {
		initialize( annotation, getPropertyType( member ) );
	}

	private static Class<?> getPropertyType(Member member) {
		if (member instanceof Field) {
			return ((Field) member).getType();
		}
		else if (member instanceof Method) {
			return ((Method) member).getReturnType();
		}
		else {
			throw new AssertionFailure("member should have been a method or field");
		}
	}
}
