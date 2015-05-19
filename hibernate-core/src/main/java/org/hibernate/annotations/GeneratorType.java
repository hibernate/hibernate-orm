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

import org.hibernate.tuple.ValueGenerator;
import org.hibernate.tuple.VmValueGeneration;

/**
 * Marks a property as generated, specifying the {@link ValueGenerator} type to be used for generating the value. It is
 * the responsibility of the client to ensure that a generator type is specified which matches the data type of the
 * annotated property.
 *
 * @author Gunnar Morling
 */
@ValueGenerationType( generatedBy = VmValueGeneration.class )
@Retention( RetentionPolicy.RUNTIME )
@Target( value = { ElementType.FIELD, ElementType.METHOD } )
public @interface GeneratorType {

	/**
	 * The value generator type
	 *
	 * @return the value generator type
	 */
	Class<? extends ValueGenerator<?>> type();

	/**
	 * When to generate the value, either only during insert or during insert and update of the hosting entity.
	 *
	 * @return the time of generation
	 */
	GenerationTime when() default GenerationTime.ALWAYS;
}
