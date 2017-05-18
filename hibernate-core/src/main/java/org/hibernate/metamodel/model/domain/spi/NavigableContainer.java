/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface NavigableContainer<T> extends Navigable<T> {
	/**
	 * Find a Navigable by name.  Returns {@code null} if a Navigable of the given
	 * name cannot be found.
	 * <p/>
	 * This form returns Navigables declared here as well as Navigables declared
	 * on the super.
	 */
	<N> Navigable<N> findNavigable(String navigableName);

	/**
	 * Find a Navigable by name.  Returns {@code null} if a Navigable of the given
	 * name cannot be found.
	 * <p/>
	 * This form limits the returned Navigables to just those declared on this container.
	 */
	<N> Navigable<N> findDeclaredNavigable(String navigableName);

	/**
	 * Get all (declared+super) Navigables
	 */
	List<Navigable> getNavigables();

	/**
	 * Get all declared Navigabled
	 */
	List<Navigable> getDeclaredNavigables();

	/**
	 * Navigable visitation across all (declared+super) contained Navigables
	 */
	void visitNavigables(NavigableVisitationStrategy visitor);

	/**
	 * Navigable visitation across all declared, contained Navigables
	 */
	void visitDeclaredNavigables(NavigableVisitationStrategy visitor);
}
