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

	Object getDiscriminatorValue();

	/**
	 * Find a declared Navigable by name.  Returns {@code null} if a Navigable of the given
	 * name cannot be found.
	 * <p/>
	 * This form limits the returned Navigables to just those declared on this container.
	 *
	 * todo (6.0) : do we ever care about this distinction from query processing?
	 * 		^^ we might depending on how we decide to model implicit/explicit downcasts
	 */
	<N> Navigable<N> findDeclaredNavigable(String navigableName);

	/**
	 * Navigable visitation across all declared, contained Navigables
	 */
	void visitDeclaredNavigables(NavigableVisitationStrategy visitor);

	/**
	 * Determine whether the given name represents a subclass (or this type itself)
	 * of the type described by this descriptor
	 */
	boolean isSubclassTypeName(String name);

}
