/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.serial.internal.MappingResolutionDetailsCollector;
import org.hibernate.metamodel.internal.RuntimeMappingHandoff;

/// Factory-ready ORM boot product containing the finalized mapping graph, the
/// optional serialization capture state and its immutable runtime handoff.
///
/// @since 9.0
/// @author Steve Ebersole
public record ResolvedMapping(
		MetadataImplementor metadata,
		MappingResolutionDetailsCollector mappingResolutionDetailsCollector,
		RuntimeMappingHandoff runtimeMappingHandoff) {
}
