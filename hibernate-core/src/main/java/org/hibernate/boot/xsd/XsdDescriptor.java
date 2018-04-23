/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.xsd;

import javax.xml.validation.Schema;

/**
 * Representation of a locally resolved XSD
 *
 * @author Steve Ebersole
 */
public final class XsdDescriptor {
	private final String localResourceName;
	private final String namespaceUri;
	private final String version;
	private final Schema schema;

	XsdDescriptor(String localResourceName, Schema schema, String version, String namespaceUri) {
		this.localResourceName = localResourceName;
		this.schema = schema;
		this.version = version;
		this.namespaceUri = namespaceUri;
	}

	public String getLocalResourceName() {
		return localResourceName;
	}

	public String getNamespaceUri() {
		return namespaceUri;
	}

	public String getVersion() {
		return version;
	}

	public Schema getSchema() {
		return schema;
	}
}
