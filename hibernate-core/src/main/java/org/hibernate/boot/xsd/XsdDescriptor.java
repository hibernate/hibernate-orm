/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
