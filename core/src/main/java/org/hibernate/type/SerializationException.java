//$Id: SerializationException.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.type;

import org.hibernate.HibernateException;

/**
 * Thrown when a property cannot be serializaed/deserialized
 * @author Gavin King
 */
public class SerializationException extends HibernateException {

	public SerializationException(String message, Exception root) {
		super(message, root);
	}

}






