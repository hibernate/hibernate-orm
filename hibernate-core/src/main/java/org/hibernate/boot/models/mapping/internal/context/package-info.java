/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
/// Internal implementation of the Hibernate Models based mapping pipeline.
///
/// This package is intentionally not a public extension contract.  It is still
/// documented because the code here carries the main design experiment of this
/// module: move available Hibernate Models resources through explicit phases
/// using typed state instead of generic second-pass callbacks.
///
/// The high-level flow is:
///
/// 1. Available resources are collected from persistence-unit inputs.
/// 2. XML and annotation sources are normalized.
/// 3. Categorized model metadata describes managed types and attributes.
/// 4. The horizontal binding model records the boot-time semantic
///    interpretation.
/// 5. Views expose stable read shapes over that model.
/// 6. Materializers create the mapping structures consumed by later boot and
///    runtime code.
/// 7. Cross-phase work is recorded in strongly typed state objects held by
///    [org.hibernate.boot.models.mapping.internal.context.BindingState].
///
/// The surrounding [org.hibernate.boot.models.mapping] package family names the
/// larger "available resources to mapping model" process.  This package is just
/// the shared implementation context for that process, not the home for phase
/// specific binders, views, materializers, or relational table references.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.models.mapping.internal.context;

import org.hibernate.Internal;
