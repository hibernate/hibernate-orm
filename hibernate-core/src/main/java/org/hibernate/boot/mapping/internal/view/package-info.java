/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
/// Stable read views over the horizontal boot binding model.
///
/// Views are the handoff between mutable binding state and later consumers.
/// They present finalized or intentionally projected facts such as managed-type
/// closures, identifier extraction shapes, selectable ordering, and contribution
/// ownership without requiring consumers to know how the mutable model was
/// populated.
///
/// In the eventual `org.hibernate.boot.mapping` pipeline, this package is
/// the phase after model population/resolution and before materialization.  That
/// phase boundary is important: readers should ask views for resolved semantics
/// instead of inferring them from worker binders or from incidental collection
/// ordering inside mutable state.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.mapping.internal.view;

import org.hibernate.Internal;
