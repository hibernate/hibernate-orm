/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

/**
 * Listener for notification of {@link org.hibernate.Session#clear()}
 *
 * @author Steve Ebersole
 */
public interface ClearEventListener extends Serializable {
	/**
	 * Callback for {@link org.hibernate.Session#clear()} notification
	 *
	 * @param event The event representing the clear
	 */
	public void onClear(ClearEvent event);
}
