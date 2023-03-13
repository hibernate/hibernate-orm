/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity;

import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * @author Steve Ebersole
 */
@Internal
public class DiscriminatorValueDetailsImpl implements EntityDiscriminatorMapping.DiscriminatorValueDetails {
	private final Object value;
	private final String jdbcLiteral;
	private final EntityMappingType matchedEntityDescriptor;

	public DiscriminatorValueDetailsImpl(Object value, String jdbcLiteral, EntityMappingType matchedEntityDescriptor) {
		this.value = value;
		this.jdbcLiteral = jdbcLiteral;
		this.matchedEntityDescriptor = matchedEntityDescriptor;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public Object getSqlLiteralValue() {
		return jdbcLiteral;
	}

	@Override
	public EntityMappingType getIndicatedEntity() {
		return matchedEntityDescriptor;
	}
}
