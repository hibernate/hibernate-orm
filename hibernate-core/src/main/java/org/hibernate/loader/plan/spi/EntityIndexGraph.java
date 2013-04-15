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
package org.hibernate.loader.plan.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.internal.LoadPlanBuildingHelper;
import org.hibernate.loader.plan.spi.build.LoadPlanBuildingContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.AssociationType;

/**
 * @author Steve Ebersole
 */
public class EntityIndexGraph extends AbstractPlanNode implements FetchableCollectionIndex, EntityReference {
	private final CollectionReference collectionReference;
	private final CollectionPersister collectionPersister;
	private final AssociationType indexType;
	private final EntityPersister indexPersister;
	private final PropertyPath propertyPath;

	private List<Fetch> fetches;

	private IdentifierDescription identifierDescription;

	public EntityIndexGraph(
			SessionFactoryImplementor sessionFactory,
			CollectionReference collectionReference,
			PropertyPath collectionPath) {
		super( sessionFactory );
		this.collectionReference = collectionReference;
		this.collectionPersister = collectionReference.getCollectionPersister();
		this.indexType = (AssociationType) collectionPersister.getIndexType();
		this.indexPersister = (EntityPersister) this.indexType.getAssociatedJoinable( sessionFactory() );
		this.propertyPath = collectionPath.append( "<index>" ); // todo : do we want the <index> part?
	}

	public EntityIndexGraph(EntityIndexGraph original, CopyContext copyContext) {
		super( original );
		this.collectionReference = original.collectionReference;
		this.collectionPersister = original.collectionReference.getCollectionPersister();
		this.indexType = original.indexType;
		this.indexPersister = original.indexPersister;
		this.propertyPath = original.propertyPath;

		copyContext.getReturnGraphVisitationStrategy().startingFetches( original );
		if ( fetches == null || fetches.size() == 0 ) {
			this.fetches = Collections.emptyList();
		}
		else {
			List<Fetch> fetchesCopy = new ArrayList<Fetch>();
			for ( Fetch fetch : fetches ) {
				fetchesCopy.add( fetch.makeCopy( copyContext, this ) );
			}
			this.fetches = fetchesCopy;
		}
		copyContext.getReturnGraphVisitationStrategy().finishingFetches( original );
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public String getSqlTableAlias() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public LockMode getLockMode() {
		return null;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return indexPersister;
	}

	@Override
	public IdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	@Override
	public EntityAliases getEntityAliases() {
		return null;
	}

	@Override
	public void addFetch(Fetch fetch) {
		if ( fetches == null ) {
			fetches = new ArrayList<Fetch>();
		}
		fetches.add( fetch );
	}

	@Override
	public Fetch[] getFetches() {
		return fetches == null ? NO_FETCHES : fetches.toArray( new Fetch[ fetches.size() ] );
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy) {
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return indexPersister;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardCollectionFetch(
				this,
				attributeDefinition,
				fetchStrategy,
				loadPlanBuildingContext
		);
	}

	@Override
	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			String sqlTableAlias,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardEntityFetch(
				this,
				attributeDefinition,
				fetchStrategy,
				sqlTableAlias,
				loadPlanBuildingContext
		);
	}

	@Override
	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardCompositeFetch( this, attributeDefinition, loadPlanBuildingContext );
	}

	@Override
	public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
		this.identifierDescription = identifierDescription;
	}

	@Override
	public EntityIndexGraph makeCopy(CopyContext copyContext) {
		return new EntityIndexGraph( this, copyContext );
	}
}
