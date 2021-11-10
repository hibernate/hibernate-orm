/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

import static jakarta.persistence.metamodel.Bindable.BindableType.SINGULAR_ATTRIBUTE;

/**
 * @author Steve Ebersole
 */
public class AnyMappingSqmPathSource<J> extends AbstractSqmPathSource<J> {
	private final SqmPathSource<?> keyPathSource;

	@SuppressWarnings("WeakerAccess")
	public AnyMappingSqmPathSource(
			String localPathName,
			AnyMappingDomainType<J> domainType,
			BindableType jpaBindableType) {
		super( localPathName, domainType, jpaBindableType );
		keyPathSource = new BasicSqmPathSource( "id", (BasicDomainType) domainType.getKeyType(), SINGULAR_ATTRIBUTE );
	}

	@Override
	public AnyMappingDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (AnyMappingDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		if ( "id".equals( name ) ) {
			return keyPathSource;
		}

		throw new UnsupportedOperationException( "De-referencing parts of an ANY mapping, other than the key, is not supported" );
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
		return new SqmAnyValuedSimplePath<>( navigablePath, this, lhs, lhs.nodeBuilder() );
	}

}
