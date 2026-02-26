/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.spi;

import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

import java.util.Map;

/// Access to information useful while performing discovery.  Acts as a
/// "parameter object" for [Scanner#discoverClassNames]
///
/// @author Steve Ebersole
public interface ScanningContext {
	/// Access to all configuration properties defined on the context.
	Map<Object,Object> getProperties();

	/// Access to the [ArchiveDescriptorFactory] defined for the context, providing
	/// the ability to interpret [URLs][java.net.URL] in an abstracted manner.
	ArchiveDescriptorFactory getArchiveDescriptorFactory();
}
