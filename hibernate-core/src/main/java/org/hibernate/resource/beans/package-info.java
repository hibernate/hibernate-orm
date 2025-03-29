/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines Hibernate's integration with CDI. Because CDI may or may not be available
 * a lot of this support is directed toward abstracting/encapsulating CDI. The central
 * contracts here from a client point of view are
 * {@link org.hibernate.resource.beans.spi.ManagedBean} and
 * {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry} which may or may not
 * really be backed by CDI.
 */
package org.hibernate.resource.beans;
