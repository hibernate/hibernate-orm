/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.archive.spi;

import java.util.function.Consumer;

/// Models a logical archive, which might be
///   - a jar file
///   - a zip file
///   - an exploded directory
///   - etc
///
/// Used mainly for scanning purposes via {@linkplain #visitClassEntries visitation}
///
/// @author Steve Ebersole
/// @author Emmanuel Bernard
public interface ArchiveDescriptor {

	/// Visit each entry in the archive which represents a class.
	void visitClassEntries(Consumer<ArchiveEntry> entryConsumer);
}
