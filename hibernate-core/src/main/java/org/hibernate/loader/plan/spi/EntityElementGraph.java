package org.hibernate.loader.plan.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.internal.LoadPlanBuildingHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.AssociationType;

/**
 * @author Steve Ebersole
 */
public class EntityElementGraph extends AbstractPlanNode implements FetchOwner, EntityReference {
	private final CollectionReference collectionReference;
	private final CollectionPersister collectionPersister;
	private final AssociationType elementType;
	private final EntityPersister elementPersister;
	private final PropertyPath propertyPath;

	private List<Fetch> fetches;

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
		this.propertyPath = collectionPath.append( "<elements>" );
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
		return elementPersister;
	}

	@Override
	public IdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	@Override
	public EntityAliases getEntityAliases() {
		return collectionReference.getElementEntityAliases();
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
		return elementPersister;
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
	public String toString() {
		return "EntityElementGraph(collection=" + collectionPersister.getRole() + ", type=" + elementPersister.getEntityName() + ")";
	}
}
