/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;

/**
 * Thrown when a version number or timestamp check failed, indicating that the Session contained
 * stale data (when using long transactions with versioning). Also occurs if we try delete or update
 * a row that does not exist.
 *
 * Note that this exception often indicates that the user failed to specify the correct
 * {@code unsaved-value} strategy for an entity
 *
 * @author Gavin King
 */
public class StaleStateException extends HibernateException {
	/**
	 * Constructs a StaleStateException using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public StaleStateException(String message) {
		super( message );
	}
}
