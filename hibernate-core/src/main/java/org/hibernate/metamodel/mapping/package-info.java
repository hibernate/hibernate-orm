/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Defines the runtime mapping metamodel describing how domain-model parts map
/// to relational database objects.
///
/// This package is a provider-use SPI. Providers may inspect these immutable
/// runtime views while contributing types, SQL translation, or result handling.
/// A type is a supported provider implementation contract only when it declares
/// the `IMPLEMENT` role explicitly.
///
/// Do not retain bootstrap-scoped mapping values or depend on implementations
/// from the `internal` subpackage.
///
/// @implNote Built on top of the `org.hibernate.persister` package.
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(SPI.Role.USE)
package org.hibernate.metamodel.mapping;

import org.hibernate.Incubating;
import org.hibernate.SPI;
