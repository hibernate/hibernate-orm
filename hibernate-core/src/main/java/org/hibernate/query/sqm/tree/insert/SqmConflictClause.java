/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.insert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.criteria.JpaConflictClause;
import org.hibernate.query.criteria.JpaConflictUpdateAction;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.SingularAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @since 6.5
 */
public class SqmConflictClause<T> implements SqmVisitableNode, JpaConflictClause<T> {

	private final SqmInsertStatement<T> insertStatement;
	private final SqmRoot<T> excludedRoot;
	private @Nullable String constraintName;
	private List<SqmPath<?>> constraintPaths;
	private @Nullable SqmConflictUpdateAction<T> updateAction;

	public SqmConflictClause(SqmInsertStatement<T> insertStatement) {
		this.insertStatement = insertStatement;
		this.excludedRoot = new SqmRoot<>(
				insertStatement.getTarget().getManagedType(),
				"excluded",
				false,
				insertStatement.nodeBuilder()
		);
	}

	private SqmConflictClause(
			SqmInsertStatement<T> insertStatement,
			SqmRoot<T> excludedRoot,
			@Nullable String constraintName,
			List<SqmPath<?>> constraintPaths,
			@Nullable SqmConflictUpdateAction<T> updateAction) {
		this.insertStatement = insertStatement;
		this.excludedRoot = excludedRoot;
		this.constraintName = constraintName;
		this.constraintPaths = constraintPaths == null ? null : Collections.unmodifiableList( constraintPaths );
		this.updateAction = updateAction;
	}

	@Override
	public SqmRoot<T> getExcludedRoot() {
		return excludedRoot;
	}

	@Override
	public @Nullable String getConstraintName() {
		return constraintName;
	}

	@Override
	public SqmConflictClause<T> conflictOnConstraint(@Nullable String constraintName) {
		if ( constraintPaths != null && !constraintPaths.isEmpty() ) {
			throw new IllegalStateException( "Constraint paths were already set: " + constraintPaths );
		}
		this.constraintName = constraintName;
		return this;
	}

	@Override
	public JpaConflictClause<T> conflictOnConstraintAttributes(String... attributes) {
		final ArrayList<SqmPath<?>> paths = new ArrayList<>( attributes.length );
		for ( String attribute : attributes ) {
			paths.add( insertStatement.getTarget().get( attribute ) );
		}
		return conflictOnConstraintPaths( paths );
	}

	@Override
	public JpaConflictClause<T> conflictOnConstraintAttributes(SingularAttribute<T, ?>... attributes) {
		final ArrayList<SqmPath<?>> paths = new ArrayList<>( attributes.length );
		for ( SingularAttribute<T, ?> attribute : attributes ) {
			paths.add( insertStatement.getTarget().get( attribute ) );
		}
		return conflictOnConstraintPaths( paths );
	}

	@Override
	public SqmConflictClause<T> conflictOnConstraintPaths(Path<?>... paths) {
		return conflictOnConstraintPaths( Arrays.asList( paths ) );
	}

	@Override
	public SqmConflictClause<T> conflictOnConstraintPaths(List<? extends Path<?>> paths) {
		if ( constraintName != null ) {
			throw new IllegalStateException( "Constraint name was already set: " + constraintName );
		}
		//noinspection unchecked
		this.constraintPaths = (List<SqmPath<?>>) Collections.unmodifiableList( paths );
		return this;
	}

	@Override
	public List<SqmPath<?>> getConstraintPaths() {
		return constraintPaths == null
				? Collections.emptyList()
				: constraintPaths;
	}

	@Override
	public SqmConflictUpdateAction<T> createConflictUpdateAction() {
		return new SqmConflictUpdateAction<>( insertStatement );
	}

	@Override
	public @Nullable SqmConflictUpdateAction<T> getConflictAction() {
		return updateAction;
	}

	@Override
	public JpaConflictClause<T> onConflictDo(JpaConflictUpdateAction<T> action) {
		this.updateAction = (SqmConflictUpdateAction<T>) action;
		return this;
	}

	@Override
	public SqmConflictUpdateAction<T> onConflictDoUpdate() {
		final SqmConflictUpdateAction<T> conflictUpdateAction = createConflictUpdateAction();
		onConflictDo( conflictUpdateAction );
		return conflictUpdateAction;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return insertStatement.nodeBuilder();
	}

	@Override
	public SqmConflictClause<T> copy(SqmCopyContext context) {
		final SqmConflictClause<T> existing = context.getCopy( this );
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

	private List<SqmPath<?>> copyOf(List<SqmPath<?>> constraintPaths, SqmCopyContext context) {
		if ( constraintPaths.isEmpty() ) {
			return constraintPaths;
		}
		final ArrayList<SqmPath<?>> copies = new ArrayList<>( constraintPaths.size() );
		for ( SqmPath<?> constraintPath : constraintPaths ) {
			copies.add( constraintPath.copy( context ) );
		}
		return copies;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitConflictClause( this );
	}

	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( " on conflict" );
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

	private static void appendUnqualifiedPath(StringBuilder sb, SqmPath<?> path) {
		if ( path.getLhs() == null ) {
			// Skip rendering the root
			return;
		}
		appendUnqualifiedPath( sb, path.getLhs() );
		if ( path.getLhs().getLhs() != null ) {
			sb.append( '.' );
		}
		sb.append( path.getReferencedPathSource().getPathName() );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmConflictClause<?> that
			&& Objects.equals( excludedRoot, that.excludedRoot )
			&& Objects.equals( constraintName, that.constraintName )
			&& Objects.equals( constraintPaths, that.constraintPaths )
			&& Objects.equals( updateAction, that.updateAction );
	}

	@Override
	public int hashCode() {
		return Objects.hash( excludedRoot, constraintName, constraintPaths, updateAction );
	}
}
