/*
 * SPDX-License-Identifier: Apache-2.0
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
 * By default, the timestamp is generated {@linkplain java.time.Clock#instant in memory},
 * but this may be changed by explicitly specifying the {@link #source}.
 * Otherwise, this annotation is a synonym for
 * {@link CurrentTimestamp @CurrentTimestamp(source=VM)}.
 * <p>
 * The annotated property may be of any one of the following types:
 * {@link java.util.Date},
 * {@link java.util.Calendar},
 * {@link java.sql.Date},
 * {@link java.sql.Time},
 * {@link java.sql.Timestamp},
 * {@link java.time.Instant},
 * {@link java.time.LocalDate},
 * {@link java.time.LocalDateTime},
 * {@link java.time.LocalTime},
 * {@link java.time.MonthDay},
 * {@link java.time.OffsetDateTime},
 * {@link java.time.OffsetTime},
 * {@link java.time.Year},
 * {@link java.time.YearMonth}, or
 * {@link java.time.ZonedDateTime}.
 * <p>
 * A field annotated {@code @UpdateTimestamp} may not be directly set by the application
 * program.
 *
 * @see CurrentTimestamp
 * @see CreationTimestamp
 *
 * @author Gunnar Morling
 */
@ValueGenerationType(generatedBy = CurrentTimestampGeneration.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface UpdateTimestamp {
	/**
	 * Specifies how the timestamp is generated. By default, it is generated
	 * in memory, which might save a round trip to the database, depending on
	 * the capabilities of the database and JDBC driver.
	 */
	SourceType source() default SourceType.VM;
}
