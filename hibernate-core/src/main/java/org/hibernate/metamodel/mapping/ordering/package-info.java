/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Internal parsing and translation of collection ordering fragments.
///
/// Providers should use
/// [OrderByFragment][org.hibernate.metamodel.mapping.ordering.spi.OrderByFragment]
/// and must not depend on declarations in this package.
///
/// @see jakarta.persistence.OrderBy
/// @see org.hibernate.annotations.SQLOrder
/// @author Steve Ebersole
@Internal
package org.hibernate.metamodel.mapping.ordering;

import org.hibernate.Internal;
