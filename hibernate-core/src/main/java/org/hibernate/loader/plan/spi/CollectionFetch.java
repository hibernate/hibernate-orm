/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.spi.ResultSetProcessingContext;

/**
 * @author Steve Ebersole
 */
public class CollectionFetch extends AbstractCollectionReference implements Fetch {
	private final FetchOwner fetchOwner;
	private final FetchStrategy fetchStrategy;

	public CollectionFetch(
			SessionFactoryImplementor sessionFactory,
			String alias,
			LockMode lockMode,
			FetchOwner fetchOwner,
			FetchStrategy fetchStrategy,
			String ownerProperty,
			CollectionAliases collectionAliases,
			EntityAliases elementEntityAliases) {
		super(
				sessionFactory,
				alias,
				lockMode,
				sessionFactory.getCollectionPersister(
						fetchOwner.retrieveFetchSourcePersister().getEntityName() + '.' + ownerProperty
				),
				fetchOwner.getPropertyPath().append( ownerProperty ),
				collectionAliases,
				elementEntityAliases
		);
		this.fetchOwner = fetchOwner;
		this.fetchStrategy = fetchStrategy;
	}

	@Override
	public FetchOwner getOwner() {
		return fetchOwner;
	}

	@Override
	public String getOwnerPropertyName() {
		return getPropertyPath().getProperty();
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
