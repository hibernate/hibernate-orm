/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Source-to-mapping pipeline for boot-time mapping model creation.
///
/// This package family is an ORM-internal implementation detail.  Its types are
/// often `public` so that separate pipeline phases can share contracts across
/// package boundaries, but that Java visibility should not be read as a
/// supported user API.
///
/// This package family owns the phases that turn available boot model
/// resources into Hibernate's resolved boot mapping model.  The broad flow is:
///
/// 1. discover and normalize available resources;
/// 2. apply XML annotation-clearing and overlay rules;
/// 3. categorize managed types, attributes, identifiers, and global
///    registrations;
/// 4. populate and resolve the binding model;
/// 5. expose finalized read views over the resolved binding state;
/// 6. materialize `org.hibernate.mapping` objects from those views.
///
/// The subpackages split that process by phase and responsibility:
///
/// - [org.hibernate.boot.models.mapping.internal.xml] handles XML resource processing,
///   including complete-metadata annotation clearing and non-complete XML
///   overlays.
/// - [org.hibernate.boot.models.mapping.internal.categorize] turns normalized source
///   details into managed-type, attribute, identifier, and registration
///   categories.
/// - [org.hibernate.boot.models.mapping.internal.binders] contains phase/action classes
///   that interpret categorized facts and populate binding state.
/// - [org.hibernate.boot.models.mapping.internal.model] contains mutable boot binding
///   state: managed types, declarations, usages, value intents, contributions,
///   and correspondence facts.
/// - [org.hibernate.boot.models.mapping.internal.view] contains stable read contracts
///   over finalized binding state.
/// - [org.hibernate.boot.models.mapping.internal.materialize] creates or populates
///   `org.hibernate.mapping` compatibility objects from finalized views.
/// - [org.hibernate.boot.models.mapping.internal.sources] contains source-context
///   objects used by binders and materializers while interpreting attributes,
///   components, tables, associations, and selectables.
/// - [org.hibernate.boot.models.mapping.internal.relational] contains binding-time table
///   reference contracts and implementations used by table, value, association,
///   and key binding.
/// - [org.hibernate.boot.models.mapping.internal.context] contains shared
///   pipeline context contracts, implementation state, and adapters that do
///   not belong to one narrow phase.
/// - [org.hibernate.boot.models.mapping.internal.extension] is an internal-only proof
///   of a possible future public binding extension SPI, not a supported
///   extension contract.
///
/// The long-term direction is for `org.hibernate.mapping` to become the honest
/// resolved boot mapping model.  The binding model and views exist to preserve
/// source facts, application context, deferred dependencies, and ordering or
/// correspondence information until `org.hibernate.mapping` can represent those
/// facts directly.
///
/// Audit summary:
///
/// - There is no supported user API in this package family.
/// - All currently implemented pipeline contracts live under `internal` because
///   they are for Hibernate code only.
/// - The internal extension package is a proof area for a possible future
///   supported binding extension SPI; it is not itself an SPI.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.models.mapping;

import org.hibernate.Internal;
