package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmJoin;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedDerivedRootJoin<T> extends SqmCorrelatedRootJoin<T> {

	public SqmCorrelatedDerivedRootJoin(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<T> referencedNavigable,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, nodeBuilder );
	}

	@Nonnull
	@Override
	public SqmCorrelatedDerivedRootJoin<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCorrelatedDerivedRootJoin<>(
						getNavigablePath(),
						getReferencedPathSource(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	// Need to suppress argument warnings because correlatedJoin which is under initialization is passed to addSqmJoin,
	// which expects an initialized argument. We know this is safe though because we only store the instance
	@Nonnull
	@SuppressWarnings({"unchecked", "argument"})
	public static <X, J extends SqmJoin<X, ?>> SqmCorrelatedDerivedRootJoin<X> create(@Nonnull J correlationParent, @Nonnull J correlatedJoin) {
		final SqmFrom<?, X> parentPath = (SqmFrom<?, X>) correlationParent.getParentPath();
		final SqmCorrelatedDerivedRootJoin<X> rootJoin;
		if ( parentPath == null ) {
			rootJoin = new SqmCorrelatedDerivedRootJoin<>(
					correlationParent.getNavigablePath(),
					(SqmPathSource<X>) correlationParent.getReferencedPathSource(),
					correlationParent.nodeBuilder()
			);
		}
		else {
			rootJoin = new SqmCorrelatedDerivedRootJoin<>(
					parentPath.getNavigablePath(),
					parentPath.getReferencedPathSource(),
					correlationParent.nodeBuilder()
			);
		}
		rootJoin.addSqmJoin( correlatedJoin );
		return rootJoin;
	}

	@Override
	public boolean containsOnlyInnerJoins() {
		// The derived join is just referenced, no need to create any table groups
		return true;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nonnull
	@Override
	public SqmEntityDomainType<T> getModel() {
		throw new UnsupportedOperationException( "Correlated derived root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Nonnull
	@Override
	public String getEntityName() {
		throw new UnsupportedOperationException( "Correlated derived root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Nonnull
	@Override
	public SqmPathSource<T> getResolvedModel() {
		return getReferencedPathSource();
	}
}
