/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class EmbeddedSqmPathSource<J>
		extends AbstractSqmPathSource<J>
		implements CompositeSqmPathSource<J> {
	private final boolean isGeneric;

	public EmbeddedSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			EmbeddableDomainType<J> domainType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.isGeneric = isGeneric;
	}

	@Override
	public EmbeddableDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (EmbeddableDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return (SqmPathSource<?>) getSqmPathType().findAttribute( name );
	}

	@Override
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final NavigablePath navigablePath;
		if ( pathModel instanceof SqmJoinable<?, ?> ) {
			navigablePath = ( (SqmJoinable<?, ?>) pathModel ).createNavigablePath( lhs, null );
		}
		else if ( intermediatePathSource == null ) {
			navigablePath = lhs.getNavigablePath().append( getPathName() );
		}
		else {
			navigablePath = lhs.getNavigablePath().append( intermediatePathSource.getPathName() ).append( getPathName() );
		}
		return new SqmEmbeddedValuedSimplePath<>(
				navigablePath,
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}
}
