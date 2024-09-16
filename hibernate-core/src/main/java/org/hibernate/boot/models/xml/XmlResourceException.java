/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml;

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
