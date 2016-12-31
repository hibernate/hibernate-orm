/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;
import org.hibernate.sqm.parser.criteria.tree.path.JpaPath;
import org.hibernate.sqm.parser.criteria.tree.path.JpaPathSource;

/**
 * Implementation contract for things which can be the source (parent, left-hand-side, etc) of a path
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaPathSourceImplementor<X> extends JpaPath<X>, JpaPathSource<X> {
	// todo : PropertyPath, instead of #getPathIdentifier() ?

	/**
	 * Get the string representation of this path as a navigation from one of the
	 * queries <tt>identification variables</tt>
	 *
	 * @return The path's identifier.
	 */
	String getPathIdentifier();
}
