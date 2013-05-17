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
package org.hibernate.loader.plan.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.internal.LoadPlanBuildingHelper;
import org.hibernate.loader.plan.spi.build.LoadPlanBuildingContext;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class CompositeFetch extends AbstractSingularAttributeFetch {
	public static final FetchStrategy FETCH_PLAN = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );

	private final FetchOwnerDelegate delegate;

	public CompositeFetch(
			SessionFactoryImplementor sessionFactory,
			FetchOwner owner,
			String ownerProperty) {
		super( sessionFactory, owner, ownerProperty, FETCH_PLAN );
		this.delegate = new CompositeFetchOwnerDelegate(
				sessionFactory,
				(CompositeType) getOwner().getType( this ),
				getOwner().getColumnNames( this )
		);
	}

	public CompositeFetch(CompositeFetch original, CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		super( original, copyContext, fetchOwnerCopy );
		this.delegate = original.getFetchOwnerDelegate();
	}

	@Override
	protected FetchOwnerDelegate getFetchOwnerDelegate() {
		return delegate;
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return getOwner().retrieveFetchSourcePersister();
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
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
			CompositionDefinition attributeDefinition, LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardCompositeFetch( this, attributeDefinition, loadPlanBuildingContext );
	}

	@Override
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public CompositeFetch makeCopy(CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		copyContext.getReturnGraphVisitationStrategy().startingCompositeFetch( this );
		final CompositeFetch copy = new CompositeFetch( this, copyContext, fetchOwnerCopy );
		copyContext.getReturnGraphVisitationStrategy().finishingCompositeFetch( this );
		return copy;
	}
}
