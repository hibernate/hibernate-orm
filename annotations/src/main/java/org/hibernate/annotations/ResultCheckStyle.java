//$Id:
package org.hibernate.annotations;

/**
 * Possible checks on Sql Insert, Delete, Update
 *
 * @author László Benke
 */
public enum ResultCheckStyle {
	/**
	 * Do not perform checking.  Either user simply does not want checking, or is
	 * indicating a {@link java.sql.CallableStatement} execution in which the
	 * checks are being performed explicitly and failures are handled through
	 * propogation of {@link java.sql.SQLException}s.
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
