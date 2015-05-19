/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria;
import javax.persistence.criteria.Path;

import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * Implementation contract for things which can be the source (parent, left-hand-side, etc) of a path
 *
 * @author Steve Ebersole
 */
public interface PathSource<X> extends Path<X> {
	public void prepareAlias(RenderingContext renderingContext);

	/**
	 * Get the string representation of this path as a navigation from one of the
	 * queries <tt>identification variables</tt>
	 *
	 * @return The path's identifier.
	 */
	public String getPathIdentifier();
}
