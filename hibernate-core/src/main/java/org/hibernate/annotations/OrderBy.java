/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Order a collection using SQL ordering (not HQL ordering).
 *
 * Different from {@link javax.persistence.OrderBy} in that this expects SQL fragment, JPA OrderBy expects a
 * valid JPQL order-by fragment.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 *
 * @see javax.persistence.OrderBy
 * @see SortComparator
 * @see SortNatural
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface OrderBy {
	/**
	 * SQL ordering clause.
	 */
	String clause();
}
