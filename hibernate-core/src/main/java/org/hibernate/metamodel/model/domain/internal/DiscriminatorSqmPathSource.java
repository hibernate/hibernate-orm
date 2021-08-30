/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.IllegalPathUsageException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * SqmPathSource implementation for entity discriminator
 *
 * @author Steve Ebersole
 */
public class DiscriminatorSqmPathSource<D> extends AbstractSqmPathSource<D> {
	private final EntityDomainType<?> entityDomainType;
	private final EntityMappingType entityMapping;

	public DiscriminatorSqmPathSource(
			DomainType<D> discriminatorValueType,
			EntityDomainType<?> entityDomainType,
			EntityMappingType entityMapping) {
		super( EntityDiscriminatorMapping.ROLE_NAME, discriminatorValueType, BindableType.SINGULAR_ATTRIBUTE );
		this.entityDomainType = entityDomainType;
		this.entityMapping = entityMapping;
	}

	@Override
	public SqmPath<D> createSqmPath(SqmPath<?> lhs) {
		return new DiscriminatorSqmPath( this, lhs, entityDomainType, entityMapping, lhs.nodeBuilder() );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) throws IllegalPathUsageException {
		throw new IllegalPathUsageException( "Entity discriminator cannot be de-referenced" );
	}
}
