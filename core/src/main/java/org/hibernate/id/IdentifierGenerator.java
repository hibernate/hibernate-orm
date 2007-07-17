//$Id: IdentifierGenerator.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.id;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

import java.io.Serializable;

/**
 * The general contract between a class that generates unique
 * identifiers and the <tt>Session</tt>. It is not intended that
 * this interface ever be exposed to the application. It <b>is</b>
 * intended that users implement this interface to provide
 * custom identifier generation strategies.<br>
 * <br>
 * Implementors should provide a public default constructor.<br>
 * <br>
 * Implementations that accept configuration parameters should
 * also implement <tt>Configurable</tt>.
 * <br>
 * Implementors <em>must</em> be threadsafe
 *
 * @author Gavin King
 * @see PersistentIdentifierGenerator
 * @see Configurable
 */
public interface IdentifierGenerator {

    /**
     * The configuration parameter holding the entity name
     */
    public static final String ENTITY_NAME = "entity_name";
    
	/**
	 * Generate a new identifier.
	 * @param session
	 * @param object the entity or toplevel collection for which the id is being generated
	 *
	 * @return a new identifier
	 * @throws HibernateException
	 */
	public Serializable generate(SessionImplementor session, Object object) 
	throws HibernateException;

}
