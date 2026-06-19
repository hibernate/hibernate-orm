/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import java.util.Map;

import org.hibernate.annotations.FilterDef;
import org.hibernate.models.spi.ClassDetails;

/// Global registration of a filter definition.
///
/// @param name The filter name
/// @param defaultCondition The default SQL condition for the filter
/// @param parameters Filter parameter names mapped to their Java types
///
/// @see FilterDef
/// @see org.hibernate.boot.jaxb.mapping.JaxbFilterDef
///
/// @since 9.0
/// @author Marco Belladelli
public record FilterDefRegistration(String name, String defaultCondition, Map<String, ClassDetails> parameters) {
}
