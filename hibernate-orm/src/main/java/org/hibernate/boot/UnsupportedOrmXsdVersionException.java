/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import org.hibernate.boot.jaxb.Origin;

/**
 * @author Steve Ebersole
 */
public class UnsupportedOrmXsdVersionException extends MappingException {
	private final String requestedVersion;

	public UnsupportedOrmXsdVersionException(String requestedVersion, Origin origin) {
		super( "Encountered unsupported orm.xml xsd version [" + requestedVersion + "]", origin );
		this.requestedVersion = requestedVersion;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getRequestedVersion() {
		return requestedVersion;
	}
}
