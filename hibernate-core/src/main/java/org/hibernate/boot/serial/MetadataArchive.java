/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial;

import java.io.OutputStream;

import org.hibernate.Incubating;
import org.hibernate.boot.registry.StandardServiceRegistry;

/// Opaque, data-only archive of a factory-ready Hibernate ORM boot model.
///
/// Reading or writing an archive does not load application classes. Restoration
/// is explicit and uses the supplied ORM service environment.
///
/// An archive is a trusted build artifact, not a safe interchange format for
/// untrusted network or user input. Producer and consumer must use the same
/// Hibernate ORM version and archive format. No forward or backward
/// compatibility across ORM versions is implied.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface MetadataArchive {
	/// Writes this archive to `output`, flushing but not closing the stream.
	///
	/// @throws MetadataSerializationException if the archive cannot be written
	void writeTo(OutputStream output);

	/// Restores a factory-ready metadata product in the supplied service environment.
	///
	/// @throws IllegalArgumentException if `serviceRegistry` is `null`
	/// @throws MetadataSerializationException if the archive is corrupt, its
	/// producer or mapping environment is incompatible, or a declarative
	/// strategy cannot be reconstructed
	RestoredMetadata restore(StandardServiceRegistry serviceRegistry);
}
