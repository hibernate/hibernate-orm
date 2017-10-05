/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.metamodel.model.domain.internal.FilterableNavigableSpliterator;

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

	default <N extends Navigable<?>> Spliterator<N> navigableSource(Class<N> filterType) {
		return new FilterableNavigableSpliterator<>( this, filterType, true );
	}

	default <N extends Navigable<?>> Spliterator<N> declaredNavigableSource(Class<N> filterType) {
		return new FilterableNavigableSpliterator<>( this, filterType, false );
	}

	default <N extends Navigable<?>> Stream<N> declaredNavigableStream(Class<N> filterType) {
		return StreamSupport.stream( declaredNavigableSource( filterType ), false );
	}

	/**
	 * Find a Navigable by name.  Returns {@code null} if a Navigable of the given
	 * name cannot be found.
	 * <p/>
	 * This form limits the returned Navigables to just those declared on this container.
	 */
	<N> Navigable<N> findDeclaredNavigable(String navigableName);

	/**
	 * Get all declared Navigables
	 */
	List<Navigable<?>> getDeclaredNavigables();

	/**
	 * Navigable visitation across all declared, contained Navigables
	 */
	void visitDeclaredNavigables(NavigableVisitationStrategy visitor);


	void injectSuperTypeDescriptor(InheritanceCapable<? super T> superTypeDescriptor);

	/**
	 * Do not call directly.  Use {@link #injectSuperTypeDescriptor} instead.
	 */
	void addSubclassType(InheritanceCapable<? extends T> subclassType);
}
