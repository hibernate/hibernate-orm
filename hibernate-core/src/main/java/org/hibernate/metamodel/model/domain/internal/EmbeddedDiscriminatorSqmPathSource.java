/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * SqmPathSource implementation for embeddable discriminator
 *
 * @author Marco Belladelli
 */
public class EmbeddedDiscriminatorSqmPathSource<D> extends AbstractDiscriminatorSqmPathSource<D> {
	private final EmbeddableDomainType<?> embeddableDomainType;

	public EmbeddedDiscriminatorSqmPathSource(DomainType<D> discriminatorType, EmbeddableDomainType<?> embeddableDomainType) {
		super( discriminatorType );
		this.embeddableDomainType = embeddableDomainType;
	}

	@Override
	public SqmPath<D> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		//noinspection unchecked
		return new EmbeddedDiscriminatorSqmPath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				(EmbeddableDomainType<D>) embeddableDomainType,
				lhs.nodeBuilder()
		);
	}
}
