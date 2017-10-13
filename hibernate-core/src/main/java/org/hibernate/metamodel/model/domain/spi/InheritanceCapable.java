/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collection;

/**
 * Specialization of ManagedTypeImplementor for types for which
 * we support *mapped* inheritance.
 * <p/>
 * NOTE: parameterized to eventually support embeddables, for which we
 * do not currently support inheritance but know we want to
 *
 * @author Steve Ebersole
 */
public interface InheritanceCapable<T> extends ManagedTypeDescriptor<T> {
	InheritanceCapable<? super T> getSuperclassType();

	/**
	 * Get the sub-types for this managed type.  No specific ordering is
	 * guaranteed.
	 */
	Collection<InheritanceCapable<? extends T>> getSubclassTypes();

	/**
	 * Determine whether the given name represents a subclass entity
	 * (or this entity itself) of the entity mapped by this persister.
	 *
	 * @param entityName The entity name to be checked.
	 * @return True if the given entity name represents either the entity
	 * mapped by this persister or one of its subclass entities; false
	 * otherwise.
	 */
	boolean isSubclassEntityName(String entityName);

	/**
	 * Find a declared Navigable by name.  Returns {@code null} if a Navigable of the given
	 * name cannot be found.
	 * <p/>
	 * This form limits the returned Navigables to just those declared on this container.
	 *
	 * todo (6.0) : do we ever care about this distinction from query processing?
	 */
	<N> Navigable<N> findDeclaredNavigable(String navigableName);

	/**
	 * Navigable visitation across all declared, contained Navigables
	 */
	void visitDeclaredNavigables(NavigableVisitationStrategy visitor);

	/**
	 * todo (6.0) : this should be part of the complete / finishInitialization handling
	 * 		which means it can probably be hidden in the abstract impls.  Would be nice to
	 * 		not expose such mutators
	 */
	void injectSuperTypeDescriptor(InheritanceCapable<? super T> superTypeDescriptor);

	/**
	 * Do not call directly.  Use {@link #injectSuperTypeDescriptor} instead.
	 *
	 * todo (6.0) : again, as with `#injectSuperTypeDescriptor`, I think this can be a detail within the abstract impl
	 */
	void addSubclassType(InheritanceCapable<? extends T> subclassType);
}
