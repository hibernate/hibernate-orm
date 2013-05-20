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
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.type.CollectionType;

/**
 * @author Steve Ebersole
 */
public class CollectionFetch extends AbstractCollectionReference implements Fetch {
	private final FetchOwner fetchOwner;
	private final FetchStrategy fetchStrategy;

	public CollectionFetch(
			SessionFactoryImplementor sessionFactory,
			LockMode lockMode,
			FetchOwner fetchOwner,
			FetchStrategy fetchStrategy,
			String ownerProperty) {
		super(
				sessionFactory,
				lockMode,
				sessionFactory.getCollectionPersister(
						fetchOwner.retrieveFetchSourcePersister().getEntityName() + '.' + ownerProperty
				),
				fetchOwner.getPropertyPath().append( ownerProperty )
		);
		this.fetchOwner = fetchOwner;
		this.fetchStrategy = fetchStrategy;
		fetchOwner.addFetch( this );
	}

	protected CollectionFetch(CollectionFetch original, CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		super( original, copyContext );
		this.fetchOwner = fetchOwnerCopy;
		this.fetchStrategy = original.fetchStrategy;
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
	public boolean isNullable() {
		return true;
	}

	@Override
	public String[] getColumnNames() {
		return getOwner().getColumnNames( this );
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

	@Override
	public CollectionFetch makeCopy(CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		copyContext.getReturnGraphVisitationStrategy().startingCollectionFetch( this );
		final CollectionFetch copy = new CollectionFetch( this, copyContext, fetchOwnerCopy );
		copyContext.getReturnGraphVisitationStrategy().finishingCollectionFetch( this );
		return copy;
	}
}
