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
import org.hibernate.engine.FetchStyle;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.internal.CollectionQuerySpaceImpl;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan2.spi.CollectionReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import static org.hibernate.loader.plan2.build.internal.CollectionQuerySpaceImpl.CollectionIndexCompositeJoin;

/**
 * @author Steve Ebersole
 */
public class CollectionFetchableIndexCompositeGraph
		extends AbstractCompositeFetch
		implements CompositeFetch, CollectionFetchableIndex {

	private final CollectionReference collectionReference;
	private final CollectionIndexCompositeJoin compositeJoin;

	public CollectionFetchableIndexCompositeGraph(
			CollectionReference collectionReference,
			CollectionIndexCompositeJoin compositeJoin) {
		super(
				(CompositeType) compositeJoin.getCollectionQuerySpace().getCollectionPersister().getIndexType(),
				collectionReference.getPropertyPath().append( "<index>" )
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
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		// metamodel should already disallow collections to be defined as part of a collection composite-index
		// so, nothing to do here
		super.validateFetchPlan( fetchStrategy, attributeDefinition );
	}

	// todo : override buildCompositeFetch as well to account for nested composites?
	// 		the idea would be to find nested composites attempting to define a collection.  but again, the metramodel
	//		is supposed to disallow that anyway.

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		throw new WalkingException( "Encountered collection as part of the Map composite-index" );
	}
}
