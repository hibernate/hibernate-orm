/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.Collection;

/**
 * @author Steve Ebersole
 */
public interface IdentifiableTypeMapping extends ManagedTypeMapping {
	EntityMappingHierarchy getEntityMappingHierarchy();

	IdentifiableTypeMapping getSuperTypeMapping();

	/**
	 * @todo (6.0) Should we order these?
	 * 		I'm just not sure there is a clear benefit here (beyond root first), so at the moment
	 * 		I'd lean towards no.
	 */
	Collection<IdentifiableTypeMapping> getSubTypeMappings();
}
