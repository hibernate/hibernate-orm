/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.NotIndexedCollectionException;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;

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
			PluralPersistentAttribute<?, C, ?> referencedNavigable,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedNavigable, lhs, null, nodeBuilder );
	}

	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralPersistentAttribute<?, C, ?> referencedNavigable,
			SqmPath<?> lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		// We need to do an unchecked cast here: PluralPersistentAttribute implements path source with
		//  the element type, but paths generated from it must be collection-typed.
		//noinspection unchecked
		super( navigablePath, (SqmPathSource<C>) referencedNavigable, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public SqmPluralValuedSimplePath<C> copy(SqmCopyContext context) {
		final SqmPluralValuedSimplePath<C> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final SqmPluralValuedSimplePath<C> path = context.registerCopy(
				this,
				new SqmPluralValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						(PluralPersistentAttribute<?,C,?>) getModel(),
						lhsCopy,
						getExplicitAlias(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	public PluralPersistentAttribute<?, C, ?> getPluralAttribute() {
		return (PluralPersistentAttribute<?, C, ?>) getModel();
	}

	@Override
	public JavaType<C> getJavaTypeDescriptor() {
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
		final SqmPath<?> sqmPath = get( name );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPathRegistry pathRegistry = creationState.getCurrentProcessingState().getPathRegistry();
		final String alias = selector.toHqlString();
		final NavigablePath navigablePath = getNavigablePath().getParent().append(
				getNavigablePath().getLocalName(),
				alias
		).append( CollectionPart.Nature.ELEMENT.getName() );
		final SqmFrom<?, ?> indexedPath = pathRegistry.findFromByPath( navigablePath );
		if ( indexedPath != null ) {
			return indexedPath;
		}
		SqmFrom<?, ?> path = pathRegistry.findFromByPath( navigablePath.getParent() );
		if ( path == null ) {
			final SqmPathSource<C> referencedPathSource = getReferencedPathSource();
			final SqmFrom<?, Object> parent = pathRegistry.resolveFrom( getLhs() );
			final SqmQualifiedJoin<Object, ?> join;
			final SqmExpression<?> index;
			if ( referencedPathSource instanceof ListPersistentAttribute<?, ?> ) {
				join = new SqmListJoin<>(
						parent,
						(ListPersistentAttribute<Object, ?>) referencedPathSource,
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
						(MapPersistentAttribute<Object, ?, ?>) referencedPathSource,
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
			pathRegistry.register( path = join );
		}
		final SqmIndexedCollectionAccessPath<Object> result = new SqmIndexedCollectionAccessPath<>(
				navigablePath,
				path,
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
	public <S extends C> SqmTreatedPath<C, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Cannot treat plural valued simple paths" );
	}

	@Override
	public <S extends C> SqmTreatedEntityValuedSimplePath<C, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new UnsupportedOperationException( "Cannot treat plural valued simple paths" );
	}
}
