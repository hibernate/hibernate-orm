/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.event.monitor.spi.EventMonitor;

/**
 * @deprecated Renamed {@link EventMonitor}.
 */
@Deprecated(since = "7", forRemoval = true)
public interface EventManager extends EventMonitor {

}
