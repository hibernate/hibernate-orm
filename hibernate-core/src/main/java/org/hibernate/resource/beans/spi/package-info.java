/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines an SPI for integration with CDI-like containers.
 * <p>
 * Because CDI may or may not be available, much of this support
 * is directed toward abstracting/encapsulating CDI.
 * <p>
 * The central contracts here from a client point of view are
 * {@link org.hibernate.resource.beans.spi.ManagedBean} and
 * {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry},
 * which may or may not really be backed by CDI.
 */
package org.hibernate.resource.beans.spi;
