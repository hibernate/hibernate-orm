/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.result;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class NoMoreOutputsException extends HibernateException {
	public NoMoreOutputsException(String message) {
		super( message );
	}

	public NoMoreOutputsException() {
		super( "Outputs have been exhausted" );
	}
}
