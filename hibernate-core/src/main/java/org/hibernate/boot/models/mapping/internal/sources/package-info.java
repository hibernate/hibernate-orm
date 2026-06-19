/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Source-context objects used by mapping binders and materializers.
///
/// The records and helpers in this package adapt categorized source facts,
/// annotations, XML overlays, path adjustments, and binding context into
/// focused inputs for value, component, association, collection, table, and
/// foreign-key binding.  They are deliberately internal: they capture the
/// current binder pipeline's calling context, not a supported source-model API.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.models.mapping.internal.sources;

import org.hibernate.Internal;
