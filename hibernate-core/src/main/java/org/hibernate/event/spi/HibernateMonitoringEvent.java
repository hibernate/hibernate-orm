/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.event.monitor.spi.DiagnosticEvent;

/**
 * @deprecated Renamed {@link DiagnosticEvent}.
 */
@Deprecated(since = "7", forRemoval = true)
public interface HibernateMonitoringEvent extends DiagnosticEvent {

}
