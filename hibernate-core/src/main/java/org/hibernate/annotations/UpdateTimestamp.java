/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.generator.internal.CurrentTimestampGeneration;

/**
 * Specifies that the annotated field of property is a generated <em>update timestamp.</em>
 * The timestamp is regenerated every time an entity instance is updated in the database.
 * <p>
 * By default, the timestamp is generated {@linkplain java.time.Clock#instant() in memory},
 * but this may be changed by explicitly specifying the {@link #source}.
 * Otherwise, this annotation is a synonym for
 * {@link CurrentTimestamp @CurrentTimestamp(source=VM)}.
 *
 * @see CurrentTimestamp
 *
 * @author Gunnar Morling
 */
@ValueGenerationType(generatedBy = CurrentTimestampGeneration.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface UpdateTimestamp {
	/**
	 * Specifies how the timestamp is generated. By default, it is generated
	 * in memory, which saves a round trip to the database.
	 */
	SourceType source() default SourceType.VM;
}
