/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.service.Service;

/**
 * Envers service contract that participates in the {@code StandardServiceRegistry}.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public interface EnversService extends Service {
	/**
	 * Is the Envers integration enabled?
	 * <p/>
	 * This is generally used as a protection for other Envers services (in the ServiceLoader sense)
	 * determine whether they should do their work.
	 *
	 * @return {@code true} If the integration is enabled; {@code false} otherwise.
	 */
	boolean isEnabled();
}
