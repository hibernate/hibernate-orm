/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
