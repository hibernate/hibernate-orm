/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import org.hibernate.boot.jaxb.Origin;

/**
 * @author Brett Meyer
 */
public class InvalidMappingException extends org.hibernate.InvalidMappingException {
	private final Origin origin;

	public InvalidMappingException(Origin origin) {
		super(
				String.format( "Could not parse mapping document: %s (%s)", origin.getName(), origin.getType() ),
				origin
		);
		this.origin = origin;
	}

	public InvalidMappingException(Origin origin, Throwable e) {
		super(
				String.format( "Could not parse mapping document: %s (%s)", origin.getName(), origin.getType() ),
				origin.getType().getLegacyTypeText(),
				origin.getName(),
				e
		);
		this.origin = origin;
	}

	public Origin getOrigin() {
		return origin;
	}
}
