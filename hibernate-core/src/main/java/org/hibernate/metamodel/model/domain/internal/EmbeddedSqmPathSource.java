/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.SqmJoinable;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmEmbeddableDomainType;

/**
 * @author Steve Ebersole
 */
public class EmbeddedSqmPathSource<J>
		extends AbstractSqmPathSource<J>
		implements CompositeSqmPathSource<J> {
	private final boolean isGeneric;
	private final SqmEmbeddableDomainType<J> domainType;

	public EmbeddedSqmPathSource(
			String localPathName,
			@Nullable SqmPathSource<J> pathModel,
			SqmEmbeddableDomainType<J> domainType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.domainType = domainType;
		this.isGeneric = isGeneric;
	}

	@Override
	public SqmEmbeddableDomainType<J> getPathType() {
		return domainType;
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		return getPathType().findSubPathSource( name );
	}

	@Override
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		return new SqmEmbeddedValuedSimplePath<>(
				pathModel instanceof SqmJoinable<?, ?> sqmJoinable
						? sqmJoinable.createNavigablePath( lhs, null )
						: PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}
}
