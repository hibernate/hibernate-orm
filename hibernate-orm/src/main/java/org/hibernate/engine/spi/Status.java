/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

/**
 * Represents the status of an entity with respect to
 * this session. These statuses are for internal
 * book-keeping only and are not intended to represent
 * any notion that is visible to the _application_.
 */
public enum Status {
	MANAGED,
	READ_ONLY,
	DELETED,
	GONE,
	LOADING,
	SAVING
}
