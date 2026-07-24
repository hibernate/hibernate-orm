/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
/// Phase-oriented worker classes for populating and completing boot binding
/// state.
///
/// The classes in this package are intentionally small and local to the binding
/// process.  A binder either interprets one source-model concept into binding
/// state or consumes typed pending state created by an earlier phase.  The phase
/// contracts are declared by
/// [org.hibernate.boot.mapping.internal.binders.TypeBindingPhase], while the
/// coordinator owns the actual ordering.
///
/// This package is the worker/action phase in the larger boot-model mapping
/// process.  It sits after source normalization and categorization, and before
/// stable views and materialization.  Records ending in `Binding` are local,
/// typed continuation state used to carry unresolved facts from one binder phase
/// to another.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.Internal;
