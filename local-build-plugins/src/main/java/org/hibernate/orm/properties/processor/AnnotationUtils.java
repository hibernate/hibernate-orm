/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties.processor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;


public final class AnnotationUtils {

	private AnnotationUtils() {
	}

	public static boolean isIgnored(Element element) {
		return findAnnotation( element, HibernateOrmConfiguration.class )
				.flatMap( a -> a.attribute( "ignore", Boolean.class ) )
				.orElse( false );
	}

	public static Optional<AnnotationAttributeHolder> findAnnotation(Element element, Class<?> annotation) {
		for ( AnnotationMirror mirror : element.getAnnotationMirrors() ) {
			if ( mirror.getAnnotationType().toString().equals( annotation.getName() ) ) {
				return Optional.of( new AnnotationAttributeHolder( mirror ) );
			}
		}
		return Optional.empty();
	}

	public static class AnnotationAttributeHolder {
		private final AnnotationMirror annotationMirror;

		private AnnotationAttributeHolder(AnnotationMirror annotationMirror) {
			this.annotationMirror = annotationMirror;
		}

		public <T> Optional<T> attribute(String name, Class<T> klass) {
			return annotationMirror.getElementValues().entrySet().stream()
					.filter( entry -> entry.getKey().getSimpleName().contentEquals( name ) )
					.map( entry -> klass.cast( entry.getValue().getValue() ) )
					.findAny();
		}

		public <T> Optional<List<T>> multiAttribute(String name, Class<T> klass) {
			return annotationMirror.getElementValues().entrySet().stream()
					.filter( entry -> entry.getKey().getSimpleName().contentEquals( name ) )
					.map( entry -> entry.getValue().getValue() )
					.map( obj -> ( (List<?>) List.class.cast( obj ) ) )
					.map( list -> list.stream().map( AnnotationValue.class::cast ).map( AnnotationValue::getValue )
							.map( klass::cast ).collect( Collectors.toList() ) )
					.findAny();
		}
	}

}
