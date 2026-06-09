package org.hibernate.boot.mapping.internal.xml;

/**
 * Generally indicates a problem locating an XML resource
 *
 * @author Steve Ebersole
 */
public class XmlResourceException extends RuntimeException {
	public XmlResourceException(String message) {
		super( message );
	}

	public XmlResourceException(String message, Throwable cause) {
		super( message, cause );
	}
}
