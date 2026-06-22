/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.Map;

import org.hibernate.annotations.FilterDef;
import org.hibernate.models.spi.ClassDetails;

/// Global registration of a filter definition.
///
/// @param name The filter name
/// @param defaultCondition The default SQL condition for the filter
/// @param autoEnabled Whether the filter should be enabled by default
/// @param applyToLoadByKey Whether the filter should apply to direct key lookups
/// @param parameterTypes Filter parameter names mapped to their Java types
/// @param parameterResolvers Filter parameter names mapped to their resolver types
///
/// @see FilterDef
/// @see org.hibernate.boot.jaxb.mapping.JaxbFilterDef
///
/// @since 9.0
/// @author Marco Belladelli
public record FilterDefRegistration(
		String name,
		String defaultCondition,
		boolean autoEnabled,
		boolean applyToLoadByKey,
		Map<String, ClassDetails> parameterTypes,
		Map<String, ClassDetails> parameterResolvers) {
}
