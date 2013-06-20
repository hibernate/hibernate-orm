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
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.type.CompositeType;

/**
 * Represents the {@link FetchOwner} for a composite collection element.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class CompositeElementGraph extends AbstractFetchOwner implements FetchableCollectionElement {
	private final CollectionReference collectionReference;
	private final PropertyPath propertyPath;
	private final CollectionPersister collectionPersister;
	private final CompositeBasedSqlSelectFragmentResolver sqlSelectFragmentResolver;

	/**
	 * Constructs a {@link CompositeElementGraph}.
	 *
	 * @param sessionFactory - the session factory.
	 * @param collectionReference - the collection reference.
	 * @param collectionPath - the {@link PropertyPath} for the collection.
	 */
	public CompositeElementGraph(
			SessionFactoryImplementor sessionFactory,
			CollectionReference collectionReference,
			PropertyPath collectionPath) {
		super( sessionFactory );

		this.collectionReference = collectionReference;
		this.collectionPersister = collectionReference.getCollectionPersister();
		this.propertyPath = collectionPath.append( "<elements>" );
		this.sqlSelectFragmentResolver = new CompositeBasedSqlSelectFragmentResolver(
				sessionFactory,
				(CompositeType) collectionPersister.getElementType(),
				new CompositeBasedSqlSelectFragmentResolver.BaseSqlSelectFragmentResolver() {
					@Override
					public String[] toSqlSelectFragments(String alias) {
						return ( (QueryableCollection) collectionPersister ).getElementColumnNames( alias );
					}
				}

		);
	}

	public CompositeElementGraph(CompositeElementGraph original, CopyContext copyContext) {
		super( original, copyContext );
		this.collectionReference = original.collectionReference;
		this.collectionPersister = original.collectionPersister;
		this.propertyPath = original.propertyPath;
		this.sqlSelectFragmentResolver = original.sqlSelectFragmentResolver;
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
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
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		throw new HibernateException( "Collection composite element cannot define collections" );
	}

	@Override
	public SqlSelectFragmentResolver toSqlSelectFragmentResolver() {
		return sqlSelectFragmentResolver;
	}
}
