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
