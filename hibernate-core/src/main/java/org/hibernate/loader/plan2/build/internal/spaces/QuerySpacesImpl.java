/*
 * Hibernate, Relational Persistence for Idiomatic Java
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
package org.hibernate.loader.plan2.build.internal.spaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan2.spi.CollectionQuerySpace;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.QuerySpace;
import org.hibernate.loader.plan2.spi.QuerySpaceUidNotRegisteredException;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class QuerySpacesImpl implements ExpandingQuerySpaces {
	private static final Logger log = CoreLogging.logger( QuerySpacesImpl.class );

	private final SessionFactoryImplementor sessionFactory;
	private final List<QuerySpace> roots = new ArrayList<QuerySpace>();
	private final Map<String,QuerySpace> querySpaceByUid = new ConcurrentHashMap<String, QuerySpace>();

	public QuerySpacesImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}


	// QuerySpaces impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public List<QuerySpace> getRootQuerySpaces() {
		return roots;
	}

	@Override
	public QuerySpace findQuerySpaceByUid(String uid) {
		return querySpaceByUid.get( uid );
	}

	@Override
	public QuerySpace getQuerySpaceByUid(String uid) {
		final QuerySpace space = findQuerySpaceByUid( uid );
		if ( space == null ) {
			throw new QuerySpaceUidNotRegisteredException( uid );
		}
		return space;
	}

// ExpandingQuerySpaces impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private int implicitUidBase = 0;

	@Override
	public String generateImplicitUid() {
		return "<gen:" + implicitUidBase++ + ">";
	}

	@Override
	public EntityQuerySpace makeEntityQuerySpace(String uid, EntityPersister entityPersister) {
		if ( querySpaceByUid.containsKey( uid ) ) {
			throw new IllegalStateException( "Encountered duplicate QuerySpace uid : " + uid );
		}

		final EntityQuerySpaceImpl space = new EntityQuerySpaceImpl(
				entityPersister,
				uid,
				this,
				true,
				sessionFactory
		);
		registerQuerySpace( space );
		roots.add( space );

		return space;
	}

	@Override
	public CollectionQuerySpace makeCollectionQuerySpace(String uid, CollectionPersister collectionPersister) {
		if ( querySpaceByUid.containsKey( uid ) ) {
			throw new IllegalStateException( "Encountered duplicate QuerySpace uid : " + uid );
		}

		final CollectionQuerySpaceImpl space = new CollectionQuerySpaceImpl(
				collectionPersister,
				uid,
				this,
				true,
				sessionFactory
		);
		registerQuerySpace( space );
		roots.add( space );

		return space;
	}

	/**
	 * Feeds a QuerySpace into this spaces group.
	 *
	 * @param querySpace The space
	 */
	public void registerQuerySpace(QuerySpace querySpace) {
		log.debugf(
				"Adding QuerySpace : uid = %s -> %s]",
				querySpace.getUid(),
				querySpace
		);
		final QuerySpace previous = querySpaceByUid.put( querySpace.getUid(), querySpace );
		if ( previous != null ) {
			throw new IllegalStateException( "Encountered duplicate QuerySpace uid : " + querySpace.getUid() );
		}
	}

}
