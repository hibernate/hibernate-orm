/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
