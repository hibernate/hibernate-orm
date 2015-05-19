/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A grouping of Hibernate-specific {@link NamedNativeQuery} definitions.  Effectively extends the named native
 * query definitions made available through {@link javax.persistence.NamedNativeQueries}.
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface NamedNativeQueries {
	/**
	 * The grouping of Hibernate named native SQL queries.
	 */
	NamedNativeQuery[] value();
}
