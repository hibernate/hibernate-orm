/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations;

import org.jboss.jandex.AnnotationInstance;

/**
 * A contract for extracting values from Jandex representation of an
 * annotation.
 *
 * @author Steve Ebersole
 */
public interface TypedValueExtractor<T> {
	/**
	 * Extracts the value by type from the given annotation attribute value
	 * representation.  The attribute value may be {@code null} (which
	 * represents an unspecified attribute), in which case we need reference
	 * to the {@link org.hibernate.boot.registry.classloading.spi.ClassLoaderService}
	 * to be able to resolve the default value for the given attribute.
	 *
	 * @param annotationInstance The representation of the annotation usage
	 * from which to extract an attribute value.
	 * @param name The name of the attribute to extract
	 *
	 * @return The extracted value.
	 */
	public T extract(AnnotationInstance annotationInstance, String name);

	/**
	 * Just like {@link #extract(org.jboss.jandex.AnnotationInstance,String)}
	 * except that here we return the passed defaultValue if the annotation
	 * attribute value is {@code null}.
	 *
	 * @param annotationInstance The representation of the annotation usage
	 * from which to extract an attribute value.
	 * @param name The name of the attribute to extract
	 * @param defaultValue The typed value to use if the annotation
	 * attribute value is {@code null}
	 *
	 * @return The extracted value.
	 */
	public T extract(AnnotationInstance annotationInstance, String name, T defaultValue);
}
