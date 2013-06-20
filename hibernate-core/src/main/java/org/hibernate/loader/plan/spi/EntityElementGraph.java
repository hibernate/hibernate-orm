package org.hibernate.loader.plan.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.build.LoadPlanBuildingContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;

/**
 *  Represents the {@link FetchOwner} for a collection element that is
 *  an entity association type.
 *
 * @author Steve Ebersole
 */
public class EntityElementGraph extends AbstractFetchOwner implements FetchableCollectionElement, EntityReference {
	private final CollectionReference collectionReference;
	private final CollectionPersister collectionPersister;
	private final AssociationType elementType;
	private final EntityPersister elementPersister;
	private final PropertyPath propertyPath;
	private final EntityPersisterBasedSqlSelectFragmentResolver sqlSelectFragmentResolver;

	private IdentifierDescription identifierDescription;

	/**
	 * Constructs an {@link EntityElementGraph}.
	 *
	 * @param sessionFactory - the session factory.
	 * @param collectionReference - the collection reference.
	 * @param collectionPath - the {@link PropertyPath} for the collection.
	 */
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
		this.sqlSelectFragmentResolver = new EntityPersisterBasedSqlSelectFragmentResolver( (Queryable) elementPersister );
	}

	public EntityElementGraph(EntityElementGraph original, CopyContext copyContext) {
		super( original, copyContext );

		this.collectionReference = original.collectionReference;
		this.collectionPersister = original.collectionReference.getCollectionPersister();
		this.elementType = original.elementType;
		this.elementPersister = original.elementPersister;
		this.propertyPath = original.propertyPath;
		this.sqlSelectFragmentResolver = original.sqlSelectFragmentResolver;
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
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
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
	public SqlSelectFragmentResolver toSqlSelectFragmentResolver() {
		return sqlSelectFragmentResolver;
	}

	@Override
	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		final EntityType attributeType = (EntityType) attributeDefinition.getType();

		final FetchOwner collectionOwner = CollectionFetch.class.isInstance( collectionReference )
				? ( (CollectionFetch) collectionReference ).getOwner()
				: null;

		if ( collectionOwner != null ) {
			// check for bi-directionality
			final boolean sameType = attributeType.getAssociatedEntityName().equals(
					collectionOwner.retrieveFetchSourcePersister().getEntityName()
			);

			if ( sameType ) {
				// todo : check for columns too...

				return new BidirectionalEntityElementGraphFetch(
						sessionFactory(),
						LockMode.READ,
						this,
						attributeDefinition,
						fetchStrategy,
						collectionOwner
				);
			}
		}

		return super.buildEntityFetch(
				attributeDefinition,
				fetchStrategy,
				loadPlanBuildingContext
		);
	}

	private class BidirectionalEntityElementGraphFetch extends EntityFetch implements BidirectionalEntityFetch {
		private final FetchOwner collectionOwner;

		public BidirectionalEntityElementGraphFetch(
				SessionFactoryImplementor sessionFactory,
				LockMode lockMode,
				FetchOwner owner,
				AttributeDefinition fetchedAttribute,
				FetchStrategy fetchStrategy,
				FetchOwner collectionOwner) {
			super( sessionFactory, lockMode, owner, fetchedAttribute, fetchStrategy );
			this.collectionOwner = collectionOwner;
		}

		@Override
		public EntityReference getTargetEntityReference() {
			return (EntityReference) collectionOwner;
		}
	}
}
