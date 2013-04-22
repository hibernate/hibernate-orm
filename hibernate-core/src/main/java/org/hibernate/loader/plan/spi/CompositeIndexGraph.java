package org.hibernate.loader.plan.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.internal.LoadPlanBuildingHelper;
import org.hibernate.loader.plan.spi.build.LoadPlanBuildingContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;

/**
 * @author Steve Ebersole
 */
public class CompositeIndexGraph extends AbstractPlanNode implements FetchableCollectionIndex {
	private final CollectionReference collectionReference;
	private final PropertyPath propertyPath;
	private final CollectionPersister collectionPersister;

	private List<Fetch> fetches;

	public CompositeIndexGraph(
			SessionFactoryImplementor sessionFactory,
			CollectionReference collectionReference,
			PropertyPath propertyPath) {
		super( sessionFactory );
		this.collectionReference = collectionReference;
		this.collectionPersister = collectionReference.getCollectionPersister();
		this.propertyPath = propertyPath.append( "<index>" );
	}

	protected CompositeIndexGraph(CompositeIndexGraph original, CopyContext copyContext) {
		super( original );
		this.collectionReference = original.collectionReference;
		this.collectionPersister = original.collectionPersister;
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
		return collectionPersister.getOwnerEntityPersister();
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
		throw new HibernateException( "Composite index cannot define collections" );
	}

	@Override
	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardEntityFetch(
				this,
				attributeDefinition,
				fetchStrategy,
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
	public CompositeIndexGraph makeCopy(CopyContext copyContext) {
		return new CompositeIndexGraph( this, copyContext );
	}
}
