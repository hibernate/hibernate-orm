package org.hibernate.loader.plan.spi;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.build.LoadPlanBuildingContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class CompositeIndexGraph extends AbstractFetchOwner implements FetchableCollectionIndex {
	private final CollectionReference collectionReference;
	private final PropertyPath propertyPath;
	private final CollectionPersister collectionPersister;
	private final FetchOwnerDelegate fetchOwnerDelegate;

	public CompositeIndexGraph(
			SessionFactoryImplementor sessionFactory,
			CollectionReference collectionReference,
			PropertyPath propertyPath) {
		super( sessionFactory );
		this.collectionReference = collectionReference;
		this.collectionPersister = collectionReference.getCollectionPersister();
		this.propertyPath = propertyPath.append( "<index>" );
		this.fetchOwnerDelegate = new CompositeFetchOwnerDelegate(
				sessionFactory,
				(CompositeType) collectionPersister.getIndexType(),
				( (QueryableCollection) collectionPersister ).getIndexColumnNames()
		);
	}

	protected CompositeIndexGraph(CompositeIndexGraph original, CopyContext copyContext) {
		super( original, copyContext );
		this.collectionReference = original.collectionReference;
		this.collectionPersister = original.collectionPersister;
		this.propertyPath = original.propertyPath;
		this.fetchOwnerDelegate = original.fetchOwnerDelegate;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy) {
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return collectionPersister.getOwnerEntityPersister();
	}

	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public CompositeIndexGraph makeCopy(CopyContext copyContext) {
		return new CompositeIndexGraph( this, copyContext );
	}

	@Override
	protected FetchOwnerDelegate getFetchOwnerDelegate() {
		return fetchOwnerDelegate;
	}

	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		throw new HibernateException( "Composite index cannot define collections" );
	}

}
