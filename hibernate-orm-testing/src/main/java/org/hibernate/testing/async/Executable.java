/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.async;

/**
 * Something we want to perform with protection to time it out.
 *
 * @author Steve Ebersole
 */
public interface Executable {
	/**
	 * Perform the action
	 */
	public void execute();

	/**
	 * Called when the timeout period is exceeded.
	 */
	public void timedOut();
}
