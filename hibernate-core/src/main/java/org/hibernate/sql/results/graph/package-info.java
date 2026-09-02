/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Defines the provider-facing graph used to turn JDBC rows into domain results.
///
/// Providers may consume the standard graph nodes and implement the interfaces
/// classified for `IMPLEMENT`. Graph values are scoped to result creation or
/// processing and must not be retained beyond that lifecycle.
///
/// @see org.hibernate.sql.results.graph.DomainResult
/// @see org.hibernate.sql.results.graph.Fetch
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(SPI.Role.USE)
package org.hibernate.sql.results.graph;

import org.hibernate.Incubating;
import org.hibernate.SPI;
