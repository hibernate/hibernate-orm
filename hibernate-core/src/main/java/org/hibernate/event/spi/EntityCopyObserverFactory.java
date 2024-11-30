/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.service.Service;

@FunctionalInterface
public interface EntityCopyObserverFactory extends Service {
	EntityCopyObserver createEntityCopyObserver();
}
