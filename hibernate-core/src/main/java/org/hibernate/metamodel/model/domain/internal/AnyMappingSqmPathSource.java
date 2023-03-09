/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

import static jakarta.persistence.metamodel.Bindable.BindableType.SINGULAR_ATTRIBUTE;

/**
 * @author Steve Ebersole
 */
public class AnyMappingSqmPathSource<J> extends AbstractSqmPathSource<J> {
	private final SqmPathSource<?> keyPathSource;
	private final AnyDiscriminatorSqmPathSource discriminatorPathSource;

	public AnyMappingSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			AnyMappingDomainType<J> domainType,
			BindableType jpaBindableType) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		keyPathSource = new BasicSqmPathSource<>(
				"id",
				null,
				(BasicDomainType<?>) domainType.getKeyType(),
				domainType.getKeyType().getExpressibleJavaType(),
				SINGULAR_ATTRIBUTE,
				false
		);
		discriminatorPathSource = new AnyDiscriminatorSqmPathSource<>(
				localPathName,
				null,
				domainType.getDiscriminatorType(),
				jpaBindableType
		);
	}

	@Override @SuppressWarnings("unchecked")
	public AnyMappingDomainType<J> getSqmPathType() {
		return (AnyMappingDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		if ( "id".equals( name ) ) {
			return keyPathSource;
		}
		else if("{discriminator}".equals( name )) {
			return discriminatorPathSource;
		}

		throw new UnsupportedMappingException( "De-referencing parts of an ANY mapping, other than the key, is not supported" );
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
		return new SqmAnyValuedSimplePath<>( navigablePath, pathModel, lhs, lhs.nodeBuilder() );
	}
}
