/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.NotIndexedCollectionException;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * An SqmPath for plural attribute paths
 *
 * @param <C> The collection type
 *
 * @author Steve Ebersole
 */
public class SqmPluralValuedSimplePath<C> extends AbstractSqmSimplePath<C> {
	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			SqmPluralPersistentAttribute<?, C, ?> referencedNavigable,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedNavigable, lhs, null, nodeBuilder );
	}

	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			SqmPluralPersistentAttribute<?, C, ?> referencedNavigable,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		// We need to do an unchecked cast here: PluralPersistentAttribute implements path source with
		//  the element type, but paths generated from it must be collection-typed.
		//noinspection unchecked
		super( navigablePath, (SqmPathSource<C>) referencedNavigable, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public SqmPluralValuedSimplePath<C> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmPluralValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						(SqmPluralPersistentAttribute<?,C,?>) getModel(),
						lhsCopy,
						getExplicitAlias(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	public PluralPersistentAttribute<?, C, ?> getPluralAttribute() {
		return (SqmPluralPersistentAttribute<?, C, ?>) getModel();
	}

	@Override
	public @NonNull JavaType<C> getJavaTypeDescriptor() {
		return getPluralAttribute().getAttributeJavaType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralValuedPath( this );
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		// this is a reference to a collection outside the from clause
		final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact( name );
		if ( nature == null ) {
			throw new PathException( "Plural path '" + getNavigablePath()
					+ "' refers to a collection and so element attribute '" + name
					+ "' may not be referenced directly (use element() function)" );
		}
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		final var pathRegistry = creationState.getCurrentProcessingState().getPathRegistry();
		final String alias = selector.toHqlString();
		final NavigablePath navigablePath =
				getParentNavigablePath()
						.append( getNavigablePath().getLocalName(), alias )
						.append( CollectionPart.Nature.ELEMENT.getName() );
		final SqmFrom<?, ?> indexedPath = pathRegistry.findFromByPath( navigablePath );
		if ( indexedPath != null ) {
			return indexedPath;
		}
		final SqmFrom<?, ?> path = pathRegistry.findFromByPath( castNonNull( navigablePath.getParent() ) );
		final SqmAttributeJoin<Object, ?> join;
		if ( path == null ) {
			final SqmPathSource<C> referencedPathSource = getReferencedPathSource();
			final SqmFrom<?, Object> parent = pathRegistry.resolveFrom( getLhs() );
			final SqmExpression<?> index;
			if ( referencedPathSource instanceof ListPersistentAttribute<?, ?> ) {
				join = new SqmListJoin<>(
						parent,
						(SqmListPersistentAttribute<Object, ?>) referencedPathSource,
						alias,
						SqmJoinType.INNER,
						false,
						parent.nodeBuilder()
				);
				index = ( (SqmListJoin<?, ?>) join ).index();
			}
			else if ( referencedPathSource instanceof MapPersistentAttribute<?, ?, ?> ) {
				join = new SqmMapJoin<>(
						parent,
						(SqmMapPersistentAttribute<Object, ?, ?>) referencedPathSource,
						alias,
						SqmJoinType.INNER,
						false,
						parent.nodeBuilder()
				);
				index = ( (SqmMapJoin<?, ?, ?>) join ).key();
			}
			else {
				throw new NotIndexedCollectionException( "Index operator applied to path '" + getNavigablePath()
						+ "' which is not a list or map" );
			}
			join.setJoinPredicate( creationState.getCreationContext().getNodeBuilder().equal( index, selector ) );
			parent.addSqmJoin( join );
			pathRegistry.register( join );
		}
		else {
			//noinspection unchecked
			join = (SqmAttributeJoin<Object, ?>) path;
		}
		final SqmIndexedCollectionAccessPath<Object> result = new SqmIndexedCollectionAccessPath<>(
				navigablePath,
				join,
				selector
		);
		pathRegistry.register( result );
		return result;
	}

	@Override
	public SqmExpression<Class<? extends C>> type() {
		throw new UnsupportedOperationException( "Cannot access the type of plural valued simple paths" );
	}

	@Override
	public <S extends C> SqmTreatedPath<C, S> treatAs(Class<S> treatJavaType) {
		throw new UnsupportedOperationException( "Cannot treat plural valued simple paths" );
	}

	@Override
	public <S extends C> SqmTreatedEntityValuedSimplePath<C, S> treatAs(EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException( "Cannot treat plural valued simple paths" );
	}
}
