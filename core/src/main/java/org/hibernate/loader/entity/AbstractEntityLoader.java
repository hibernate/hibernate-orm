//$Id: AbstractEntityLoader.java 9636 2006-03-16 14:14:48Z max.andersen@jboss.com $
package org.hibernate.loader.entity;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

public abstract class AbstractEntityLoader extends OuterJoinLoader 
		implements UniqueEntityLoader {

	protected static final Log log = LogFactory.getLog(EntityLoader.class);
	protected final OuterJoinLoadable persister;
	protected final Type uniqueKeyType;
	protected final String entityName;

	public AbstractEntityLoader(
			OuterJoinLoadable persister, 
			Type uniqueKeyType, 
			SessionFactoryImplementor factory, 
			Map enabledFilters) {
		super( factory, enabledFilters );
		this.uniqueKeyType = uniqueKeyType;
		this.entityName = persister.getEntityName();
		this.persister = persister;
		
	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session) 
	throws HibernateException {
		return load(session, id, optionalObject, id);
	}

	protected Object load(SessionImplementor session, Object id, Object optionalObject, Serializable optionalId) 
	throws HibernateException {
		
		List list = loadEntity(
				session, 
				id, 
				uniqueKeyType, 
				optionalObject, 
				entityName, 
				optionalId, 
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
				throw new HibernateException(
						"More than one row with the given identifier was found: " +
						id +
						", for class: " +
						persister.getEntityName()
					);
			}
		}
		
	}

	protected Object getResultColumnOrRow(Object[] row, ResultTransformer transformer, ResultSet rs, SessionImplementor session) 
	throws SQLException, HibernateException {
		return row[row.length-1];
	}

	protected boolean isSingleRowLoader() {
		return true;
	}

}
