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
public interface InheritanceCapable<T> extends ManagedTypeImplementor<T> {
	/**
	 *
	 */
	InheritanceCapable<? super T> getSuperclassType();

	/**
	 * Get the sub-types for this managed type.  No specific ordering is
	 * guaranteed.
	 */
	Collection<InheritanceCapable<? extends T>> getSubclassTypes();

	void injectSuperTypeDescriptor(InheritanceCapable<? super T> superTypeDescriptor);

	/**
	 * Do not call directly.  Use {@link #injectSuperTypeDescriptor} instead.
	 */
	void addSubclassType(InheritanceCapable<? extends T> subclassType);
}
