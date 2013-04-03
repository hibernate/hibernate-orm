/*
 * jDocBook, processing of DocBook sources
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
package org.hibernate.loader.plan.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.AbstractLoadPlanBuilderStrategy;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.LoadPlanBuilderStrategy;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * LoadPlanBuilderStrategy implementation used for building LoadPlans with a single processing RootEntity LoadPlan building.
 *
 * Really this is a single-root LoadPlan building strategy for building LoadPlans for:<ul>
 *     <li>entity load plans</li>
 *     <li>cascade load plans</li>
 *     <li>collection initializer plans</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class SingleRootReturnLoadPlanBuilderStrategy
		extends AbstractLoadPlanBuilderStrategy
		implements LoadPlanBuilderStrategy {

	private final LoadQueryInfluencers loadQueryInfluencers;

	private final String rootAlias;

	private Return rootReturn;

	private PropertyPath propertyPath = new PropertyPath( "" );

	public SingleRootReturnLoadPlanBuilderStrategy(
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers,
			String rootAlias,
			int suffixSeed) {
		super( sessionFactory, suffixSeed );
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.rootAlias = rootAlias;
	}

	@Override
	protected boolean supportsRootEntityReturns() {
		return true;
	}

	@Override
	protected boolean supportsRootCollectionReturns() {
		return true;
	}

	@Override
	protected void addRootReturn(Return rootReturn) {
		if ( this.rootReturn != null ) {
			throw new HibernateException( "Root return already identified" );
		}
		this.rootReturn = rootReturn;
	}

	@Override
	public LoadPlan buildLoadPlan() {
		return new LoadPlanImpl( false, rootReturn );
	}

	@Override
	protected FetchStrategy determineFetchPlan(AssociationAttributeDefinition attributeDefinition) {
		FetchStrategy fetchStrategy = attributeDefinition.determineFetchPlan( loadQueryInfluencers, propertyPath );
		if ( fetchStrategy.getTiming() == FetchTiming.IMMEDIATE && fetchStrategy.getStyle() == FetchStyle.JOIN ) {
			// see if we need to alter the join fetch to another form for any reason
			fetchStrategy = adjustJoinFetchIfNeeded( attributeDefinition, fetchStrategy );
		}
		return fetchStrategy;
	}

	protected FetchStrategy adjustJoinFetchIfNeeded(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {
		if ( currentDepth() > sessionFactory().getSettings().getMaximumFetchDepth() ) {
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		if ( attributeDefinition.getType().isCollectionType() && isTooManyCollections() ) {
			// todo : have this revert to batch or subselect fetching once "sql gen redesign" is in place
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		return fetchStrategy;
	}

	@Override
	protected boolean isTooManyCollections() {
		return false;
	}

	@Override
	protected EntityReturn buildRootEntityReturn(EntityDefinition entityDefinition) {
		final String entityName = entityDefinition.getEntityPersister().getEntityName();
		return new EntityReturn(
				sessionFactory(),
				rootAlias,
				LockMode.NONE, // todo : for now
				entityName,
				StringHelper.generateAlias( StringHelper.unqualifyEntityName( entityName ), currentDepth() ),
				generateEntityColumnAliases( entityDefinition.getEntityPersister() )
		);
	}

	@Override
	protected CollectionReturn buildRootCollectionReturn(CollectionDefinition collectionDefinition) {
		final CollectionPersister persister = collectionDefinition.getCollectionPersister();
		final String collectionRole = persister.getRole();

		final CollectionAliases collectionAliases = generateCollectionColumnAliases(
				collectionDefinition.getCollectionPersister()
		);

		final Type elementType = collectionDefinition.getCollectionPersister().getElementType();
		final EntityAliases elementAliases;
		if ( elementType.isEntityType() ) {
			final EntityType entityElementType = (EntityType) elementType;
			elementAliases = generateEntityColumnAliases(
					(EntityPersister) entityElementType.getAssociatedJoinable( sessionFactory() )
			);
		}
		else {
			elementAliases = null;
		}

		return new CollectionReturn(
				sessionFactory(),
				rootAlias,
				LockMode.NONE, // todo : for now
				persister.getOwnerEntityPersister().getEntityName(),
				StringHelper.unqualify( collectionRole ),
				collectionAliases,
				elementAliases
		);
	}


	// LoadPlanBuildingContext impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String resolveRootSourceAlias(EntityDefinition definition) {
		return rootAlias;
	}

	@Override
	public String resolveRootSourceAlias(CollectionDefinition definition) {
		return rootAlias;
	}
}
