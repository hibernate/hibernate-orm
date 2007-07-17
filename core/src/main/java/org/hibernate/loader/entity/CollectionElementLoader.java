//$Id: CollectionElementLoader.java 9636 2006-03-16 14:14:48Z max.andersen@jboss.com $
package org.hibernate.loader.entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.JoinWalker;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * 
 *
 * @author Gavin King
 */
public class CollectionElementLoader extends OuterJoinLoader {
	
	private static final Log log = LogFactory.getLog(CollectionElementLoader.class);

	private final OuterJoinLoadable persister;
	private final Type keyType;
	private final Type indexType;
	private final String entityName;

	public CollectionElementLoader(
			QueryableCollection collectionPersister,
			SessionFactoryImplementor factory, 
			Map enabledFilters) 
	throws MappingException {
		super(factory, enabledFilters);

		this.keyType = collectionPersister.getKeyType();
		this.indexType = collectionPersister.getIndexType();
		this.persister = (OuterJoinLoadable) collectionPersister.getElementPersister();
		this.entityName = persister.getEntityName();
		
		JoinWalker walker = new EntityJoinWalker(
				persister, 
				ArrayHelper.join( 
						collectionPersister.getKeyColumnNames(), 
						collectionPersister.getIndexColumnNames()
					),
				1, 
				LockMode.NONE, 
				factory, 
				enabledFilters
			);
		initFromWalker( walker );

		postInstantiate();
		
		log.debug( "Static select for entity " + entityName + ": " + getSQLString() );

	}

	public Object loadElement(SessionImplementor session, Object key, Object index) 
	throws HibernateException {
		
		List list = loadEntity(
				session, 
				key,
				index,
				keyType, 
				indexType,
				persister
			);
		
		if ( list.size()==1 ) {
			return list.get(0);
		}
		else if ( list.size()==0 ) {
			return null;
		}
		else {
			if ( getCollectionOwners()!=null ) {
				return list.get(0);
			}
			else {
				throw new HibernateException("More than one row was found");
			}
		}
		
	}

	protected Object getResultColumnOrRow(
		Object[] row,
		ResultTransformer transformer,
		ResultSet rs, SessionImplementor session)
	throws SQLException, HibernateException {
		return row[row.length-1];
	}

	protected boolean isSingleRowLoader() {
		return true;
	}

	
}