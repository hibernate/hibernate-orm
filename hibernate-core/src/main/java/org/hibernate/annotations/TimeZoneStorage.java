/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Specifies how the time zone information of a persistent property or field should be persisted.
 * The <code>TimeZoneStorage</code> annotation may be used in conjunction with the <code>Basic</code> annotation, or in
 * conjunction with the <code>ElementCollection</code> annotation when the
 * element collection value is of basic type. If the <code>TimeZoneStorage</code> annotation is not
 * used, the <code>TimeZoneStorageType</code> value is assumed to be <code>NORMALIZED</code>.
 *
 * <pre>
 *   Example:
 *
 *   &#064;Entity public class Person {
 *       public OffsetDateTime getBirthDateTimeNormalized() {...}
 *
 *       &#064;TimeZoneStorage
 *       &#064;TimeZoneColumn(column = &#064;Column(...))
 *       public OffsetDateTime getBirthDateTimeNativeOrColumn() {...}
 *       ...
 *   }
 * </pre>
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 * @author Andrea Boriero
 * @see TimeZoneColumn
 */
@Incubating
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD })
public @interface TimeZoneStorage {
	/**
	 * The storage strategy for the time zone information.
	 */
	TimeZoneStorageType value() default TimeZoneStorageType.AUTO;
}
