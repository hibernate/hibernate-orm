/*
 * SPDX-License-Identifier: Apache-2.0
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
