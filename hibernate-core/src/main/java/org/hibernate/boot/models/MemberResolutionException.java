/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models;

import org.hibernate.MappingException;

/**
 * Indicates a problem resolving a member from {@linkplain org.hibernate.models.spi.ClassDetails}
 *
 * @author Steve Ebersole
 */
public class MemberResolutionException extends MappingException {
	public MemberResolutionException(String message) {
		super( message );
	}

	public MemberResolutionException(String message, Throwable cause) {
		super( message, cause );
	}
}
