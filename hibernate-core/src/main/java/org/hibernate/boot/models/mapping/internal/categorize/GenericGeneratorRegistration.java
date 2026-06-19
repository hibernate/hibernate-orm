/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;

import java.util.Map;

/// Global registration of a Hibernate generic identifier generator.
///
/// @param name The generator name
/// @param strategy The generator strategy class name
/// @param parameters The generator parameters
///
/// @see org.hibernate.annotations.GenericGenerator
/// @see JaxbGenericIdGeneratorImpl
///
/// @since 9.0
/// @author Steve Ebersole
public record GenericGeneratorRegistration(String name, String strategy, Map<String, String> parameters) {
}
