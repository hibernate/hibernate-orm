/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.loader.collection;
import org.jboss.logging.Logger;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.JoinWalker;
import org.hibernate.persister.collection.QueryableCollection;

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
