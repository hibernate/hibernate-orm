/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;

/**
 * Commonality between {@link PersistentClass} and {@link MappedSuperclass},
 * what JPA calls an {@linkplain jakarta.persistence.metamodel.IdentifiableType identifiable type}.
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeClass extends TableContainer {
	IdentifiableTypeClass getSuperType();
	List<IdentifiableTypeClass> getSubTypes();

	List<Property> getDeclaredProperties();

	Table getImplicitTable();

	void applyProperty(Property property);
}
