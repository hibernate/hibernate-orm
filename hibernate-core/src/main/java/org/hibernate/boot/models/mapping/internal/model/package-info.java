/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
/// Horizontal boot binding model.
///
/// This package contains the mutable semantic state built from categorized
/// Hibernate Models source facts.  It is the phase that records what the
/// persistence unit says about managed types, attributes, identifiers,
/// contributions, access strategies, paths, and ordering/correspondence facts
/// before those facts are consumed by later phases.
///
/// The objects in this package are intentionally model-like, but they are not
/// intended to become a second compatibility graph under
/// different names.  They own source facts, semantic intent, declaration versus
/// usage context, and resolved boot-time contribution facts.  They should not
/// own physical boot objects, DDL objects, runtime objects, or mutable
/// compatibility state produced by later phases.
///
/// A useful rule of thumb is:
///
/// * binding-model objects answer "what did the domain model declare, and what
///   semantic boot intent has ORM resolved from that declaration?";
/// * materialization outputs answer "what compatibility structures must be
///   produced for existing boot/runtime consumers?";
/// * runtime objects answer "how does the finalized SessionFactory use the
///   model at runtime?".
///
/// When a new field or method would mostly help create DDL structures, cache
/// later-phase boot objects, or mirror mutable compatibility state, it belongs
/// in a materializer or compatibility bridge rather than in this package.
///
/// In the broader design this package belongs to an eventual
/// `org.hibernate.boot.models.mapping` pipeline:
///
/// 1. available resources are gathered;
/// 2. XML and annotation sources are normalized;
/// 3. sources are categorized into managed-type facts;
/// 4. this model records and resolves the boot-time semantic interpretation;
/// 5. view contracts expose stable read shapes;
/// 6. materializers produce the mapping structures used by the runtime.
///
/// The current package remains under `org.hibernate.boot.models.mapping` only
/// because the shared containing package for that full pipeline has not yet been
/// introduced.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.models.mapping.internal.model;

import org.hibernate.Internal;
