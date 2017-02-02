/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

import org.hibernate.HibernateError;

/**
 * Indicates a problem walking the domain tree.  Almost always this indicates an internal error in Hibernate
 *
 * @author Steve Ebersole
 */
public class WalkingException extends HibernateError {
	public WalkingException(String message) {
		super( message );
	}

	public WalkingException(String message, Throwable root) {
		super( message, root );
	}
}
