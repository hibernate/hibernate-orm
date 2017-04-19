/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.internal.spaces.CompositePropertyMapping;
import org.hibernate.loader.plan.build.internal.spaces.QuerySpaceHelper;
import org.hibernate.loader.plan.build.spi.ExpandingCollectionQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.spi.CollectionFetchableElement;
import org.hibernate.loader.plan.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionReference implements CollectionReference {
	private final ExpandingCollectionQuerySpace collectionQuerySpace;
	private final PropertyPath propertyPath;

	private final CollectionFetchableIndex index;
	private final CollectionFetchableElement element;

	private final boolean allowElementJoin;
	private final boolean allowIndexJoin;

	protected AbstractCollectionReference(
			ExpandingCollectionQuerySpace collectionQuerySpace,
			PropertyPath propertyPath,
			boolean shouldIncludeJoins) {
		this.collectionQuerySpace = collectionQuerySpace;
		this.propertyPath = propertyPath;

		this.allowElementJoin = shouldIncludeJoins;

		// Currently we can only allow a join for the collection index if all of the following are true:
		// - collection element joins are allowed;
		// - index is an EntityType;
		// - index values are not "formulas" (e.g., a @MapKey index is translated into "formula" value(s)).
		// Hibernate cannot currently support eager joining of associations within a component (@Embeddable) as an index.
		if ( shouldIncludeJoins &&
				collectionQuerySpace.getCollectionPersister().hasIndex() &&
				collectionQuerySpace.getCollectionPersister().getIndexType().isEntityType()  ) {
			final String[] indexFormulas =
					( (QueryableCollection) collectionQuerySpace.getCollectionPersister() ).getIndexFormulas();
			final int nNonNullFormulas = ArrayHelper.countNonNull( indexFormulas );
			this.allowIndexJoin = nNonNullFormulas == 0;
		}
		else {
			this.allowIndexJoin = false;
		}

		// All other fields must be initialized before building this.index and this.element.
		this.index = buildIndexGraph();
		this.element = buildElementGraph();
	}

	private CollectionFetchableIndex buildIndexGraph() {
		final CollectionPersister persister = collectionQuerySpace.getCollectionPersister();
		if ( persister.hasIndex() ) {
			final Type type = persister.getIndexType();
			if ( type.isAssociationType() ) {
				if ( type.isEntityType() ) {
					final EntityPersister indexPersister = persister.getFactory().getEntityPersister(
							( (EntityType) type ).getAssociatedEntityName()
					);

					final ExpandingEntityQuerySpace entityQuerySpace = QuerySpaceHelper.INSTANCE.makeEntityQuerySpace(
							collectionQuerySpace,
							indexPersister,
							CollectionPropertyNames.COLLECTION_INDICES,
							(EntityType) persister.getIndexType(),
							collectionQuerySpace.getExpandingQuerySpaces().generateImplicitUid(),
							collectionQuerySpace.canJoinsBeRequired(),
							allowIndexJoin
					);
					return new CollectionFetchableIndexEntityGraph( this, entityQuerySpace );
				}
				else if ( type.isAnyType() ) {
					return new CollectionFetchableIndexAnyGraph( this );
				}
			}
			else if ( type.isComponentType() ) {
				final ExpandingCompositeQuerySpace compositeQuerySpace = QuerySpaceHelper.INSTANCE.makeCompositeQuerySpace(
						collectionQuerySpace,
						new CompositePropertyMapping(
								(CompositeType) persister.getIndexType(),
								(PropertyMapping) persister,
								""
						),
						CollectionPropertyNames.COLLECTION_INDICES,
						(CompositeType) persister.getIndexType(),
						collectionQuerySpace.getExpandingQuerySpaces().generateImplicitUid(),
						collectionQuerySpace.canJoinsBeRequired(),
						allowIndexJoin
				);
				return new CollectionFetchableIndexCompositeGraph( this, compositeQuerySpace );
			}
		}

		return null;
	}

	private CollectionFetchableElement buildElementGraph() {
		final CollectionPersister persister = collectionQuerySpace.getCollectionPersister();
		final Type type = persister.getElementType();
		if ( type.isAssociationType() ) {
			if ( type.isEntityType() ) {
				final EntityPersister elementPersister = persister.getFactory().getEntityPersister(
						( (EntityType) type ).getAssociatedEntityName()
				);
				final ExpandingEntityQuerySpace entityQuerySpace = QuerySpaceHelper.INSTANCE.makeEntityQuerySpace(
						collectionQuerySpace,
						elementPersister,
						CollectionPropertyNames.COLLECTION_ELEMENTS,
						(EntityType) persister.getElementType(),
						collectionQuerySpace.getExpandingQuerySpaces().generateImplicitUid(),
						collectionQuerySpace.canJoinsBeRequired(),
						allowElementJoin
				);
				return new CollectionFetchableElementEntityGraph( this, entityQuerySpace );
			}
			else if ( type.isAnyType() ) {
				return new CollectionFetchableElementAnyGraph( this );
			}
		}
		else if ( type.isComponentType() ) {
			final ExpandingCompositeQuerySpace compositeQuerySpace = QuerySpaceHelper.INSTANCE.makeCompositeQuerySpace(
					collectionQuerySpace,
					new CompositePropertyMapping(
							(CompositeType) persister.getElementType(),
							(PropertyMapping) persister,
							""
					),
					CollectionPropertyNames.COLLECTION_ELEMENTS,
					(CompositeType) persister.getElementType(),
					collectionQuerySpace.getExpandingQuerySpaces().generateImplicitUid(),
					collectionQuerySpace.canJoinsBeRequired(),
					allowElementJoin
			);
			return new CollectionFetchableElementCompositeGraph( this, compositeQuerySpace );
		}

		return null;
	}

	@Override
	public boolean allowElementJoin() {
		return allowElementJoin;
	}

	@Override
	public boolean allowIndexJoin() {
		return allowIndexJoin;
	}

	@Override
	public String getQuerySpaceUid() {
		return collectionQuerySpace.getUid();
	}

	@Override
	public CollectionPersister getCollectionPersister() {
		return collectionQuerySpace.getCollectionPersister();
	}

	@Override
	public CollectionFetchableIndex getIndexGraph() {
		return index;
	}

	@Override
	public CollectionFetchableElement getElementGraph() {
		return element;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}
}
