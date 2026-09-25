package org.hibernate.type;
import org.hibernate.HibernateException;

/**
 * Thrown when a property cannot be serialized/deserialized
 * @author Gavin King
 */
public class SerializationException extends HibernateException {

	public SerializationException(String message, Exception root) {
		super(message, root);
	}

}
