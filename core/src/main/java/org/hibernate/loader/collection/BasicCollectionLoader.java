//$Id: BasicCollectionLoader.java 7123 2005-06-13 20:10:20Z oneovthafew $
package org.hibernate.loader.collection;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.loader.JoinWalker;
import org.hibernate.persister.collection.QueryableCollection;

/**
 * Loads a collection of values or a many-to-many association.
 * <br>
 * The collection persister must implement <tt>QueryableCOllection<tt>. For
 * other collections, create a customized subclass of <tt>Loader</tt>.
 *
 * @see OneToManyLoader
 * @author Gavin King
 */
public class BasicCollectionLoader extends CollectionLoader {

	private static final Log log = LogFactory.getLog(BasicCollectionLoader.class);

	public BasicCollectionLoader(
			QueryableCollection collectionPersister, 
			SessionFactoryImplementor session, 
			Map enabledFilters)
	throws MappingException {
		this(collectionPersister, 1, session, enabledFilters);
	}

	public BasicCollectionLoader(
			QueryableCollection collectionPersister, 
			int batchSize, 
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {
		this(collectionPersister, batchSize, null, factory, enabledFilters);
	}
	
	protected BasicCollectionLoader(
			QueryableCollection collectionPersister, 
			int batchSize, 
			String subquery, 
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {
		
		super(collectionPersister, factory, enabledFilters);
		
		JoinWalker walker = new BasicCollectionJoinWalker(
				collectionPersister, 
				batchSize, 
				subquery, 
				factory, 
				enabledFilters
			);
		initFromWalker( walker );

		postInstantiate();

		log.debug( "Static select for collection " + collectionPersister.getRole() + ": " + getSQLString() );
	}
	
}
