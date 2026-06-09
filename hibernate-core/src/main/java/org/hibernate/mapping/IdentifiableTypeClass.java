/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;

/**
 * Commonality between {@link PersistentClass} and {@link MappedSuperclass},
 * what JPA calls an {@linkplain jakarta.persistence.metamodel.IdentifiableType identifiable type}.
 * <p>
 * The supertype/subtype methods model the direct managed-type graph.  They are
 * intentionally distinct from entity-inheritance compatibility methods such as
 * {@link PersistentClass#getSuperclass()} and
 * {@link PersistentClass#getDirectSubclasses()}.
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeClass extends TableContainer {
	/**
	 * The direct managed-type supertype.
	 * <p>
	 * For an entity whose Java superclass is a mapped superclass, this returns
	 * the mapped-superclass mapping node rather than skipping directly to the
	 * nearest entity superclass.  Use entity-specific compatibility helpers when
	 * the caller needs only entity inheritance.
	 */
	IdentifiableTypeClass getSuperType();

	/**
	 * The direct managed-type subtypes.
	 * <p>
	 * This list may include entities and mapped superclasses.  For entity-only
	 * subclass traversal, use {@link PersistentClass#getDirectSubclasses()}.
	 */
	List<IdentifiableTypeClass> getSubTypes();

	List<Property> getDeclaredProperties();

	Component getIdentifierMapper();

	Table getImplicitTable();

	boolean isVersioned();
	Property getVersion();
}
