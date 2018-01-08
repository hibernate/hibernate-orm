/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package for testing Hibernate's support for integrating
 * with CDI for beans whose lifecycle is  not managed by the
 * {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry}
 * (i.e. beans retrieved with shouldRegistryManageLifecycle = false).
 */
package org.hibernate.test.cdi.nonregistrymanaged;
