/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Provider contracts for identity-column DDL, insert syntax, and
/// identity-specific value retrieval.
///
/// @since 8.0
/// @author Steve Ebersole
@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
package org.hibernate.dialect.identity.spi;
