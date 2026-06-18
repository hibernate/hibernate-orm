/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.DiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * {@link SqmPath} specialization for an embeddable discriminator
 *
 * @author Marco Belladelli
 */
public class EmbeddedDiscriminatorSqmPath<T> extends AbstractSqmPath<T> implements DiscriminatorSqmPath<T> {
	private final EmbeddableDomainType<T> embeddableDomainType;

	protected EmbeddedDiscriminatorSqmPath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			EmbeddableDomainType<T> embeddableDomainType,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
		this.embeddableDomainType = embeddableDomainType;
	}

	public EmbeddableDomainType<T> getEmbeddableDomainType() {
		return embeddableDomainType;
	}

	@Override
	public @Nonnull SqmPath<?> getLhs() {
		return castNonNull( super.getLhs() );
	}

	@Override
	public @Nonnull EmbeddedDiscriminatorSqmPathSource<T> getExpressible() {
//		return (EmbeddedDiscriminatorSqmPathSource<T>) getNodeType();
		return (EmbeddedDiscriminatorSqmPathSource<T>) getReferencedPathSource();
	}

	@Override
	public EmbeddedDiscriminatorSqmPath<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		return context.registerCopy(
				this,
				(EmbeddedDiscriminatorSqmPath<T>) getLhs().copy( context ).type()
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitDiscriminatorPath( this );
	}

}
