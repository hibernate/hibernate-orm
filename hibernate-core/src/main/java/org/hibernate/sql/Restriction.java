/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import org.hibernate.Internal;

/**
 * A restriction (predicate) to be applied to a query
 *
 * @author Steve Ebersole
 */
@Internal
public interface Restriction {
	/**
	 * Render the restriction into the SQL buffer
	 */
	void render(StringBuilder sqlBuffer, RestrictionRenderingContext context);
}
