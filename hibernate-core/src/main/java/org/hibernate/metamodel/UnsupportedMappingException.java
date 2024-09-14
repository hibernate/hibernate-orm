/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.NonTransientException;

/**
 * Indicated a problem with a mapping.  Usually this is a problem with a combination
 * of mapping constructs.
 */
public class UnsupportedMappingException extends HibernateException implements NonTransientException {
	public UnsupportedMappingException(String message) {
		super( message );
	}
}
