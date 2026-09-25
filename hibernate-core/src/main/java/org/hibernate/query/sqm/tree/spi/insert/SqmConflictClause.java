package org.hibernate.query.sqm.tree.spi.insert;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.criteria.JpaConflictClause;
import org.hibernate.query.criteria.JpaConflictUpdateAction;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * @since 6.5
 */
public class SqmConflictClause<T> implements SqmVisitableNode, JpaConflictClause<T> {

	private final SqmInsertStatement<T> insertStatement;
	private final SqmRoot<T> excludedRoot;
	private @Nullable String constraintName;
	private @Nullable List<SqmPath<?>> constraintPaths;
	private @Nullable SqmConflictUpdateAction<T> updateAction;

	public SqmConflictClause(@Nonnull SqmInsertStatement<T> insertStatement) {
		this.insertStatement = insertStatement;
		this.excludedRoot = new SqmRoot<>(
				insertStatement.getTarget().getManagedType(),
				"excluded",
				false,
				insertStatement.nodeBuilder()
		);
	}

	private SqmConflictClause(
			@Nonnull SqmInsertStatement<T> insertStatement,
			@Nonnull SqmRoot<T> excludedRoot,
			@Nullable String constraintName,
			@Nullable List<SqmPath<?>> constraintPaths,
			@Nullable SqmConflictUpdateAction<T> updateAction) {
		this.insertStatement = insertStatement;
		this.excludedRoot = excludedRoot;
		this.constraintName = constraintName;
		this.constraintPaths = constraintPaths == null ? null : Collections.unmodifiableList( constraintPaths );
		this.updateAction = updateAction;
	}

	@Nonnull
	@Override
	public SqmRoot<T> getExcludedRoot() {
		return excludedRoot;
	}

	@Override
	public @Nullable String getConstraintName() {
		return constraintName;
	}

	@Nonnull
	@Override
	public SqmConflictClause<T> conflictOnConstraint(@Nullable String constraintName) {
		if ( constraintPaths != null && !constraintPaths.isEmpty() ) {
			throw new IllegalStateException( "Constraint paths were already set: " + constraintPaths );
		}
		this.constraintName = constraintName;
		return this;
	}

	@Nonnull
	@Override
	public JpaConflictClause<T> conflictOnConstraintAttributes(@Nonnull String... attributes) {
		final ArrayList<SqmPath<?>> paths = new ArrayList<>( attributes.length );
		for ( String attribute : attributes ) {
			paths.add( insertStatement.getTarget().get( attribute ) );
		}
		return conflictOnConstraintPaths( paths );
	}

	@Nonnull
	@Override
	public JpaConflictClause<T> conflictOnConstraintAttributes(@Nonnull SingularAttribute<T, ?>... attributes) {
		final ArrayList<SqmPath<?>> paths = new ArrayList<>( attributes.length );
		for ( SingularAttribute<T, ?> attribute : attributes ) {
			paths.add( insertStatement.getTarget().get( attribute ) );
		}
		return conflictOnConstraintPaths( paths );
	}

	@Nonnull
	@Override
	public SqmConflictClause<T> conflictOnConstraintPaths(@Nonnull Path<?>... paths) {
		return conflictOnConstraintPaths( Arrays.asList( paths ) );
	}

	@Nonnull
	@Override
	public SqmConflictClause<T> conflictOnConstraintPaths(@Nonnull List<? extends Path<?>> paths) {
		if ( constraintName != null ) {
			throw new IllegalStateException( "Constraint name was already set: " + constraintName );
		}
		//noinspection unchecked
		this.constraintPaths = (List<SqmPath<?>>) Collections.unmodifiableList( paths );
		return this;
	}

	@Nonnull
	@Override
	public List<SqmPath<?>> getConstraintPaths() {
		return constraintPaths == null
				? Collections.emptyList()
				: constraintPaths;
	}

	@Nonnull
	@Override
	public SqmConflictUpdateAction<T> createConflictUpdateAction() {
		return new SqmConflictUpdateAction<>( insertStatement );
	}

	@Override
	public @Nullable SqmConflictUpdateAction<T> getConflictAction() {
		return updateAction;
	}

	@Nonnull
	@Override
	public JpaConflictClause<T> onConflictDo(@Nullable JpaConflictUpdateAction<T> action) {
		this.updateAction = (SqmConflictUpdateAction<T>) action;
		return this;
	}

	@Nonnull
	@Override
	public SqmConflictUpdateAction<T> onConflictDoUpdate() {
		final SqmConflictUpdateAction<T> conflictUpdateAction = createConflictUpdateAction();
		onConflictDo( conflictUpdateAction );
		return conflictUpdateAction;
	}

	@Override
	public @Nonnull NodeBuilder nodeBuilder() {
		return insertStatement.nodeBuilder();
	}

	@Nonnull
	@Override
	public SqmConflictClause<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				new SqmConflictClause<>(
						insertStatement.copy( context ),
						excludedRoot.copy( context ),
						constraintName,
						constraintPaths == null ? null : copyOf( constraintPaths, context ),
						updateAction == null ? null : updateAction.copy( context )
				)
		);
	}

	@Nonnull
	private List<SqmPath<?>> copyOf(@Nonnull List<SqmPath<?>> constraintPaths, @Nonnull SqmCopyContext context) {
		if ( constraintPaths.isEmpty() ) {
			return constraintPaths;
		}
		final ArrayList<SqmPath<?>> copies = new ArrayList<>( constraintPaths.size() );
		for ( SqmPath<?> constraintPath : constraintPaths ) {
			copies.add( constraintPath.copy( context ) );
		}
		return copies;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitConflictClause( this );
	}

	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( " on conflict" );
		final List<SqmPath<?>> constraintPaths = getConstraintPaths();
		if ( constraintName != null ) {
			hql.append( " on constraint " );
			hql.append( constraintName );
		}
		else if ( !constraintPaths.isEmpty() ) {
			char separator = '(';
			for ( SqmPath<?> path : constraintPaths ) {
				hql.append( separator );
				appendUnqualifiedPath( hql, path );
				separator = ',';
			}
			hql.append( ')' );
		}
		if ( updateAction == null ) {
			hql.append( " do nothing" );
		}
		else {
			updateAction.appendHqlString( hql, context );
		}
	}

	private static void appendUnqualifiedPath(@Nonnull StringBuilder sb, @Nonnull SqmPath<?> path) {
		final SqmPath<?> lhs = path.getLhs();
		if ( lhs == null ) {
			// Skip rendering the root
			return;
		}
		appendUnqualifiedPath( sb, lhs );
		if ( lhs.getLhs() != null ) {
			sb.append( '.' );
		}
		sb.append( path.getReferencedPathSource().getPathName() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmConflictClause<?> that
			&& excludedRoot.equals( that.excludedRoot )
			&& Objects.equals( constraintName, that.constraintName )
			&& Objects.equals( constraintPaths, that.constraintPaths )
			&& Objects.equals( updateAction, that.updateAction );
	}

	@Override
	public int hashCode() {
		int result = excludedRoot.hashCode();
		result = 31 * result + Objects.hashCode( constraintName );
		result = 31 * result + Objects.hashCode( constraintPaths );
		result = 31 * result + Objects.hashCode( updateAction );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmConflictClause<?> that
			&& excludedRoot.isCompatible( that.excludedRoot )
			&& Objects.equals( constraintName, that.constraintName )
			&& SqmCacheable.areCompatible( constraintPaths, that.constraintPaths )
			&& SqmCacheable.areCompatible( updateAction, that.updateAction );
	}

	@Override
	public int cacheHashCode() {
		int result = excludedRoot.cacheHashCode();
		result = 31 * result + Objects.hashCode( constraintName );
		result = 31 * result + SqmCacheable.cacheHashCode( constraintPaths );
		result = 31 * result + SqmCacheable.cacheHashCode( updateAction );
		return result;
	}
}
