/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
/// Phase-oriented binders for creating and completing `org.hibernate.mapping`
/// objects.
///
/// The classes in this package are intentionally small and local to the binding
/// process.  A binder either creates mapping objects for one source-model concept
/// or consumes typed pending state created by an earlier phase.  The phase contracts
/// are declared by [org.hibernate.boot.models.bind.internal.binders.TypeBindingPhase],
/// while the coordinator owns the/ actual ordering.
///
/// Records ending in `Binding` are not final mapping objects.  They are local,
/// typed continuation state.  For example, a member binder may create a
/// `ManyToOne` value before the target entity's members, identifier columns, or
/// table keys are available.  Instead of registering an arbitrary retry callback,
/// the member binder records the exact missing fact in one of these binding
/// records, and a later phase consumes that record deterministically.
///
/// @author Steve Ebersole
package org.hibernate.boot.models.bind.internal.binders;
