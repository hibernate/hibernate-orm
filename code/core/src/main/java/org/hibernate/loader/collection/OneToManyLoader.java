//$Id: OneToManyLoader.java 7123 2005-06-13 20:10:20Z oneovthafew $
package org.hibernate.loader.collection;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
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

	private static final Log log = LogFactory.getLog(OneToManyLoader.class);

	public OneToManyLoader(
			QueryableCollection oneToManyPersister, 
			SessionFactoryImplementor session, 
			Map enabledFilters)
	throws MappingException {
		this(oneToManyPersister, 1, session, enabledFilters);
	}

	public OneToManyLoader(
			QueryableCollection oneToManyPersister, 
			int batchSize, 
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {
		this(oneToManyPersister, batchSize, null, factory, enabledFilters);
	}

	public OneToManyLoader(
			QueryableCollection oneToManyPersister, 
			int batchSize, 
			String subquery, 
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {

		super(oneToManyPersister, factory, enabledFilters);
		
		JoinWalker walker = new OneToManyJoinWalker(
				oneToManyPersister, 
				batchSize, 
				subquery, 
				factory, 
				enabledFilters
			);
		initFromWalker( walker );

		postInstantiate();

		log.debug( "Static select for one-to-many " + oneToManyPersister.getRole() + ": " + getSQLString() );
	}

}
