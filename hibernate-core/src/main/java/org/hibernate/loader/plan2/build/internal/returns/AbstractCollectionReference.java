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
package org.hibernate.loader.plan2.build.internal.returns;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingCollectionQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan2.spi.CollectionFetchableElement;
import org.hibernate.loader.plan2.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan2.spi.CollectionReference;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
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
			PropertyPath propertyPath) {
		this.collectionQuerySpace = collectionQuerySpace;
		this.propertyPath = propertyPath;

		this.index = buildIndexGraph( collectionQuerySpace );
		this.element = buildElementGraph( collectionQuerySpace );
	}

	private CollectionFetchableIndex buildIndexGraph(ExpandingCollectionQuerySpace collectionQuerySpace) {
		final CollectionPersister persister = collectionQuerySpace.getCollectionPersister();
		if ( persister.hasIndex() ) {
			final Type type = persister.getIndexType();
			if ( type.isAssociationType() ) {
				if ( type.isEntityType() ) {
					final EntityPersister indexPersister = persister.getFactory().getEntityPersister(
							( (EntityType) type ).getAssociatedEntityName()
					);

					final ExpandingEntityQuerySpace entityQuerySpace = collectionQuerySpace.addIndexEntityQuerySpace(
							indexPersister
					);
					return new CollectionFetchableIndexEntityGraph( this, entityQuerySpace );
				}
			}
			else if ( type.isComponentType() ) {
				final ExpandingCompositeQuerySpace compositeQuerySpace = collectionQuerySpace.addIndexCompositeQuerySpace(
						(CompositeType) type
				);
				return new CollectionFetchableIndexCompositeGraph( this, compositeQuerySpace );
			}
		}

		return null;
	}

	private CollectionFetchableElement buildElementGraph(
			ExpandingCollectionQuerySpace collectionQuerySpace) {
		final CollectionPersister persister = collectionQuerySpace.getCollectionPersister();
		final Type type = persister.getElementType();
		if ( type.isAssociationType() ) {
			if ( type.isEntityType() ) {
				final EntityPersister elementPersister = persister.getFactory().getEntityPersister(
						( (EntityType) type ).getAssociatedEntityName()
				);

				final ExpandingEntityQuerySpace entityQuerySpace = collectionQuerySpace.addElementEntityQuerySpace(
						elementPersister
				);
				return new CollectionFetchableElementEntityGraph( this, entityQuerySpace );
			}
		}
		else if ( type.isComponentType() ) {
			final ExpandingCompositeQuerySpace compositeQuerySpace = collectionQuerySpace.addElementCompositeQuerySpace(
					(CompositeType) type
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
