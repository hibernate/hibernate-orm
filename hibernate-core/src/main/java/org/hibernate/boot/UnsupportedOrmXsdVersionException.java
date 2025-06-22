/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import org.hibernate.boot.jaxb.Origin;

/**
 * Specialized exception indicating that an unsupported {@code orm.xml} XSD version was specified
 *
 * @author Steve Ebersole
 */
public class UnsupportedOrmXsdVersionException extends MappingException {
	private final String requestedVersion;

	public UnsupportedOrmXsdVersionException(String requestedVersion, Origin origin) {
		super( "Encountered unsupported orm.xml xsd version [" + requestedVersion + "]", origin );
		this.requestedVersion = requestedVersion;
	}

	public String getRequestedVersion() {
		return requestedVersion;
	}
}
