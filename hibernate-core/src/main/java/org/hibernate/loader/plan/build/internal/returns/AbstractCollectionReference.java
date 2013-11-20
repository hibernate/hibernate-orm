/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.build.internal.returns;

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

	protected AbstractCollectionReference(
			ExpandingCollectionQuerySpace collectionQuerySpace,
			PropertyPath propertyPath,
			boolean shouldIncludeJoins) {
		this.collectionQuerySpace = collectionQuerySpace;
		this.propertyPath = propertyPath;

		this.index = buildIndexGraph( collectionQuerySpace, shouldIncludeJoins );
		this.element = buildElementGraph( collectionQuerySpace, shouldIncludeJoins );
	}

	private CollectionFetchableIndex buildIndexGraph(
			ExpandingCollectionQuerySpace collectionQuerySpace,
			boolean shouldIncludeJoins) {
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
							shouldIncludeJoins
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
						shouldIncludeJoins
				);
				return new CollectionFetchableIndexCompositeGraph( this, compositeQuerySpace );
			}
		}

		return null;
	}

	private CollectionFetchableElement buildElementGraph(
			ExpandingCollectionQuerySpace collectionQuerySpace,
			boolean shouldIncludeJoins) {
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
						shouldIncludeJoins
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
					shouldIncludeJoins
			);
			return new CollectionFetchableElementCompositeGraph( this, compositeQuerySpace );
		}

		return null;
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
