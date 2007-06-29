//$Id: NamedQueryLoader.java 10019 2006-06-15 07:50:12Z steve.ebersole@jboss.com $
package org.hibernate.persister.entity;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.AbstractQueryImpl;
import org.hibernate.loader.entity.UniqueEntityLoader;

/**
 * Not really a <tt>Loader</tt>, just a wrapper around a
 * named query.
 * @author Gavin King
 */
public final class NamedQueryLoader implements UniqueEntityLoader {
	private final String queryName;
	private final EntityPersister persister;
	
	private static final Log log = LogFactory.getLog(NamedQueryLoader.class);

	public NamedQueryLoader(String queryName, EntityPersister persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session) 
	throws HibernateException {
		
		if ( log.isDebugEnabled() ) {
			log.debug(
					"loading entity: " + persister.getEntityName() + 
					" using named query: " + queryName 
				);
		}
		
		AbstractQueryImpl query = (AbstractQueryImpl) session.getNamedQuery(queryName);
		if ( query.hasNamedParameters() ) {
			query.setParameter( 
					query.getNamedParameters()[0], 
					id, 
					persister.getIdentifierType() 
				);
		}
		else {
			query.setParameter( 0, id, persister.getIdentifierType() );
		}
		query.setOptionalId(id);
		query.setOptionalEntityName( persister.getEntityName() );
		query.setOptionalObject(optionalObject);
		query.setFlushMode( FlushMode.MANUAL );
		query.list();
		
		// now look up the object we are really interested in!
		// (this lets us correctly handle proxies and multi-row
		// or multi-column queries)
		return session.getPersistenceContext()
				.getEntity( new EntityKey( id, persister, session.getEntityMode() ) );

	}
}