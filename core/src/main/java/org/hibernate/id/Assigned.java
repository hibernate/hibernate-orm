//$Id: Assigned.java 6914 2005-05-26 03:58:24Z oneovthafew $
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

/**
 * <b>assigned</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns the current identifier assigned
 * to an instance.
 *
 * @author Gavin King
 */

public class Assigned implements IdentifierGenerator, Configurable {
	
	private String entityName;

	public Serializable generate(SessionImplementor session, Object obj) throws HibernateException {
		
		final Serializable id = session.getEntityPersister( entityName, obj ) 
				//TODO: cache the persister, this shows up in yourkit
				.getIdentifier( obj, session.getEntityMode() );
		
		if (id==null) {
			throw new IdentifierGenerationException(
				"ids for this class must be manually assigned before calling save(): " + 
				entityName
			);
		}
		
		return id;
	}

	public void configure(Type type, Properties params, Dialect d)
	throws MappingException {
		entityName = params.getProperty(ENTITY_NAME);
		if (entityName==null) {
			throw new MappingException("no entity name");
		}
	}
}






