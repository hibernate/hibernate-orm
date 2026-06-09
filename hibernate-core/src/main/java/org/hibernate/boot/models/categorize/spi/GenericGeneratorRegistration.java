/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;

/// Global registration of a Hibernate generic identifier generator.
///
/// @param name The generator name
/// @param configuration The generator configuration
///
/// @see GenericGenerator
/// @see JaxbGenericIdGeneratorImpl
///
/// @since 9.0
/// @author Steve Ebersole
public record GenericGeneratorRegistration(String name, GenericGenerator configuration) {
}
