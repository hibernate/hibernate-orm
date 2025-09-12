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
import org.hibernate.query.ReturnableType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * SqmPathSource implementation for entity discriminator
 *
 * @author Steve Ebersole
 */
public class DiscriminatorSqmPathSource<D> extends AbstractSqmPathSource<D>
		implements ReturnableType<D> {
	private final EntityDomainType<?> entityDomainType;
	private final EntityMappingType entityMapping;

	public DiscriminatorSqmPathSource(
			DomainType<D> discriminatorValueType,
			EntityDomainType<?> entityDomainType,
			EntityMappingType entityMapping) {
		super( EntityDiscriminatorMapping.ROLE_NAME, null, discriminatorValueType, BindableType.SINGULAR_ATTRIBUTE );
		this.entityDomainType = entityDomainType;
		this.entityMapping = entityMapping;
	}

	@Override
	public SqmPath<D> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final NavigablePath navigablePath;
		if ( intermediatePathSource == null ) {
			navigablePath = lhs.getNavigablePath().append( getPathName() );
		}
		else {
			navigablePath = lhs.getNavigablePath().append( intermediatePathSource.getPathName() ).append( getPathName() );
		}
		return new EntityDiscriminatorSqmPath( navigablePath, pathModel, lhs, entityDomainType, entityMapping, lhs.nodeBuilder() );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new IllegalStateException( "Entity discriminator cannot be de-referenced" );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class<D> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public DomainType<D> getSqmType() {
		return this;
	}
}
