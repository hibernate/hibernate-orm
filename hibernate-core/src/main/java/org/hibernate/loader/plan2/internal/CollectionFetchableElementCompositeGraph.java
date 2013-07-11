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
package org.hibernate.loader.plan2.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CollectionFetchableElement;
import org.hibernate.loader.plan2.spi.CollectionReference;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CompositeType;

import static org.hibernate.loader.plan2.build.internal.CollectionQuerySpaceImpl.CollectionElementCompositeJoin;

/**
 * @author Steve Ebersole
 */
public class CollectionFetchableElementCompositeGraph
		extends AbstractCompositeFetch
		implements CompositeFetch, CollectionFetchableElement {

	private final CollectionReference collectionReference;
	private final CollectionElementCompositeJoin compositeJoin;

	public CollectionFetchableElementCompositeGraph(
			CollectionReference collectionReference,
			CollectionElementCompositeJoin compositeJoin) {
		super(
				(CompositeType) compositeJoin.getCollectionQuerySpace().getCollectionPersister().getIndexType(),
				collectionReference.getPropertyPath().append( "<element>" )
		);
		this.collectionReference = collectionReference;
		this.compositeJoin = compositeJoin;
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public FetchSource getSource() {
		return collectionReference.getIndexGraph();
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		throw new WalkingException( "Encountered collection as part of fetched Collection composite-element" );
	}
}
