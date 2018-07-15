/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.tuple.UpdateTimestampGeneration;

/**
 * Marks a property as the update timestamp of the containing entity. The property value will be set to the current VM
 * date whenever the owning entity is updated.
 * <p>
 * Supported property types:
 * <ul>
 * <li>{@link java.util.Date}</li>
 * <li>{@link java.util.Calendar}</li>
 * <li>{@link java.sql.Date}</li>
 * <li>{@link java.sql.Time}</li>
 * <li>{@link java.sql.Timestamp}</li>
 * <li>{@link java.time.Instant}</li>
 * <li>{@link java.time.LocalDate}</li>
 * <li>{@link java.time.LocalDateTime}</li>
 * <li>{@link java.time.LocalTime}</li>
 * <li>{@link java.time.MonthDay}</li>
 * <li>{@link java.time.OffsetDateTime}</li>
 * <li>{@link java.time.OffsetTime}</li>
 * <li>{@link java.time.Year}</li>
 * <li>{@link java.time.YearMonth}</li>
 * <li>{@link java.time.ZonedDateTime}</li>
 * </ul>
 *
 * @author Gunnar Morling
 */
@ValueGenerationType(generatedBy = UpdateTimestampGeneration.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD })
public @interface UpdateTimestamp {
}
