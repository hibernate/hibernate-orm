/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
