/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.insert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaConflictClause;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import jakarta.persistence.criteria.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertAssignable;

/**
 * Convenience base class for InsertSqmStatement implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmInsertStatement<T> extends AbstractSqmDmlStatement<T> implements SqmInsertStatement<T> {
	private List<SqmPath<?>> insertionTargetPaths;
	private @Nullable SqmConflictClause<T> conflictClause;

	protected AbstractSqmInsertStatement(SqmRoot<T> targetRoot, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( targetRoot, querySource, nodeBuilder );
	}

	protected AbstractSqmInsertStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<T> target,
			List<SqmPath<?>> insertionTargetPaths,
			SqmConflictClause<T> conflictClause) {
		super( builder, querySource, parameters, cteStatements, target );
		this.insertionTargetPaths = insertionTargetPaths;
		this.conflictClause = conflictClause;
	}

	protected List<SqmPath<?>> copyInsertionTargetPaths(SqmCopyContext context) {
		if ( insertionTargetPaths == null ) {
			return null;
		}
		else {
			final List<SqmPath<?>> insertionTargetPaths = new ArrayList<>( this.insertionTargetPaths.size() );
			for ( SqmPath<?> insertionTargetPath : this.insertionTargetPaths ) {
				insertionTargetPaths.add( insertionTargetPath.copy( context ) );
			}
			return insertionTargetPaths;
		}
	}

	void setConflictClause(SqmConflictClause<T> conflictClause) {
		this.conflictClause = conflictClause;
	}

	protected void verifyInsertTypesMatch(
			List<SqmPath<?>> insertionTargetPaths,
			List<? extends SqmTypedNode<?>> expressions) {
		final int size = insertionTargetPaths.size();
		final int expressionsSize = expressions.size();
		if ( size != expressionsSize ) {
			throw new SemanticException(
					String.format(
							"Expected insert attribute count [%d] did not match Query selection count [%d]",
							size,
							expressionsSize
					),
					null,
					null
			);
		}

		for ( int i = 0; i < expressionsSize; i++ ) {
			final SqmTypedNode<?> expression = expressions.get( i );
			final SqmPath<?> targetPath = insertionTargetPaths.get(i);
			assertAssignable( null, targetPath, expression, nodeBuilder() );
//			if ( expression.getNodeJavaType() == null ) {
//				continue;
//			}
//			if ( insertionTargetPaths.get( i ).getJavaTypeDescriptor() != expression.getNodeJavaType() ) {
//				throw new SemanticException(
//						String.format(
//								"Expected insert attribute type [%s] did not match Query selection type [%s] at selection index [%d]",
//								insertionTargetPaths.get( i ).getJavaTypeDescriptor().getTypeName(),
//								expression.getNodeJavaType().getTypeName(),
//								i
//						),
//						hqlString,
//						null
//				);
//			}
		}
	}

	@Override
	public void setTarget(JpaRoot<T> root) {
		if ( root.getModel() instanceof SqmPolymorphicRootDescriptor<?> ) {
			throw new SemanticException(
					String.format(
							"Target type '%s' is not an entity",
							root.getModel().getHibernateEntityName()
					)
			);
		}
		super.setTarget( root );
	}

	@Override
	public List<SqmPath<?>> getInsertionTargetPaths() {
		return insertionTargetPaths == null
				? Collections.emptyList()
				: Collections.unmodifiableList( insertionTargetPaths );
	}

	@Override
	public SqmInsertStatement<T> setInsertionTargetPaths(Path<?>... insertionTargetPaths) {
		return setInsertionTargetPaths( Arrays.asList( insertionTargetPaths ) );
	}

	@Override
	public SqmInsertStatement<T> setInsertionTargetPaths(List<? extends Path<?>> insertionTargetPaths) {
		//noinspection unchecked
		this.insertionTargetPaths = (List<SqmPath<?>>) insertionTargetPaths;
		return this;
	}

	public void addInsertTargetStateField(SqmPath<?> stateField) {
		if ( insertionTargetPaths == null ) {
			insertionTargetPaths = new ArrayList<>();
		}
		insertionTargetPaths.add( stateField );
	}

	@Override
	public void visitInsertionTargetPaths(Consumer<SqmPath<?>> consumer) {
		if ( insertionTargetPaths != null ) {
			insertionTargetPaths.forEach( consumer );
		}
	}

	@Override
	public SqmConflictClause<T> createConflictClause() {
		return new SqmConflictClause<>( this );
	}

	@Override
	public @Nullable SqmConflictClause<T> getConflictClause() {
		return conflictClause;
	}

	@Override
	public JpaConflictClause<T> onConflict() {
		return this.conflictClause = createConflictClause();
	}

	@Override
	public JpaCriteriaInsert<T> onConflict(@Nullable JpaConflictClause<T> conflictClause) {
		this.conflictClause = (SqmConflictClause<T>) conflictClause;
		return this;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		appendHqlCteString( hql, context );
		hql.append( "insert into " );
		hql.append( getTarget().getEntityName() );
		if ( insertionTargetPaths != null && !insertionTargetPaths.isEmpty() ) {
			hql.append( '(' );
			insertionTargetPaths.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < insertionTargetPaths.size(); i++ ) {
				hql.append( ", " );
				insertionTargetPaths.get( i ).appendHqlString( hql, context );
			}
			hql.append( ')' );
		}
	}
}
