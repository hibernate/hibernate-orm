/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Hibernate's built-in dialect-specific JDBC type implementations.
///
/// Provider Dialects must obtain reusable stock descriptors through the
/// factories in [org.hibernate.dialect.type.spi] and must not depend on this
/// package.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.dialect.type.internal;

import org.hibernate.Internal;
