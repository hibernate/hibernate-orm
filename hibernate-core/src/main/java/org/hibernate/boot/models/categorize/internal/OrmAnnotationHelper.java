/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.AnnotationAccessException;
import org.hibernate.models.spi.AnnotationDescriptor;

/**
 * @author Steve Ebersole
 */
public class OrmAnnotationHelper {

	public static void forEachOrmAnnotation(Consumer<AnnotationDescriptor<?>> consumer) {
		JpaAnnotations.forEachAnnotation( consumer );
		HibernateAnnotations.forEachAnnotation( consumer );
	}

	public static void forEachOrmAnnotation(Class<?> declarer, Consumer<AnnotationDescriptor<?>> consumer) {
		for ( Field field : declarer.getFields() ) {
			if ( AnnotationDescriptor.class.equals( field.getType() ) ) {
				try {
					consumer.accept( (AnnotationDescriptor<?>) field.get( null ) );
				}
				catch (IllegalAccessException e) {
					throw new AnnotationAccessException(
							String.format(
									Locale.ROOT,
									"Unable to access standard annotation descriptor field - %s",
									field.getName()
							),
							e
					);
				}
			}
		}
	}
}
