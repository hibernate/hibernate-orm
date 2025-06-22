/*
 * SPDX-License-Identifier: Apache-2.0
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
