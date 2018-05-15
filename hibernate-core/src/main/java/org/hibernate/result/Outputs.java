/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result;

/**
 * Group of mixed Output references representing outcomes of the individual statements executed
 * (procedure, anonymous procedure block, etc).  Each Output may be either a {@link ResultSetOutput}
 * or {@link UpdateCountOutput}.  This allows consuming the results directly from JDBC's
 * mixed result-set/update-count while being easier to work with.
 *
 * @author Steve Ebersole
 */
public interface Outputs {
	/**
	 * Retrieve the current Output object.
	 *
	 * @return The current Output object.  Can be {@code null}
	 */
	Output getCurrent();

	/**
	 * Go to the next Output object (if any), returning an indication of whether there was another (aka, will
	 * the next call to {@link #getCurrent()} return {@code null}?
	 *
	 * @return {@code true} if the next call to {@link #getCurrent()} will return a non-{@code null} value.
	 */
	boolean goToNext();

	/**
	 * Eagerly release any resources held by this Outputs.
	 */
	void release();
}
