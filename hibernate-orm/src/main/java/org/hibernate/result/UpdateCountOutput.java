/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result;

/**
 * Models a return that is an update count (count of rows affected)
 *
 * @author Steve Ebersole
 */
public interface UpdateCountOutput extends Output {
	/**
	 * Retrieve the associated update count
	 *
	 * @return The update count
	 */
	public int getUpdateCount();
}
