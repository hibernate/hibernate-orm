/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import org.hibernate.spi.NavigablePath;

import java.util.Collection;

public class LoadedValuesCollectorFactory {
	private final Collection<NavigablePath> pathsToLock;

	public LoadedValuesCollectorFactory(Collection<NavigablePath> pathsToLock) {
		this.pathsToLock = pathsToLock;
	}

	public LoadedValuesCollectorImpl build() {
		return new LoadedValuesCollectorImpl( pathsToLock );
	}
}
