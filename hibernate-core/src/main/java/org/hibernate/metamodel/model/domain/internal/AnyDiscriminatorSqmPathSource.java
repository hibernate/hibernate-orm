/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * SqmPathSource implementation for {@link org.hibernate.annotations.AnyDiscriminator}
 *
 */
public class AnyDiscriminatorSqmPathSource<D> extends AbstractSqmPathSource<D>
		implements ReturnableType<D> {

	public AnyDiscriminatorSqmPathSource(
			String localPathName,
			AnyDiscriminatorDomainTypeImpl domainType,
			BindableType jpaBindableType) {
		super( localPathName, domainType, jpaBindableType );
	}

	@Override
	public SqmPath<D> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final NavigablePath navigablePath;
		if ( intermediatePathSource == null || intermediatePathSource.getPathName().equals( CollectionPart.Nature.ELEMENT.getName() ) ) {
			navigablePath = lhs.getNavigablePath();
		}
		else {
			navigablePath = lhs.getNavigablePath().append( intermediatePathSource.getPathName() );
		}
		return new AnyDiscriminatorSqmPath( navigablePath, this, lhs, lhs.nodeBuilder() );
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
	public AnyDiscriminatorDomainTypeImpl<D> getSqmPathType() {
		return (AnyDiscriminatorDomainTypeImpl<D>) super.getSqmPathType();
	}

	@Override
	public JavaType<D> getExpressibleJavaType() {
		return getSqmPathType().getExpressibleJavaType();
	}
}
