/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * SqmPathSource implementation for {@link org.hibernate.annotations.AnyDiscriminator}
 *
 */
public class AnyDiscriminatorSqmPathSource<D> extends AbstractSqmPathSource<D>
		implements ReturnableType<D> {

	public AnyDiscriminatorSqmPathSource(
			String localPathName,
			SqmPathSource<D> pathModel,
			SimpleDomainType<D> domainType,
			BindableType jpaBindableType) {
		super( localPathName, pathModel, domainType, jpaBindableType );
	}

	@Override
	public SqmPath<D> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final NavigablePath navigablePath;
		if ( intermediatePathSource == null ) {
			navigablePath = lhs.getNavigablePath();
		}
		else {
			navigablePath = lhs.getNavigablePath().append( intermediatePathSource.getPathName() );
		}
		return new AnyDiscriminatorSqmPath( navigablePath, pathModel, lhs, lhs.nodeBuilder() );
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
	public BasicType<D> getSqmPathType() {
		return (BasicType<D>) super.getSqmPathType();
	}

	@Override
	public DomainType<D> getSqmType() {
		return getSqmPathType();
	}

	@Override
	public JavaType<D> getExpressibleJavaType() {
		return getSqmPathType().getExpressibleJavaType();
	}
}
