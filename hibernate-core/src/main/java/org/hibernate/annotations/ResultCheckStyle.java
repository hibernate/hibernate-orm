/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Possible styles of checking return codes on SQL INSERT, UPDATE and DELETE queries.
 *
 * @author L�szl� Benke
 */
public enum ResultCheckStyle {
	/**
	 * Do not perform checking.  Might mean that the user really just does not want any checking.  Might
	 * also mean that the user is expecting a failure to be indicated by a {@link java.sql.SQLException} being
	 * thrown (presumably from a {@link java.sql.CallableStatement} which is performing explicit checks and
	 * propagating failures back through the driver).
	 */
	NONE,
	/**
	 * Perform row-count checking.  Row counts are the int values returned by both
	 * {@link java.sql.PreparedStatement#executeUpdate()} and
	 * {@link java.sql.Statement#executeBatch()}.  These values are checked
	 * against some expected count.
	 */
	COUNT,
	/**
	 * Essentially the same as {@link #COUNT} except that the row count actually
	 * comes from an output parameter registered as part of a
	 * {@link java.sql.CallableStatement}.  This style explicitly prohibits
	 * statement batching from being used...
	 */
	PARAM
}
