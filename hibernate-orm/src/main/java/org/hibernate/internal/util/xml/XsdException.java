/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.internal.util.xml;

import org.hibernate.HibernateException;

/**
 * Indicates an issue finding or loading an XSD schema.
 * 
 * @author Steve Ebersole
 */
public class XsdException extends HibernateException {
	private final String xsdName;

	public XsdException(String message, String xsdName) {
		super( message );
		this.xsdName = xsdName;
	}

	public XsdException(String message, Throwable root, String xsdName) {
		super( message, root );
		this.xsdName = xsdName;
	}

	public String getXsdName() {
		return xsdName;
	}
}
