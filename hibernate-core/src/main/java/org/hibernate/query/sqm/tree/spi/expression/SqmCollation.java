/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Christian Beikov
 */
public class SqmCollation extends SqmLiteral<String> {
	public SqmCollation(@Nonnull String value, @Nullable SqmBindableType<String> inherentType, @Nonnull NodeBuilder nodeBuilder) {
		super(value, inherentType == null ? nodeBuilder.getStringType() : inherentType, nodeBuilder);
	}

	@Nonnull
	@Override
	public SqmCollation copy(@Nonnull SqmCopyContext context) {
		final SqmCollation existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCollation expression = context.registerCopy(
				this,
				new SqmCollation( getLiteralValue(), getNodeType(), nodeBuilder() )
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public @Nonnull String getLiteralValue() {
		return castNonNull( super.getLiteralValue() );
	}

	@Override
	public @Nonnull SqmBindableType<String> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Nullable
	@Override
	public <R> R accept(@Nonnull SemanticQueryWalker<R> walker) {
		return walker.visitCollation( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( getLiteralValue() );
	}
}
