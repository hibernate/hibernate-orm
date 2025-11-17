/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * An SPI for services which collect, report, or monitor diagnostic events.
 * For example, this SPI is implemented by Hibernate JFR to report events
 * to Java Flight Recorder.
 *
 * @see org.hibernate.event.monitor.spi.EventMonitor
 *
 * @since 7
 */
package org.hibernate.event.monitor.spi;
