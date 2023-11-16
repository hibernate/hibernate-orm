/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.query.sqm;

import org.hibernate.HibernateException;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Indicates a problem with a TREAT usage
 *
 * @author Steve Ebersole
 */
public class TreatException extends HibernateException {
	public TreatException(String message) {
		super( message );
	}

	public TreatException(String message, @Nullable Throwable cause) {
		super( message, cause );
	}
}
