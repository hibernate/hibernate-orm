/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collection;

import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;

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
	/**
	 * Opportunity to perform any final tasks as part of initialization of the
	 * runtime model.  At this point...
	 *
	 * todo (6.0) : document the expectations of "at this point"
	 */
	void finishInitialization(
			InheritanceCapable<? super T> superTypeDescriptor,
			ManagedTypeMappingImplementor bootModelDescriptor,
			RuntimeModelCreationContext creationContext);

	InheritanceCapable<? super T> getSuperclassType();

	/**
	 * Get the sub-types for this managed type.  No specific ordering is
	 * guaranteed.
	 */
	Collection<InheritanceCapable<? extends T>> getSubclassTypes();

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

	/**
	 * @deprecated Use {@link #isSubclassTypeName(String)} instead
	 */
	@Deprecated
	boolean isSubclassEntityName(String entityName);

	/**
	 * Determine whether the given name represents a subclass (or this type itself)
	 * of the type described by this descriptor
	 */
	default boolean isSubclassTypeName(String name) {
		return isSubclassEntityName( name );
	}
}
