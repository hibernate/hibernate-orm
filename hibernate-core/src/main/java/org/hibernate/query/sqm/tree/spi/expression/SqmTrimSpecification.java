/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.AbstractSqmNode;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;

import java.util.Objects;

/**
 * Needed to pass TrimSpecification as an SqmExpression when we call out to
 * SqmFunctionTemplates handling TRIM calls as a function argument.
 *
 * @author Steve Ebersole
 */
public class SqmTrimSpecification extends AbstractSqmNode implements SqmTypedNode<Void> {
	private final TrimSpec specification;

	public SqmTrimSpecification(@Nonnull TrimSpec specification, @Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.specification = specification;
	}

	@Nonnull
	@Override
	public SqmTrimSpecification copy(@Nonnull SqmCopyContext context) {
		return this;
	}

	@Nonnull
	public TrimSpec getSpecification() {
		return specification;
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitTrimSpecification( this );
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return specification.name();
	}

	@Override
	public @Nullable SqmBindableType<Void> getNodeType() {
		return null;
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( specification );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmTrimSpecification that
			&& specification == that.specification;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( specification );
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
