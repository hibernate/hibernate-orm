package org.hibernate.sql.exec.internal.lock;

import org.hibernate.spi.NavigablePath;

import java.util.Collection;

/// Reusable collector definition; completion state is created only by [#build()].
public class LoadedValuesCollectorFactory {
	private final Collection<NavigablePath> pathsToLock;
	private final boolean deferForceIncrement;

	public LoadedValuesCollectorFactory(Collection<NavigablePath> pathsToLock) {
		this( pathsToLock, false );
	}

	private LoadedValuesCollectorFactory(Collection<NavigablePath> pathsToLock, boolean deferForceIncrement) {
		this.pathsToLock = pathsToLock;
		this.deferForceIncrement = deferForceIncrement;
	}

	public LoadedValuesCollectorFactory withDeferredForceIncrement() {
		return new LoadedValuesCollectorFactory( pathsToLock, true );
	}

	public LoadedValuesCollectorImpl build() {
		return new LoadedValuesCollectorImpl( pathsToLock, deferForceIncrement );
	}
}
