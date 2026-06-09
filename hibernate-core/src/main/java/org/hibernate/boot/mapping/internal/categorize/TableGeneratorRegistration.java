/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.persistence.TableGenerator;

/// Global registration of a table-based identifier generator.
///
/// @param name The generator name
/// @param configuration The generator configuration
///
/// @since 9.0
/// @author Steve Ebersole
public record TableGeneratorRegistration(String name, TableGenerator configuration) {
}
