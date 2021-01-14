/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNaturalIdMapping implements NaturalIdMapping {
	private final EntityMappingType declaringType;
	private final String cacheRegionName;

	private final NavigableRole role;

	public AbstractNaturalIdMapping(EntityMappingType declaringType, String cacheRegionName) {
		this.declaringType = declaringType;
		this.cacheRegionName = cacheRegionName;

		this.role = declaringType.getNavigableRole().append( PART_NAME );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return role;
	}

	public EntityMappingType getDeclaringType() {
		return declaringType;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType;
	}
}
