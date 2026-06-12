/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import jakarta.persistence.SequenceGenerator;

/// Global registration of a sequence-based identifier generator.
///
/// @param name The generator name
/// @param configuration The generator configuration
///
/// @since 9.0
/// @author Steve Ebersole
public record SequenceGeneratorRegistration(String name, SequenceGenerator configuration) {
}
