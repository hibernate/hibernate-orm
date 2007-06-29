//$Id: CollectionLoader.java 7124 2005-06-13 20:27:16Z oneovthafew $
package org.hibernate.loader.collection;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
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

	public CollectionLoader(QueryableCollection collectionPersister, SessionFactoryImplementor factory, Map enabledFilters) {
		super( factory, enabledFilters );
		this.collectionPersister = collectionPersister;
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
