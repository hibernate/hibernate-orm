/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.util.xml;

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


