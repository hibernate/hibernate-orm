/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Relational table-reference model used while binding mappings.
///
/// This package contains the small binding-time model for relational table
/// expressions.  A table reference preserves the source logical name, physical
/// naming result, catalog/schema information, exportability, and the
/// `org.hibernate.mapping.Table` shell created for later boot phases.
///
/// These objects are not public extension contracts.  They are binding-model
/// support objects used by table, value, association, and key binders to talk
/// about tables consistently before primary keys, foreign keys, and runtime
/// mappings are fully resolved.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.models.mapping.internal.relational;

import org.hibernate.Internal;
