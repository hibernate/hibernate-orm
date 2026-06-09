/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
/// Internal implementation of the Hibernate Models based mapping binder.
///
/// This package is intentionally not a public extension contract.  It is still
/// documented because the code here carries the main design experiment of this
/// module: build `org.hibernate.mapping` structures from Hibernate Models source
/// details using explicit, typed binding state instead of generic second-pass
/// callbacks.
///
/// The high-level flow is:
///
/// 1. Categorized model metadata describes managed types and attributes.
/// 2. Type binders create stable mapping-model shells for entities and
///    mapped superclasses.
/// 3. Later phases fill in identifiers, attributes, table keys, association
///    targets, inverse associations, and physical foreign keys.
/// 4. Cross-phase work is recorded in strongly typed state objects held by
///    [org.hibernate.boot.models.bind.spi.BindingState].
///
/// The implementation is deliberately close to `org.hibernate.mapping` so it can
/// expose which pieces of source-model information the mapping model may want to
/// retain directly once legacy boot sources are no longer a constraint.
///
/// @author Steve Ebersole
package org.hibernate.boot.models.bind.internal;
