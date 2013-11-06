/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tuple;

import java.lang.annotation.Annotation;

import org.hibernate.HibernateException;

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
	 * @throws HibernateException in case an error occurred during initialization, e.g. if an implementation can't
	 * create a value for the given property type.
	 */
	void initialize(A annotation, Class<?> propertyType);
}
