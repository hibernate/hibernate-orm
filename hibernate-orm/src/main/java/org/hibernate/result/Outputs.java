/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result;

/**
 * Represents the outputs of executing a JDBC statement accounting for mixing of result sets and update counts
 * hiding the complexity (IMO) of how this is exposed in the JDBC API.
 * <p/>
 * The outputs are exposed as a group of {@link Output} objects, each representing a single result set or update count.
 * Conceptually, Result presents those Returns as an iterator.
 *
 * @author Steve Ebersole
 */
public interface Outputs {
	/**
	 * Retrieve the current Output object.
	 *
	 * @return The current Output object.  Can be {@code null}
	 */
	public Output getCurrent();

	/**
	 * Go to the next Output object (if any), returning an indication of whether there was another (aka, will
	 * the next call to {@link #getCurrent()} return {@code null}?
	 *
	 * @return {@code true} if the next call to {@link #getCurrent()} will return a non-{@code null} value.
	 */
	public boolean goToNext();

	/**
	 * Eagerly release any resources held by this Outputs.
	 */
	public void release();
}
