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
public class CompositeElementGraph extends AbstractFetchOwner implements FetchableCollectionElement {
	private final CollectionReference collectionReference;
	private final PropertyPath propertyPath;
	private final CollectionPersister collectionPersister;
	private final FetchOwnerDelegate fetchOwnerDelegate;

	public CompositeElementGraph(
			SessionFactoryImplementor sessionFactory,
			CollectionReference collectionReference,
			PropertyPath collectionPath) {
		super( sessionFactory );

		this.collectionReference = collectionReference;
		this.collectionPersister = collectionReference.getCollectionPersister();
		this.propertyPath = collectionPath.append( "<elements>" );
		this.fetchOwnerDelegate = new CompositeFetchOwnerDelegate(
				sessionFactory,
				(CompositeType) collectionPersister.getElementType(),
				( (QueryableCollection) collectionPersister ).getElementColumnNames()
		);
	}

	public CompositeElementGraph(CompositeElementGraph original, CopyContext copyContext) {
		super( original, copyContext );
		this.collectionReference = original.collectionReference;
		this.collectionPersister = original.collectionPersister;
		this.propertyPath = original.propertyPath;
		this.fetchOwnerDelegate = original.fetchOwnerDelegate;
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
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
	public CompositeElementGraph makeCopy(CopyContext copyContext) {
		return new CompositeElementGraph( this, copyContext );
	}

	@Override
	protected FetchOwnerDelegate getFetchOwnerDelegate() {
		return fetchOwnerDelegate;
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		throw new HibernateException( "Collection composite element cannot define collections" );
	}
}
