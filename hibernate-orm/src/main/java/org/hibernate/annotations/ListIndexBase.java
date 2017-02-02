/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines the start index value for a list index as stored on the database.  This base is subtracted from the
 * incoming database value on reads to determine the List position; it is added to the List position index when
 * writing to the database.
 *
 * By default list indexes are stored starting at zero.
 *
 * Generally used in conjunction with {@link javax.persistence.OrderColumn}.
 *
 * @see javax.persistence.OrderColumn
 *
 * @author Steve Ebersole
 */
@Retention( RUNTIME )
public @interface ListIndexBase {
	/**
	 * The list index base.  Default is 0.
	 */
	int value() default 0;
}
