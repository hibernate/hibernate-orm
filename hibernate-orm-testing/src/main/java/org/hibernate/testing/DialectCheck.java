/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing;

import org.hibernate.dialect.Dialect;

/**
 * Defines a means to check {@link Dialect} features for use in "test protection" checks.  Used from
 * {@link RequiresDialectFeature}
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public interface DialectCheck {
	/**
	 * Does the given dialect match the defined check?
	 *
	 * @param dialect The dialect against which to check
	 *
	 * @return {@code true} if it matches; {@code false} otherwise.
	 */
	public boolean isMatch(Dialect dialect);
}
