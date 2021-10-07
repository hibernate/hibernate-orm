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

import jakarta.persistence.Column;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Specifies the column name and type to use for storing the time zone information.
 * The annotation can be used in conjunction with the <code>TimeZoneStorageType.AUTO</code> and
 * <code>TimeZoneStorageType.COLUMN</code>. The column is simply ignored if <code>TimeZoneStorageType.AUTO</code>
 * is used and the database supports native time zone storage.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 * @author Andrea Boriero
 * @see TimeZoneStorage
 * @see TimeZoneStorageType#COLUMN
 * @see TimeZoneStorageType#AUTO
 */
@Incubating
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD })
public @interface TimeZoneColumn {

	/**
	 * The column for the time zone information.
	 */
	Column column();

	/**
	 * The storage type for the time zone information.
	 */
	TimeZoneType type() default TimeZoneType.OFFSET;

}
