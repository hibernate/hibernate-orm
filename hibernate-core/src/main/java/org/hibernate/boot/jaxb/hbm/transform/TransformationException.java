/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;

/**
 * Generalized error while performing {@code hbm.xml} transformation
 *
 * @author Steve Ebersole
 */
public class TransformationException extends MappingException {
	public TransformationException(String message, Origin origin) {
		super( message, origin );
	}

	public TransformationException(String message, Throwable root, Origin origin) {
		super( message, root, origin );
	}
}
