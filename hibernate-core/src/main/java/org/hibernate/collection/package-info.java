/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package defines a framework for lazy-initializing and state-tracking
 * collection wrappers.
 * <p>
 * The interface {@link org.hibernate.collection.spi.PersistentCollection}
 * and all its implementations belong to an SPI. They are not part of the
 * public API of Hibernate, and are not meant to be used directly by typical
 * programs which use Hibernate for persistence.
 *
 * @see org.hibernate.proxy
 */
package org.hibernate.collection;
