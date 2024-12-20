/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
