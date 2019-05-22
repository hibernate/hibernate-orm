/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;

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
			PluralPersistentAttribute referencedNavigable,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedNavigable, lhs, null, nodeBuilder );
	}

	@SuppressWarnings("WeakerAccess")
	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralPersistentAttribute referencedNavigable,
			SqmPath lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super( navigablePath, referencedNavigable, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public PluralPersistentAttribute<?,?,E> getReferencedPathSource() {
		//noinspection unchecked
		return (PluralPersistentAttribute) super.getReferencedPathSource();
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
	@SuppressWarnings("unchecked")
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		// this is a reference to a collection outside of the from-clause...

		// what "names" should be allowed here?
		//		1) special names such as {id}, {index}, {element}, etc?
		//		2) named Navigables relative to the Collection's element type?
		//		3) named Navigables relative to the Collection's index (if one) type?
		//		?) others?
		//		d) all of the above :)
		//
		//	or probably some combination of.  For now,

		final NavigablePath navigablePath = getNavigablePath().append( name );
		return creationState.getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
				navigablePath,
				np -> {
					final PluralPersistentAttribute<?, ?, E> referencedPathSource = getReferencedPathSource();

					if ( CollectionPropertyNames.COLLECTION_ELEMENTS.equals( name ) ) {
						return referencedPathSource.getElementPathSource().createSqmPath(
								this,
								creationState
						);
					}

					if ( CollectionPropertyNames.COLLECTION_INDEX.equals( name )
							|| CollectionPropertyNames.COLLECTION_INDICES.equals( name ) ) {
						if ( referencedPathSource instanceof MapPersistentAttribute ) {
							return ( (MapPersistentAttribute) referencedPathSource ).getKeyPathSource().createSqmPath( this, creationState );
						}
						else if ( referencedPathSource instanceof ListPersistentAttribute ) {
							return ( (ListPersistentAttribute) referencedPathSource ).getIndexPathSource().createSqmPath( this, creationState );
						}

						throw new UnsupportedOperationException(  );
					}

					return referencedPathSource.getElementPathSource().createSqmPath(
							this,
							creationState
					);
				}
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends E> SqmTreatedSimplePath<E,S> treatAs(Class<S> treatJavaType) throws PathException {
		final EntityDomainType<S> treatTargetDescriptor = nodeBuilder().getDomainModel().entity( treatJavaType );
		return new SqmTreatedSimplePath(
				this,
				treatTargetDescriptor,
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
