/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.metamodel.Type;

import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.bytecode.enhance.spi.EnhancementSession;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;

import net.bytebuddy.dynamic.ClassFileLocator;

/// Private model discovery and metadata for a single bytecode environment.
/// Environments and custom locators are borrowed, never closed by this session.
///
/// @author Steve Ebersole
public final class ByteBuddyEnhancementSession implements EnhancementSession {
	final EnhancementModel model;
	final ByteBuddyState byteBuddyState;
	final EnhancerClassLocator typePool;
	final Set<String> candidates = ConcurrentHashMap.newKeySet();
	final Map<String, Type.PersistenceType> discoveredTypes = new ConcurrentHashMap<>();
	final Set<String> completedDiscovery = ConcurrentHashMap.newKeySet();
	final ByteBuddyEnhancementMetadata metadata = new ByteBuddyEnhancementMetadata();
	private volatile boolean closed;

	public ByteBuddyEnhancementSession(EnhancementModel model, EnhancementEnvironment environment, ByteBuddyState state) {
		this(model, state, ModelTypePool.buildModelTypePool(new ClassFileLocator() {
			@Override
			public Resolution locate(String name) throws java.io.IOException {
				final byte[] bytes = environment.locate(name);
				return bytes == null ? new Resolution.Illegal(name) : new Resolution.Explicit(bytes);
			}
			@Override
			public void close() {}
		}));
	}

	public ByteBuddyEnhancementSession(EnhancementModel model, ByteBuddyState state, EnhancerClassLocator locator) {
		this.model = Objects.requireNonNull(model);
		this.byteBuddyState = Objects.requireNonNull(state);
		this.typePool = Objects.requireNonNull(locator);
		candidates.addAll(Set.copyOf(model.getCandidates()));
	}

	@Override
	public Enhancer createEnhancer(EnhancementOptions options) {
		checkOpen();
		return new EnhancerImpl(this, options);
	}

	@Override
	public synchronized void discoverTypes(String className, byte[] bytes) {
		checkOpen();
		final String name = className.replace('/', '.');
		candidates.add(name);
		metadata.beginOperation(bytes != null);
		try {
			if (bytes != null) typePool.registerClassNameAndBytes(name, bytes);
			new ByteBuddyEnhancementContext(this, new EnhancementOptions() {})
					.discoverCompositeTypes(typePool.describe(name).resolve(), typePool, true);
		}
		catch (RuntimeException e) {
			completedDiscovery.clear();
			throw new EnhancementException("Failed to discover types for class " + name, e);
		}
		finally {
			if (bytes != null) typePool.deregisterClassNameAndBytes(name);
			metadata.endOperation();
		}
	}

	void checkOpen() {
		if (closed) throw new IllegalStateException("Enhancement session is closed");
	}

	@Override
	public synchronized void invalidateMetadata() {
		checkOpen();
		typePool.clear();
		metadata.clear();
		completedDiscovery.clear();
	}

	@Override
	public synchronized void close() {
		if (!closed) {
			invalidateMetadata();
			candidates.clear();
			discoveredTypes.clear();
			closed = true;
		}
	}
}
