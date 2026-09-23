/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

/// Class responsible for performing enhancement.
///
/// @author Steve Ebersole
/// @author Jason Greene
/// @author Luis Barreiro
public interface Enhancer {

	/// Performs the enhancement.
	/// It is possible to invoke this method concurrently, but when doing so make sure
	/// no two enhancement tasks are invoked on the same class in parallel: the
	/// Enhancer implementations are not required to guard against this.
	///
	/// @param className The name of the class whose bytecode is being enhanced.
	/// @param originalBytes The class's original (pre-enhancement) byte code
	///
	/// @return The enhanced bytecode. If the original bytes are not enhanced, null is returned.
	///
	/// @throws EnhancementException Indicates a problem performing the enhancement
	byte[] enhance(String className, byte[] originalBytes) throws EnhancementException;

	/// Rewrites direct accesses to fields of enhanced managed classes, without
	/// adding managed-class state to the client. Targets must have been discovered
	/// for managed enhancement, or already have compatible enhancement.
	/// Returns null if no instructions change. Supports concurrent calls for
	/// different classes, with the same restriction as [#enhance].
	///
	/// @since 8.0
	default byte[] enhanceClient(String className, byte[] originalBytes) throws EnhancementException {
		throw new EnhancementException( "Client enhancement is not supported by " + getClass().getName() );
	}

	/// Discovers types prior to enhancement.
	/// It is possible to invoke this method concurrently.
	///
	/// @param className The name of the class whose bytecode is being analyzed for type discovery.
	/// @param originalBytes The class's original (pre-enhancement) byte code
	///
	/// @throws EnhancementException Indicates a problem during type discovery
	/// @since 6.3
	void discoverTypes(String className, byte[] originalBytes) throws EnhancementException;
}
