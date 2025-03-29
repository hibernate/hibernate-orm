/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.service.Service;

@FunctionalInterface
public interface EntityCopyObserverFactory extends Service {
	EntityCopyObserver createEntityCopyObserver();
}
