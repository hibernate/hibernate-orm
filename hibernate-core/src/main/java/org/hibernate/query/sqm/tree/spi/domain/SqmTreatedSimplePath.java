package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSimplePath<T, S extends T>
		extends SqmEntityValuedSimplePath<S>
		implements SqmSimplePath<S>, SqmTreatedPath<T,S> {

	private final SqmEntityDomainType<S> treatTarget;
	private final SqmPath<T> wrappedPath;

	public SqmTreatedSimplePath(
			@Nonnull SqmPluralValuedSimplePath<T> wrappedPath,
			@Nonnull SqmEntityDomainType<S> treatTarget,
			@Nonnull NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName()
				),
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				wrappedPath.getLhs(),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	public SqmTreatedSimplePath(
			@Nonnull SqmPath<T> wrappedPath,
			@Nonnull SqmEntityDomainType<S> treatTarget,
			@Nonnull NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName()
				),
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				castNonNull( wrappedPath.getLhs() ),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedSimplePath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPath<T> wrappedPath,
			@Nonnull SqmEntityDomainType<S> treatTarget,
			@Nonnull NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				navigablePath,
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				castNonNull( wrappedPath.getLhs() ),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Nonnull
	@Override
	public SqmTreatedSimplePath<T, S> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final var path = context.registerCopy(
				this,
				new SqmTreatedSimplePath<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						getTreatTarget(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public SqmEntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public @Nonnull SqmBindableType<S> getNodeType() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public SqmPathSource<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public SqmPathSource<S> getResolvedModel() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public <S1 extends S> SqmTreatedEntityValuedSimplePath<S,S1> treatAs(@Nonnull Class<S1> treatJavaType) {
		return super.treatAs( treatJavaType );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public SqmPath<?> get(@Nonnull String attributeName) {
		return resolvePath( attributeName, treatTarget.getSubPathSource( attributeName ) );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		// Cast needed for static nullness analysis.
		return walker.visitTreatedPath( (SqmTreatedPath<?, ?>) this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getName() );
		hql.append( ')' );
	}
}
