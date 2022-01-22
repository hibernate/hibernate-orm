/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.BindableType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class BasicSqmPathSource<J>
		extends AbstractSqmPathSource<J>
		implements BindableType<J>, ReturnableType<J> {

	public BasicSqmPathSource(
			String localPathName,
			BasicDomainType<J> domainType,
			BindableType jpaBindableType) {
		super( localPathName, domainType, jpaBindableType );
	}

	@Override
	public BasicDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (BasicDomainType<J>) super.getSqmPathType();
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
				this,
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
		return getExpressableJavaType().getJavaTypeClass();
	}

	@Override
	public String toString() {
		return "BasicSqmPathSource(" +
				getPathName() + " : " + getJavaType().getSimpleName() +
				")";
	}
}
