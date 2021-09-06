/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;

/**
 * An SqmPath for plural attribute paths
 *
 * @param <E> The collection element type, which is the "bindable" type in the SQM tree
 *
 * @author Steve Ebersole
 */
public class SqmPluralValuedSimplePath<E> extends AbstractSqmSimplePath<E> {
	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralPersistentAttribute<?, ?, E> referencedNavigable,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedNavigable, lhs, null, nodeBuilder );
	}

	@SuppressWarnings("WeakerAccess")
	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralPersistentAttribute<?, ?, E> referencedNavigable,
			SqmPath<?> lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public PluralPersistentAttribute<?, ?, E> getReferencedPathSource() {
		return (PluralPersistentAttribute<?, ?, E>) super.getReferencedPathSource();
	}

	@Override
	public PluralPersistentAttribute<?,?,E> getNodeType() {
		return getReferencedPathSource();
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
		// this is a reference to a collection outside of the from-clause...
		final NavigablePath navigablePath = getNavigablePath().append( name );
		return creationState.getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
				navigablePath,
				np -> get( np.getUnaliasedLocalName() )
		);
	}

	@Override
	public <S extends E> SqmTreatedSimplePath<E,S> treatAs(Class<S> treatJavaType) throws PathException {
		return (SqmTreatedSimplePath<E, S>) treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends E> SqmTreatedPath<E, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		return new SqmTreatedSimplePath<>(
				this,
				treatTarget,
				nodeBuilder()
		);
	}

//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState,
//			DomainResultCreationContext creationContext) {
//		return new CollectionResultImpl(
//				getReferencedNavigable().getPluralAttribute().getDescribedAttribute(),
//				getNavigablePath(),
//				resultVariable,
//				LockMode.NONE,
//				getReferencedNavigable().getPluralAttribute().getCollectionKeyDescriptor().createDomainResult(
//						getNavigablePath().append( "{id}" ),
//						null,
//						creationState,
//						creationContext
//				),
//				initializerProducerCreator.createProducer( resultVariable, creationState, creationContext )
//		);
//	}

}
