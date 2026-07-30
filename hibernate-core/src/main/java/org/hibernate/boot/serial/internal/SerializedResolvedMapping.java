/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serial;
import java.io.Serializable;

/// Root object of the ORM metadata payload.
///
/// @since 9.0
/// @author Steve Ebersole
public record SerializedResolvedMapping(
		MetadataState metadataState,
		MappingResolutionSnapshot mappingResolutionSnapshot,
		RuntimeMappingHandoffSnapshot runtimeMappingHandoffSnapshot) implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
}
