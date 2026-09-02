/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Defines the root contracts and built-in implementations for SQL dialects.
///
/// This exact package is a provider-use SPI. Providers may select and construct
/// its public Dialect implementations and use its public supporting vocabulary.
/// The package classification is not recursive: feature subpackages declare
/// their own API, SPI, or internal classification.
///
/// A concrete Dialect being selectable, whether explicitly through
/// {@value org.hibernate.cfg.AvailableSettings#DIALECT} or automatically through
/// Dialect resolution, does not make it a supported subclass base. Providers
/// should extend {@link org.hibernate.dialect.Dialect} or one of the types
/// directly classified for SPI implementation. Only directly classified types
/// and members promise stronger implementation or supply roles.
///
/// @see org.hibernate.dialect.Dialect
/// @since 8.0
@SPI(SPI.Role.USE)
package org.hibernate.dialect;

import org.hibernate.SPI;
