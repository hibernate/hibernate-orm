/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.NotYetImplementedFor6Exception;

/**
 * @author Steve Ebersole
 */
public interface NavigableContainer<J> extends Navigable<J> {
	default Spliterator<Navigable> navigableSource() {
		throw new NotYetImplementedFor6Exception();
	}

	default Spliterator<Navigable> declaredNavigableSource() {
		throw new NotYetImplementedFor6Exception();
	}

	default Stream<Navigable> navigableStream() {
		return StreamSupport.stream( navigableSource(), false );
	}

	default Stream<Navigable> declaredNavigableStream() {
		return StreamSupport.stream( declaredNavigableSource(), false );
	}

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
	 * Get all declared Navigables
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
