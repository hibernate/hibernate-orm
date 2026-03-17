/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.spi;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URL;
import java.util.function.Consumer;

/// Models a logical archive, which might be
///   - a jar file
///   - a zip file
///   - an exploded directory
///   - etc
///
/// Used mainly for scanning purposes via [#visitClassEntries]
/// and locating "well known" resources via [#findEntry]
///
/// @author Steve Ebersole
/// @author Emmanuel Bernard
public interface ArchiveDescriptor {
	/// The URL which is the base of this archive.
	URL getUrl();

	/// Visit each entry in the archive which represents a class.
	void visitClassEntries(Consumer<ArchiveEntry> entryConsumer);

	/// Resolve the given path relative to this archive.
	///
	/// @implNote Typically used to find `META-INF/persistence.xml` and `META-INF/orm.xml` files.
	@Nullable
	ArchiveEntry findEntry(String relativePath);

	/// Resolve a named archive relative to `this` archive.
	/// Generally the given name comes from the `META-INF/persistence.xml` within this archive
	/// using `<jar-file/>`.
	///
	/// @param jarFileReference The name given in `persistence.xml` via `<jar-file/>`
	@NonNull ArchiveDescriptor resolveJarFileReference(@NonNull String jarFileReference);
}
