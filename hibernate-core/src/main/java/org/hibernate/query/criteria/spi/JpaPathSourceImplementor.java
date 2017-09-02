/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.metamodel.Bindable;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;

/**
 * Implementation contract for things which can be the source of a path
 * (parent, left-hand-side, etc - whatever term you like)
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaPathSourceImplementor<X> extends JpaPathImplementor<X>, JpaExpressionImplementor<X> {
	// todo : PropertyPath, instead of #getPathIdentifier() ?

	Navigable<X> getReferencedNavigable();

	NavigablePath getNavigablePath();

	/**
	 * Get the string representation of this path as a navigation from one of the
	 * queries <tt>identification variables</tt>
	 *
	 * @return The path's identifier.
	 *
	 * @deprecated Use {@link #getNavigablePath()}
	 */
	@Deprecated
	default String getPathIdentifier() {
		return getNavigablePath().getFullPath();
	}

	// todo (6.0) : have Navigable implement JPA's Bindable

	/**
	 * Return the bindable object that corresponds to the
	 * path expression.
	 *
	 * @return bindable object corresponding to the path
	 */
	Bindable<X> getModel();

	/**
	 * Return the parent "node" in the path or null if no parent.
	 *
	 * @return parent
	 */
	JpaPathSourceImplementor<?> getParentPath();
}
