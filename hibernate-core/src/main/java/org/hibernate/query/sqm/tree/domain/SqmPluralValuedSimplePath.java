/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.PluralValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmPluralValuedSimplePath extends AbstractSqmSimplePath {
	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralValuedNavigable referencedNavigable,
			SqmPath lhs) {
		this( navigablePath, referencedNavigable, lhs, null );
	}

	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralValuedNavigable referencedNavigable,
			SqmPath lhs,
			String explicitAlias) {
		super( navigablePath, referencedNavigable, lhs, explicitAlias );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getCollectionDescriptor()
				.getElementDescriptor()
				.getClassification()
				.getCorrespondingJpaPersistenceType();
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
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
					if ( CollectionElement.NAVIGABLE_NAME.equals( name ) ) {
						return getReferencedNavigable().getCollectionDescriptor().getElementDescriptor().createSqmExpression(
								this,
								creationState
						);
					}

					if ( CollectionIndex.NAVIGABLE_NAME.equals( name ) ) {
						return getReferencedNavigable().getCollectionDescriptor().getIndexDescriptor().createSqmExpression(
								this,
								creationState
						);
					}

					return getReferencedNavigable().getCollectionDescriptor().getElementDescriptor().createSqmExpression(
							this,
							creationState
					);
				}
		);
	}

//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState,
//			DomainResultCreationContext creationContext) {
//		return new CollectionResultImpl(
//				getReferencedNavigable().getCollectionDescriptor().getDescribedAttribute(),
//				getNavigablePath(),
//				resultVariable,
//				LockMode.NONE,
//				getReferencedNavigable().getCollectionDescriptor().getCollectionKeyDescriptor().createDomainResult(
//						getNavigablePath().append( "{id}" ),
//						null,
//						creationState,
//						creationContext
//				),
//				initializerProducerCreator.createProducer( resultVariable, creationState, creationContext )
//		);
//	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralValuedPath( this );
	}

	@Override
	public PluralValuedNavigable getReferencedNavigable() {
		return (PluralValuedNavigable) super.getReferencedNavigable();
	}

	@Override
	public PluralValuedNavigable getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public Supplier<? extends PluralValuedNavigable> getInferableType() {
		return this::getReferencedNavigable;
	}

	@Override
	public CollectionJavaDescriptor getJavaTypeDescriptor() {
		return (CollectionJavaDescriptor) super.getJavaTypeDescriptor();
	}
}
