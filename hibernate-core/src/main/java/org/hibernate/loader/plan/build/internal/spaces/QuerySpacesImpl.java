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
package org.hibernate.loader.plan.build.internal.spaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.build.spi.ExpandingCollectionQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.loader.plan.spi.QuerySpaceUidNotRegisteredException;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;

import org.jboss.logging.Logger;

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

	private int implicitUidBase;

	@Override
	public String generateImplicitUid() {
		return "<gen:" + implicitUidBase++ + ">";
	}

	@Override
	public ExpandingEntityQuerySpace makeRootEntityQuerySpace(String uid, EntityPersister entityPersister) {
		final ExpandingEntityQuerySpace space = makeEntityQuerySpace( uid, entityPersister, true );
		roots.add( space );
		return space;
	}

	@Override
	public ExpandingEntityQuerySpace makeEntityQuerySpace(
			String uid,
			EntityPersister entityPersister,
			boolean canJoinsBeRequired) {

		checkQuerySpaceDoesNotExist( uid );

		// as a temporary fix for HHH-8980 and HHH-8830 we circumvent allowing
		// inner joins (canJoinsBeRequired) when the persister is part of an
		// entity inheritance.
		//
		// hasSubclasses() is the closest we can come to even knowing if the
		// entity is part of a hierarchy.  But its enough, since if there are no
		// subclasses we cannot have the case where the attribute to join comes from
		// a subclass :)
		//
		// a better long term fix is to expose whether a joined association attribute
		// is defined on the class/superClass(es) or on the subClass(es).  Attributes
		// from the subClass(es) should not be inner joined; it is ok to inner join
		// attributes from the class/superClass(es).

		final EntityQuerySpaceImpl space = new EntityQuerySpaceImpl(
				entityPersister,
				uid,
				this,
				canJoinsBeRequired && !entityPersister.getEntityMetamodel().hasSubclasses()
		);
		registerQuerySpace( space );

		return space;
	}

	@Override
	public ExpandingCollectionQuerySpace makeRootCollectionQuerySpace(String uid, CollectionPersister collectionPersister) {
		final ExpandingCollectionQuerySpace space = makeCollectionQuerySpace( uid, collectionPersister, true );
		roots.add( space );
		return space;
	}

	@Override
	public ExpandingCollectionQuerySpace makeCollectionQuerySpace(
			String uid,
			CollectionPersister collectionPersister,
			boolean canJoinsBeRequired) {

		checkQuerySpaceDoesNotExist( uid );

		final ExpandingCollectionQuerySpace space = new CollectionQuerySpaceImpl(
				collectionPersister,
				uid,
				this,
				canJoinsBeRequired
		);
		registerQuerySpace( space );

		return space;
	}

	@Override
	public ExpandingCompositeQuerySpace makeCompositeQuerySpace(
			String uid,
			CompositePropertyMapping compositePropertyMapping,
			boolean canJoinsBeRequired) {

		checkQuerySpaceDoesNotExist( uid );

		final ExpandingCompositeQuerySpace space = new CompositeQuerySpaceImpl(
				compositePropertyMapping,
				uid,
				this,
				canJoinsBeRequired
		);
		registerQuerySpace( space );

		return space;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	private void checkQuerySpaceDoesNotExist(String uid) {
		if ( querySpaceByUid.containsKey( uid ) ) {
			throw new IllegalStateException( "Encountered duplicate QuerySpace uid : " + uid );
		}
	}

	/**
	 * Feeds a QuerySpace into this spaces group.
	 *
	 * @param querySpace The space
	 */
	private void registerQuerySpace(QuerySpace querySpace) {
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
