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
/// intended to become a second `org.hibernate.mapping` object graph under
/// different names.  They own source facts, semantic intent, declaration versus
/// application context, and resolved boot-time contribution facts.  They should
/// not own physical compatibility objects such as `PersistentClass`,
/// `MappedSuperclass`, `Component`, `Property`, `Value`, `Column`, `Table`, or
/// foreign-key/primary-key/unique-key instances.
///
/// A useful rule of thumb is:
///
/// * binding-model objects answer "what did the domain model declare, and what
///   semantic boot intent has ORM resolved from that declaration?";
/// * `org.hibernate.mapping` objects answer "what compatibility mapping
///   structures must be materialized for existing boot/runtime consumers?";
/// * runtime mapping objects answer "how does the finalized SessionFactory use
///   the model at runtime?".
///
/// When a new field or method would mostly help create DDL structures, cache
/// legacy boot objects, or mirror mutable `org.hibernate.mapping` state, it
/// belongs in a materializer or compatibility bridge rather than in this package.
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
/// The current package remains under `org.hibernate.boot.models.bind` only
/// because the shared containing package for that full pipeline has not yet been
/// introduced.
///
/// @author Steve Ebersole
package org.hibernate.boot.models.bind.internal.model;
