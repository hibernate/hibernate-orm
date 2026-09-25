package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.DiscriminatorSqmPath;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


public class AnyDiscriminatorSqmPath<T> extends AbstractSqmPath<T> implements DiscriminatorSqmPath<T> {

	protected AnyDiscriminatorSqmPath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<T> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
	}

	@Nonnull
	@Override
	public AnyDiscriminatorSqmPath<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		return context.registerCopy(
				this,
				(AnyDiscriminatorSqmPath<T>) getLhs().copy( context ).type()
		);
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitAnyDiscriminatorTypeExpression( this ) ;
	}

	@Override
	public @Nonnull SqmPath<?> getLhs() {
		return castNonNull( super.getLhs() );
	}

	@Override
	public @Nonnull AnyDiscriminatorSqmPathSource<T> getExpressible() {
//		return (AnyDiscriminatorSqmPathSource<T>) getNodeType();
		return (AnyDiscriminatorSqmPathSource<T>) getReferencedPathSource();
	}
}
