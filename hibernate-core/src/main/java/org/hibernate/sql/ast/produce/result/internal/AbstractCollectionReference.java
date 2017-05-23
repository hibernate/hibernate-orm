/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.result.spi.CollectionReference;
import org.hibernate.sql.ast.produce.result.spi.FetchableCollectionElement;
import org.hibernate.sql.ast.produce.result.spi.FetchableCollectionIndex;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionReference implements CollectionReference {
	private final NavigableReference navigableReference;
	private final PersistentCollectionMetadata collectionMetadata;

	private final FetchableCollectionIndex index;
	private final FetchableCollectionElement element;

	private final boolean allowElementJoin;
	private final boolean allowIndexJoin;

	protected AbstractCollectionReference(NavigableReference navigableReference, boolean shouldIncludeJoins) {
		this.navigableReference = navigableReference;
		this.allowElementJoin = shouldIncludeJoins;

		this.collectionMetadata = ( (PluralPersistentAttribute) navigableReference.getNavigable() ).getPersistentCollectionMetadata();

		// Currently we can only allow a join for the collection index if all of the following are true:
		// - collection element joins are allowed;
		// - index is an EntityType;
		// - index values are not "formulas" (e.g., a @MapKey index is translated into "formula" value(s)).
		// Hibernate cannot currently support eager joining of associations within a component (@Embeddable) as an index.

		throw new NotYetImplementedException(  );
//
//		if ( shouldIncludeJoins &&
//				collectionMetadata.getIndexDescriptor() != null &&
//				collectionMetadata.getIndexDescriptor() instanceof CollectionIndexEntity ) {
//			final String[] indexFormulas =
//					( (QueryableCollection) collectionQuerySpace.getCollectionPersister() ).getIndexFormulas();
//			final int nNonNullFormulas = ArrayHelper.countNonNull( indexFormulas );
//			this.allowIndexJoin = nNonNullFormulas == 0;
//		}
//		else {
//			this.allowIndexJoin = false;
//		}
//
//		// All other fields must be initialized beforeQuery building this.index and this.element.
//		this.index = buildIndexGraph();
//		this.element = buildElementGraph();
	}

	public NavigableReference getNavigableReference() {
		return navigableReference;
	}

	//	private CollectionFetchableIndex buildIndexGraph() {
//		final CollectionPersister persister = collectionQuerySpace.getCollectionPersister();
//		if ( persister.hasIndex() ) {
//			final Type type = persister.getIndexType();
//			if ( type.isAssociationType() ) {
//				if ( type.isEntityType() ) {
//					final EntityPersister indexPersister = persister.getFactory().getEntityPersister(
//							( (EntityType) type ).getAssociatedEntityName()
//					);
//
//					final ExpandingEntityQuerySpace entityQuerySpace = QuerySpaceHelper.INSTANCE.makeEntityQuerySpace(
//							collectionQuerySpace,
//							indexPersister,
//							CollectionPropertyNames.COLLECTION_INDICES,
//							(EntityType) persister.getIndexType(),
//							collectionQuerySpace.getExpandingQuerySpaces().generateImplicitUid(),
//							collectionQuerySpace.canJoinsBeRequired(),
//							allowIndexJoin
//					);
//					return new CollectionFetchableIndexEntityGraph( this, entityQuerySpace );
//				}
//				else if ( type.isAnyType() ) {
//					return new CollectionFetchableIndexAnyGraph( this );
//				}
//			}
//			else if ( type.isComponentType() ) {
//				final ExpandingCompositeQuerySpace compositeQuerySpace = QuerySpaceHelper.INSTANCE.makeCompositeQuerySpace(
//						collectionQuerySpace,
//						new CompositePropertyMapping(
//								(CompositeType) persister.getIndexType(),
//								(PropertyMapping) persister,
//								""
//						),
//						CollectionPropertyNames.COLLECTION_INDICES,
//						(CompositeType) persister.getIndexType(),
//						collectionQuerySpace.getExpandingQuerySpaces().generateImplicitUid(),
//						collectionQuerySpace.canJoinsBeRequired(),
//						allowIndexJoin
//				);
//				return new CollectionFetchableIndexCompositeGraph( this, compositeQuerySpace );
//			}
//		}
//
//		return null;
//	}
//
//	private CollectionFetchableElement buildElementGraph() {
//		final CollectionPersister persister = collectionQuerySpace.getCollectionPersister();
//		final Type type = persister.getElementType();
//		if ( type.isAssociationType() ) {
//			if ( type.isEntityType() ) {
//				final EntityPersister elementPersister = persister.getFactory().getEntityPersister(
//						( (EntityType) type ).getAssociatedEntityName()
//				);
//				final ExpandingEntityQuerySpace entityQuerySpace = QuerySpaceHelper.INSTANCE.makeEntityQuerySpace(
//						collectionQuerySpace,
//						elementPersister,
//						CollectionPropertyNames.COLLECTION_ELEMENTS,
//						(EntityType) persister.getElementType(),
//						collectionQuerySpace.getExpandingQuerySpaces().generateImplicitUid(),
//						collectionQuerySpace.canJoinsBeRequired(),
//						allowElementJoin
//				);
//				return new CollectionFetchableElementEntityGraph( this, entityQuerySpace );
//			}
//			else if ( type.isAnyType() ) {
//				return new CollectionFetchableElementAnyGraph( this );
//			}
//		}
//		else if ( type.isComponentType() ) {
//			final ExpandingCompositeQuerySpace compositeQuerySpace = QuerySpaceHelper.INSTANCE.makeCompositeQuerySpace(
//					collectionQuerySpace,
//					new CompositePropertyMapping(
//							(CompositeType) persister.getElementType(),
//							(PropertyMapping) persister,
//							""
//					),
//					CollectionPropertyNames.COLLECTION_ELEMENTS,
//					(CompositeType) persister.getElementType(),
//					collectionQuerySpace.getExpandingQuerySpaces().generateImplicitUid(),
//					collectionQuerySpace.canJoinsBeRequired(),
//					allowElementJoin
//			);
//			return new CollectionFetchableElementCompositeGraph( this, compositeQuerySpace );
//		}
//
//		return null;
//	}

	@Override
	public boolean allowElementJoin() {
		return allowElementJoin;
	}

	@Override
	public boolean allowIndexJoin() {
		return allowIndexJoin;
	}


	@Override
	public PersistentCollectionMetadata getCollectionMetadata() {
		return collectionMetadata;
	}

	@Override
	public FetchableCollectionIndex getIndexGraph() {
		return index;
	}

	@Override
	public FetchableCollectionElement getElementGraph() {
		return element;
	}
}
