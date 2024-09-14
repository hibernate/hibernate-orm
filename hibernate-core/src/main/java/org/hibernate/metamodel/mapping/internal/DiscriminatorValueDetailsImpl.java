/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorValueDetailsImpl implements DiscriminatorValueDetails {
	private final Object value;
	private final EntityMappingType matchedEntityDescriptor;

	public DiscriminatorValueDetailsImpl(Object value, EntityMappingType matchedEntityDescriptor) {
		this.value = value;
		this.matchedEntityDescriptor = matchedEntityDescriptor;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public EntityMappingType getIndicatedEntity() {
		return matchedEntityDescriptor;
	}
}
