/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;

/**
 * Indicates an internal attempt to mark a column as non-nullable (because its part
 * of a PK, etc) but we cannot force that column to be non-nullable.
 * <p/>
 * Typically this indicates that the "column" is actually a formula.
 *
 * @author Steve Ebersole
 */
public class CannotForceNonNullableException extends AnnotationException {
	public CannotForceNonNullableException(String message) {
		super( message );
	}
}
