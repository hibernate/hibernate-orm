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
