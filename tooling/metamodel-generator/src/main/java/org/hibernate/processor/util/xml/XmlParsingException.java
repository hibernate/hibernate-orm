/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util.xml;

/**
 * Custom exception for all sorts of XML parsing related exceptions.
 *
 * @author Hardy Ferentschik
 */
public class XmlParsingException extends Exception {
	public XmlParsingException(String message, Throwable root) {
		super( message, root );
	}
}
