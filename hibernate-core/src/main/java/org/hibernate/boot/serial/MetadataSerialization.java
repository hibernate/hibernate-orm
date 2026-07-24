/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial;

import java.io.InputStream;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.serial.internal.MetadataArchiveImpl;

/// Creates and reads the explicit, factory-ready serial form of resolved boot [Metadata].
///
/// A producer must opt in while configuring bootstrap, and may then archive
/// metadata after the usual boot-model building phase:
///
/// ```java
/// StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
///         .applySetting(MappingSettings.METADATA_SERIALIZATION_ENABLED, true)
///         .build();
///
/// Metadata metadata = new MetadataSources(serviceRegistry)
///         .addAnnotatedClass(MyEntity.class)
///         .buildMetadata();
///
/// MetadataArchive archive = MetadataSerialization.serialize(metadata);
/// try (OutputStream output = Files.newOutputStream(metadataFile)) {
///     archive.writeTo(output);
/// }
/// ```
///
/// A consumer reads the data-only representation and explicitly restores the
/// live metadata graph in its own environment:
///
/// ```java
/// MetadataArchive archive;
/// try (InputStream input = Files.newInputStream(metadataFile)) {
///     archive = MetadataSerialization.read(input);
/// }
///
/// RestoredMetadata restored = archive.restore(serviceRegistry);
/// PersistentClass book = restored.getMetadata().getEntityBinding("com.acme.Book");
/// SessionFactory sessionFactory = restored.buildSessionFactory();
/// ```

/// Basic-value resolutions and runtime metamodel handoffs are rebuilt before
/// [MetadataArchive#restore(org.hibernate.boot.registry.StandardServiceRegistry)]
/// returns, allowing its [RestoredMetadata] result to build a SessionFactory
/// directly.
///
/// Service-loaded contributors, including
/// [org.hibernate.boot.model.FunctionContributor], run again while the archive
/// is restored. Explicitly supplied [org.hibernate.query.sqm.function.SqmFunctionDescriptor]
/// instances have no general declarative form and therefore make an archive
/// ineligible for serialization.
///
/// Reading and restoration are deliberately separate operations.  In
/// particular, [#read(InputStream)] does not load application classes or
/// recreate the live metadata graph. See [MetadataArchive] for restoration.
///
/// Archives are trusted build artifacts. Since the representation contains
/// Java-serialized Hibernate and Hibernate Models state, [#read(InputStream)]
/// must not be used with data received from an untrusted source. Archives are
/// accepted only from the exact same Hibernate ORM version and archive format.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public final class MetadataSerialization {
	private MetadataSerialization() {
	}

	/// Creates the data-only serial form of the given boot metadata.
	///
	/// @param metadata metadata produced by Hibernate ORM
	///
	/// @return the opaque archive, ready to be [written][MetadataArchive#writeTo]
	///
	/// @throws IllegalArgumentException if `metadata` is not a factory-ready resolved mapping
	/// @throws IllegalStateException if metadata serialization was not enabled while it was built
	/// @throws MetadataSerializationException if the metadata graph could not be serialized
	public static MetadataArchive serialize(Metadata metadata) {
		return MetadataArchiveImpl.from( metadata );
	}

	/// Reads a data-only serialized metadata archive from the given stream.
	///
	/// This operation validates and reconstructs the serial form, but does not
	/// load application classes or restore live [Metadata]. Call
	/// [MetadataArchive#restore(org.hibernate.boot.registry.StandardServiceRegistry)]
	/// when the restoration environment is available.  The stream is not closed.
	/// The input must be a trusted build artifact.
	///
	/// @param stream the source stream
	///
	/// @return the data-only serial form
	///
	/// @throws MetadataSerializationException if the archive could not be read or is invalid
	public static MetadataArchive read(InputStream stream) {
		return MetadataArchiveImpl.readFrom( stream );
	}
}
