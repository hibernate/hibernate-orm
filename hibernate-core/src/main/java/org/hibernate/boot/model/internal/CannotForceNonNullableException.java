/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;

/**
 * Indicates an internal attempt to mark a column as non-nullable
 * (because it's part of a primary key, for example) when we cannot
 * force that column to be non-nullable. This usually indicates that
 * the "column" is actually a formula.
 *
 * @author Steve Ebersole
 */
public class CannotForceNonNullableException extends AnnotationException {
	public CannotForceNonNullableException(String message) {
		super( message );
	}
}
