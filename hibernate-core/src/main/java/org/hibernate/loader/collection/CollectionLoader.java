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
import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

/**
 * Superclass for loaders that initialize collections
 * 
 * @see OneToManyLoader
 * @see BasicCollectionLoader
 * @author Gavin King
 */
public class CollectionLoader extends OuterJoinLoader implements CollectionInitializer {

	private final QueryableCollection collectionPersister;

	public CollectionLoader(
			QueryableCollection collectionPersister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( factory, loadQueryInfluencers );
		this.collectionPersister = collectionPersister;
	}

	protected QueryableCollection collectionPersister() {
		return collectionPersister;
	}

	protected boolean isSubselectLoadingEnabled() {
		return hasSubselectLoadableCollections();
	}

	public void initialize(Serializable id, SessionImplementor session)
	throws HibernateException {
		loadCollection( session, id, getKeyType() );
	}

	protected Type getKeyType() {
		return collectionPersister.getKeyType();
	}

	public String toString() {
		return getClass().getName() + '(' + collectionPersister.getRole() + ')';
	}
}
