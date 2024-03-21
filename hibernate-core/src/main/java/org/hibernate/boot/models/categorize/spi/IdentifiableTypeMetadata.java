/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;
import java.util.function.Consumer;

/**
 * Metadata about an {@linkplain jakarta.persistence.metamodel.IdentifiableType identifiable type}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeMetadata extends ManagedTypeMetadata, TableOwner {
	/**
	 * The hierarchy in which this IdentifiableType occurs.
	 */
	EntityHierarchy getHierarchy();

	/**
	 * The super-type, if one
	 */

	IdentifiableTypeMetadata getSuperType();

	/**
	 * Whether this type is considered abstract.
	 */
	default boolean isAbstract() {
		return getClassDetails().isAbstract();
	}

	/**
	 * Whether this type has subtypes
	 */
	boolean hasSubTypes();

	/**
	 * Get the number of direct subtypes
	 */
	int getNumberOfSubTypes();

	/**
	 * Get the direct subtypes
	 */
	Iterable<IdentifiableTypeMetadata> getSubTypes();

	/**
	 * Visit each direct subtype
	 */
	void forEachSubType(Consumer<IdentifiableTypeMetadata> consumer);

	/**
	 * Event listeners in effect for this type, minus
	 * {@linkplain jakarta.persistence.ExcludeDefaultListeners default listeners}.
	 *
	 * @apiNote Kept separate from {@linkplain #getCompleteJpaEventListeners()}
	 * to facilitate types building their complete set with their
	 * {@linkplain jakarta.persistence.ExcludeSuperclassListeners superclass listeners}.
	 */
	List<JpaEventListener> getHierarchyJpaEventListeners();

	/**
	 * Event listeners in effect for this type, including
	 * {@linkplain jakarta.persistence.ExcludeDefaultListeners default listeners}
	 */
	List<JpaEventListener> getCompleteJpaEventListeners();
}
