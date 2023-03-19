/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.service.JavaServiceLoadable;

/**
 * Integration contract for contributing event types and listeners to the Hibernate event system.
 *
 * Discoverable via Java's service loading mechanism ({@link java.util.ServiceLoader})
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface EventEngineContributor {
	/**
	 * Apply the contributions
	 */
	void contribute(EventEngineContributions target);
}
