/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class BasicSqmPathSource<J>
		extends AbstractSqmPathSource<J>
		implements ReturnableType<J> {
	private final JavaType<?> relationalJavaType;
	private final boolean isGeneric;

	public BasicSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			BasicDomainType<J> domainType,
			JavaType<?> relationalJavaType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.relationalJavaType = relationalJavaType;
		this.isGeneric = isGeneric;
	}

	@Override
	public BasicDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (BasicDomainType<J>) super.getSqmPathType();
	}

	@Override
	public DomainType<J> getSqmType() {
		return getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new IllegalStateException( "Basic paths cannot be dereferenced" );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final NavigablePath navigablePath;
		if ( intermediatePathSource == null ) {
			navigablePath = lhs.getNavigablePath().append( getPathName() );
		}
		else {
			navigablePath = lhs.getNavigablePath().append( intermediatePathSource.getPathName() ).append( getPathName() );
		}
		return new SqmBasicValuedSimplePath<>(
				navigablePath,
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return relationalJavaType;
	}

	@Override
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public String toString() {
		return "BasicSqmPathSource(" +
				getPathName() + " : " + getJavaType().getSimpleName() +
				")";
	}
}
