package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;


/**
 * @author Christian Beikov
 */
@Incubating(since = "6.2")
public class SqmDerivedRoot<T> extends SqmRoot<T> implements JpaDerivedRoot<T> {

	private final SqmSubQuery<T> subQuery;

	public SqmDerivedRoot(
			@Nonnull SqmSubQuery<T> subQuery,
			@Nullable String alias) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
				subQuery,
				new AnonymousTupleType<>( subQuery ),
				alias
		);
	}

	protected SqmDerivedRoot(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmSubQuery<T> subQuery,
			@Nonnull SqmPathSource<T> pathSource,
			@Nullable String alias) {
		super(
				navigablePath,
				pathSource,
				alias,
				true,
				subQuery.nodeBuilder()
		);
		this.subQuery = subQuery;
	}

	@Nonnull
	@Override
	public SqmDerivedRoot<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmDerivedRoot<>(
						getNavigablePath(),
						getQueryPart().copy( context ),
						getReferencedPathSource(),
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public SqmSubQuery<T> getQueryPart() {
		return subQuery;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitRootDerived( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nonnull
	@Override
	public SqmEntityDomainType<T> getModel() {
		// Or should we throw an exception instead?
		throw new UnsupportedOperationException( "Derived root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Nonnull
	@Override
	public String getEntityName() {
		throw new UnsupportedOperationException( "Derived root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Nonnull
	@Override
	public SqmPathSource<T> getResolvedModel() {
		return getReferencedPathSource();
	}

	@Override
	@Nonnull
	public SqmCorrelatedRoot<T> createCorrelation() {
		return new SqmCorrelatedDerivedRoot<>( this );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedFrom<T, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Derived roots can not be treated" );
	}

	@Override
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& subQuery.equals( ((SqmDerivedRoot<?>) object).subQuery );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& subQuery.isCompatible( ((SqmDerivedRoot<?>) object).subQuery );
	}
}
