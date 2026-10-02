package org.hibernate.internal.util.config;
import org.hibernate.HibernateException;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ConfigurationException extends HibernateException {
	public ConfigurationException(String string, Throwable root) {
		super( string, root );
	}

	public ConfigurationException(String s) {
		super( s );
	}
}
