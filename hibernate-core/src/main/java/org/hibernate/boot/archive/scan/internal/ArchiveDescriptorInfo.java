/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;

/**
 * @author Steve Ebersole
 */ // This needs to be protected and attributes/constructor visible in case
// a custom scanner needs to override validateReuse.
public class ArchiveDescriptorInfo {
	public final ArchiveDescriptor archiveDescriptor;
	public final boolean isRoot;

	public ArchiveDescriptorInfo(ArchiveDescriptor archiveDescriptor, boolean isRoot) {
		this.archiveDescriptor = archiveDescriptor;
		this.isRoot = isRoot;
	}
}
