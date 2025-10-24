/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.Incubating;

import java.util.function.Supplier;

/**
 * Marker interface for extensions to register themselves within a session instance.
 *
 * @see SharedSessionContractImplementor#getExtensionStorage(Class, Supplier)
 */
@Incubating
public interface ExtensionStorage {
}
