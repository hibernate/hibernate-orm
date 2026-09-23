/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

/// Owns discovery and metadata for one model and bytecode environment.
/// Close and invalidation require that no operation is in progress.
/// Closing a session invalidates its enhancers but does not close borrowed resources.
///
/// @since 8.0
/// @author Steve Ebersole
public interface EnhancementSession extends AutoCloseable {

	/// Creates an enhancer with its own generation options, using this session's
	/// model and metadata. The options must remain stable while the enhancer is used.
	///
	/// @param options generation choices independent of the persistence model
	/// @return an enhancer whose lifetime is bounded by this session
	/// @throws IllegalStateException if this session has been closed
	Enhancer createEnhancer(EnhancementOptions options);

	/// Schedules a managed class and discovers its persistence types, including
	/// embedded types and mapped superclasses. This does not generate enhanced bytes.
	///
	/// @param className the class name in binary or JVM internal form
	/// @param originalBytes class-file bytes, or null to resolve them through the environment
	/// @throws EnhancementException if discovery fails
	/// @throws IllegalStateException if this session has been closed
	void discoverTypes(String className, byte[] originalBytes);

	/// Discards cached bytecode descriptions and getter metadata after resources
	/// change, for example between managed and client build-time passes. Candidate
	/// names and discovered persistence classifications remain available. Replacing
	/// persistence mappings requires a new session rather than cache invalidation.
	///
	/// No session operation may be in progress when this method is called.
	///
	/// @throws IllegalStateException if this session has been closed
	void invalidateMetadata();

	/// Releases session-owned metadata and invalidates all enhancers created by
	/// this session. Does not close borrowed environments, locators, or class loaders.
	/// Repeated calls have no effect. No session operation may be in progress.
	@Override
	void close();
}
