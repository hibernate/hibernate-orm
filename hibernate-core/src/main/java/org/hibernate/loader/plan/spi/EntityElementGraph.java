package org.hibernate.loader.plan.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AssociationType;

/**
 * @author Steve Ebersole
 */
public class EntityElementGraph extends AbstractFetchOwner implements FetchableCollectionElement, EntityReference {
	private final CollectionReference collectionReference;
	private final CollectionPersister collectionPersister;
	private final AssociationType elementType;
	private final EntityPersister elementPersister;
	private final PropertyPath propertyPath;
	private final FetchOwnerDelegate fetchOwnerDelegate;

	private IdentifierDescription identifierDescription;

	public EntityElementGraph(
			SessionFactoryImplementor sessionFactory,
			CollectionReference collectionReference,
			PropertyPath collectionPath) {
		super( sessionFactory );

		this.collectionReference = collectionReference;
		this.collectionPersister = collectionReference.getCollectionPersister();
		this.elementType = (AssociationType) collectionPersister.getElementType();
		this.elementPersister = (EntityPersister) this.elementType.getAssociatedJoinable( sessionFactory() );
		this.propertyPath = collectionPath;
		this.fetchOwnerDelegate = new EntityFetchOwnerDelegate( elementPersister );
	}

	public EntityElementGraph(EntityElementGraph original, CopyContext copyContext) {
		super( original, copyContext );

		this.collectionReference = original.collectionReference;
		this.collectionPersister = original.collectionReference.getCollectionPersister();
		this.elementType = original.elementType;
		this.elementPersister = original.elementPersister;
		this.propertyPath = original.propertyPath;
		this.fetchOwnerDelegate = original.fetchOwnerDelegate;
	}

	@Override
	public LockMode getLockMode() {
		return null;
	}

	@Override
	public EntityReference getEntityReference() {
		return this;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return elementPersister;
	}

	@Override
	public IdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy) {
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return elementPersister;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
		this.identifierDescription = identifierDescription;
	}

	@Override
	public EntityElementGraph makeCopy(CopyContext copyContext) {
		return new EntityElementGraph( this, copyContext );
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public String toString() {
		return "EntityElementGraph(collection=" + collectionPersister.getRole() + ", type=" + elementPersister.getEntityName() + ")";
	}

	@Override
	protected FetchOwnerDelegate getFetchOwnerDelegate() {
		return fetchOwnerDelegate;
	}
}
