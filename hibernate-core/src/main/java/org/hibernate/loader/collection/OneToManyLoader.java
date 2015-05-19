/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.collection;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.JoinWalker;
import org.hibernate.persister.collection.QueryableCollection;

import org.jboss.logging.Logger;

/**
 * Loads one-to-many associations<br>
 * <br>
 * The collection persister must implement <tt>QueryableCOllection<tt>. For
 * other collections, create a customized subclass of <tt>Loader</tt>.
 *
 * @see BasicCollectionLoader
 * @author Gavin King
 */
public class OneToManyLoader extends CollectionLoader {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, OneToManyLoader.class.getName() );

	public OneToManyLoader(
			QueryableCollection oneToManyPersister,
			SessionFactoryImplementor session,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this( oneToManyPersister, 1, session, loadQueryInfluencers );
	}

	public OneToManyLoader(
			QueryableCollection oneToManyPersister,
			int batchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this( oneToManyPersister, batchSize, null, factory, loadQueryInfluencers );
	}

	public OneToManyLoader(
			QueryableCollection oneToManyPersister,
			int batchSize,
			String subquery,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( oneToManyPersister, factory, loadQueryInfluencers );

		JoinWalker walker = new OneToManyJoinWalker(
				oneToManyPersister,
				batchSize,
				subquery,
				factory,
				loadQueryInfluencers
		);
		initFromWalker( walker );

		postInstantiate();
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static select for one-to-many %s: %s", oneToManyPersister.getRole(), getSQLString() );
		}
	}
}
