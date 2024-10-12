/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.spi;

import org.hibernate.Incubating;
import org.hibernate.ObjectNotFoundException;

/**
 * Utility methods used by Hibernate Processor.
 *
 * @author Gavin King
 *
 * @since 6.5
 */
@Incubating
public class Exceptions {
	public static <E> E require(E entity, String entityName, Object id) {
		if ( entity == null ) {
			throw new ObjectNotFoundException( entityName, id );
		}
		else {
			return entity;
		}
	}

	public static void require(Object argument, String parameterName) {
		if ( argument == null ) {
			throw new IllegalArgumentException("Null " + parameterName);
		}
	}
}
