/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.tuple.CurrentTimestampGeneration;

/**
 * Specifies that the annotated field of property is a generated <em>creation timestamp</em>.
 * The timestamp is generated just once, when an entity instance is inserted in the database.
 * <p>
 * By default, the timestamp is generated by {@linkplain java.time.Clock#instant() in memory},
 * but this may be changed by explicitly specifying the {@link #source}.
 * Otherwise, this annotation is a synonym for
 * {@link CurrentTimestamp @CurrentTimestamp(timing=INSERT,source=VM)}.
 *
 * @author Gunnar Morling
 *
 * @see CurrentTimestamp
 */
@ValueGenerationType(generatedBy = CurrentTimestampGeneration.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface CreationTimestamp {
	/**
	 * Specifies how the timestamp is generated. By default, it is generated
	 * in memory, which saves a round trip to the database.
	 */
	SourceType source() default SourceType.VM;
}
