/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

/**
 * Allows reaction to basic {@link SessionFactory} occurrences.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryObserver extends Serializable {
	/**
	 * Callback to indicate that the given factory has been created and is now ready for use.
	 *
	 * @param factory The factory initialized.
	 */
	public void sessionFactoryCreated(SessionFactory factory);

	/**
	 * Callback to indicate that the given factory has been closed.  Care should be taken
	 * in how (if at all) the passed factory reference is used since it is closed.
	 *
	 * @param factory The factory closed.
	 */
	public void sessionFactoryClosed(SessionFactory factory);
}
