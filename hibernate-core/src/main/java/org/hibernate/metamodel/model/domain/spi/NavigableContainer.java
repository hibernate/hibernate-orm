/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.function.Consumer;

import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface NavigableContainer<J> extends Navigable<J> {
	/**
	 * Find a Navigable by name.  Returns {@code null} if a Navigable of the given
	 * name cannot be found.
	 * <p/>
	 * This form returns Navigables declared here as well as Navigables declared
	 * on the super.
	 */
	<N> Navigable<N> findNavigable(String navigableName);

	/**
	 * Navigable visitation across all (declared+super) contained Navigables
	 */
	void visitNavigables(NavigableVisitationStrategy visitor);

	default void visitKeyFetchables(Consumer<Fetchable> fetchableConsumer) {
		// by default, nothing to do
	}

	void visitFetchables(Consumer<Fetchable> fetchableConsumer);
}
