/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import org.hibernate.boot.jaxb.Origin;

/**
 * Indicates a problem parsing a mapping document.
 *
 * @author Steve Ebersole
 */
public class MappingException extends org.hibernate.MappingException {
	private final Origin origin;

	public MappingException(String message, Origin origin) {
		super( message );
		this.origin = origin;
	}

	public MappingException(String message, Throwable root, Origin origin) {
		super( message, root );
		this.origin = origin;
	}

	@Override
	public String getMessage() {
		String message = super.getMessage();
		if (origin != null) {
			message += " : origin(" + origin.getName() + ")";
		}
		return message;
	}

	public Origin getOrigin() {
		return origin;
	}
}
