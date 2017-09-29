/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import java.util.function.Predicate;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

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

	/**
	 * Reduce an instance of the described type into an array of it's
	 * sub-Navigable state
	 *
	 * @apiNote The returned array is of length equal to the number of
	 * sub-Navigables.  Each element in that array represents the
	 * corresponding sub-Navigable's reduced state (see {@link Navigable#reduce}).
	 */
	default Object[] reduceNavigables(J instance, SharedSessionContractImplementor session) {
		return reduceNavigables(
				instance,
				o -> true,
				o -> false,
				null,
				session
		);
	}

	/**
	 * Reduce an instance of the described type into an array whose length
	 * is equal to the number of sub-Navigables where the `includeCondition`
	 * tests {@code true}.  Each element corresponds to either:
	 *
	 * 		* if the passed `swapCondition` tests {@code true}, then
	 * 			the value passed as `swapValue`
	 * 		* otherwise the sub-Navigable's reduced state (see {@link Navigable#reduce})
	 *
	 * In more specific terms, this method is responsible for extracting the domain
	 * object's value state array - which is the form we use in many places such
	 * EntityEntry#loadedState, L2 cache entry, etc.
	 *
	 * @param instance An instance of the described type (this)
	 * @param includeCondition Predicate to see if the given sub-Navigable should create
	 * 		an index in the array being built.
	 * @param swapCondition Predicate to see if the sub-Navigable's reduced state or
	 * 		the passed `swapValue` should be used for that sub-Navigable's value as its
	 *		element in the array being built
	 * @param swapValue The value to use if the passed `swapCondition` tests {@code true}
	 * @param session The session :)
	 */
	Object[] reduceNavigables(
			J instance,
			Predicate includeCondition,
			Predicate swapCondition,
			Object swapValue,
			SharedSessionContractImplementor session);
}
