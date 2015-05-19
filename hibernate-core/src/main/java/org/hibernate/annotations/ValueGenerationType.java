/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.tuple.AnnotationValueGeneration;

/**
 * Marks an annotation type as a generator annotation type.
 * <p>
 * Adding a generator annotation to an entity property causes the value of the property to be generated upon insert
 * or update of the owning entity. Not more than one generator annotation may be placed on a given property.
 * <p>
 * Each generator annotation type is associated with a {@link AnnotationValueGeneration} which implements the strategy
 * for generating the value. Generator annotation types may define arbitrary custom attributes, e.g. allowing the
 * client to configure the generation timing (if applicable) or other settings taking an effect on the value generation.
 * The corresponding implementation can retrieve these settings from the annotation instance passed to
 * {@link AnnotationValueGeneration#initialize(java.lang.annotation.Annotation, Class)}.
 * <p>
 * Custom generator annotation types must have retention policy {@link RetentionPolicy#RUNTIME}.

 * @author Gunnar Morling
 */
@Target( value = ElementType.ANNOTATION_TYPE )
@Retention( RetentionPolicy.RUNTIME )
public @interface ValueGenerationType {

	/**
	 * The type of value generation associated with the annotated value generator annotation type. The referenced
	 * generation type must be parameterized with the type of the given generator annotation.
	 *
	 * @return the value generation type
	 */
	Class<? extends AnnotationValueGeneration<?>> generatedBy();
}
