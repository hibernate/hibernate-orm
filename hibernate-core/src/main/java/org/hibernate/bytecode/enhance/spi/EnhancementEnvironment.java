package org.hibernate.bytecode.enhance.spi;

import java.io.IOException;

/// Resolves class-file resources for an [EnhancementSession] without defining
/// application classes. A resource may come from a class loader, build output,
/// an archive, or another bytecode source. The contract has no dependency on a
/// particular bytecode provider.
///
/// Bytes supplied directly to an enhancement or discovery operation take precedence
/// for that class. Other classes are resolved through this environment. Sessions
/// may cache these resolutions; after replacing resources, the caller must invoke
/// [EnhancementSession#invalidateMetadata()] before using the changed resource view.
/// Changes to persistence mappings require a new session.
///
/// Environments are borrowed by sessions. The owner manages their resource lifetime;
/// closing a session does not close its environment or class loader. Implementations
/// must support concurrent resolution when their sessions are used concurrently.
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
public interface EnhancementEnvironment {

	/// Resolves the bytes of one class without loading or initializing it.
	///
	/// @param className a binary class name, for example `com.acme.Book` or
	///                  `com.acme.Book$Details`
	/// @return the class-file bytes, or null if the resource is absent; the bytes
	///         must remain stable while the enhancement operation consumes them
	/// @throws IOException if resource access fails, as distinct from an absent resource
	byte[] locate(String className) throws IOException;

	/// Creates a resource view using `ClassLoader.getResourceAsStream()`. The loader
	/// is borrowed and is never closed by the returned environment or its sessions.
	/// Each lookup opens and closes its own resource stream.
	///
	/// @param loader the resource loader, or null to use the platform loader's
	///               resource view, which also exposes bootstrap resources
	/// @return an environment resolving binary names as `.class` resource paths
	static EnhancementEnvironment forClassLoader(ClassLoader loader) {
		return className -> {
			final String resource = className.replace( '.', '/' ) + ".class";
			try ( var stream = loader == null
					? ClassLoader.getPlatformClassLoader().getResourceAsStream( resource )
					: loader.getResourceAsStream( resource ) ) {
				return stream == null ? null : stream.readAllBytes();
			}
		};
	}
}
