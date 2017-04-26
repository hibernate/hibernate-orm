/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface NavigableSource<T> extends Navigable<T> {
	/**
	 * Find a Navigable by name.  Returns {@code null} if the given
	 * "navigable name" cannot be resolved.
	 *
	 * @param navigableName The name to resolve relative to this source/container.
	 *
	 * @return The resolve navigable, or {@code null}
	 */
	<N> Navigable<N> findNavigable(String navigableName);

	<N> Navigable<N> findDeclaredNavigable(String navigableName);

	void visitNavigables(NavigableVisitationStrategy visitor);

	void visitDeclaredNavigables(NavigableVisitationStrategy visitor);

	// todo (6.0) : overload this for entity- and collection-valued attributes
	//		but that requires splitting SingularAttributeEntity into interface/impl
	//		and moving the interface into SPI
	//
	// todo (6.0) : may want to do this completely differently.
	// 		As-is, this leaks into places it should not.
	List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute);
}
