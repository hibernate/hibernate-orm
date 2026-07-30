/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Source-to-mapping pipeline for boot-time mapping model creation.
///
/// This package and its `internal` subpackages are ORM implementation details.
/// Their types are often `public` so that separate pipeline phases can share
/// contracts across package boundaries, but that Java visibility should not be
/// read as a supported user API. The
/// [org.hibernate.boot.mapping.spi] subpackage is the deliberate exception: it
/// exposes supported, read-only semantic views to bootstrap extensions.
///
/// This package family owns the phases that turn available boot model
/// resources into Hibernate's resolved boot mapping model.  The broad flow is:
///
/// 1. discover and normalize resolved mapping sources;
/// 2. apply XML annotation-clearing and overlay rules;
/// 3. categorize managed types, attributes, identifiers, and global
///    registrations;
/// 4. populate and resolve the binding model;
/// 5. expose finalized read views over the resolved binding state;
/// 6. materialize `org.hibernate.mapping` objects from those views.
///
/// ## Categorization, binding, and materialization
///
/// At the level visible to bootstrap extensions, the pipeline has three main
/// semantic stages:
///
/// ### Categorization
///
/// Interprets the normalized annotation and XML sources and produces the
/// categorized domain model. Managed types, attributes, and values are
/// described without creating mutable `org.hibernate.mapping` objects. The
/// supported read-only root of this result is
/// [org.hibernate.boot.mapping.spi.CategorizedDomainModel].
///
/// ### Binding
///
/// Interprets categorized facts in their mapping context and records the
/// source declaration and each contextual usage of an attribute. This is where
/// inherited and embedded paths, generic specialization, value intent,
/// contributions, and deferred correspondence become explicit. The binding
/// model is Hibernate-owned; extensions receive supported read-only
/// [org.hibernate.boot.mapping.spi.AttributeDeclaration] and
/// [org.hibernate.boot.mapping.spi.AttributeUsage] views, not the mutable
/// binding state itself.
///
/// ### Materialization
///
/// Creates the mutable boot model in `org.hibernate.mapping`. When an attribute
/// usage is assigned a concrete place in that model, its
/// [org.hibernate.boot.mapping.spi.MappingRole] and usage are correlated by an
/// [org.hibernate.boot.mapping.spi.AttributeApplication]. Custom binders run at
/// this semantic-to-materialized boundary: they read the supported semantic
/// view and customize the corresponding `PersistentClass`, `Component`,
/// `Property`, or `Value`. Resolution and finalization continue after these
/// structural mapping objects have been created.
///
/// The subpackages split that process by phase and responsibility:
///
/// - [org.hibernate.boot.mapping.spi] exposes the supported, read-only semantic
///  contracts used by bootstrap extensions.
/// - [org.hibernate.boot.mapping.internal.xml] handles XML resource processing,
///   including complete-metadata annotation clearing and non-complete XML
///   overlays.
/// - [org.hibernate.boot.mapping.internal.categorize] turns normalized source
///   details into managed-type, attribute, identifier, and registration
///   categories.
/// - [org.hibernate.boot.mapping.internal.binders] contains phase/action classes
///   that interpret categorized facts and populate binding state.
/// - [org.hibernate.boot.mapping.internal.model] contains mutable boot binding
///   state: managed types, declarations, usages, value intents, contributions,
///   and correspondence facts.
/// - [org.hibernate.boot.mapping.internal.view] contains stable read contracts
///   over finalized binding state.
/// - [org.hibernate.boot.mapping.internal.materialize] creates or populates
///   `org.hibernate.mapping` compatibility objects from finalized views.
/// - [org.hibernate.boot.mapping.internal.sources] contains source-context
///   objects used by binders and materializers while interpreting attributes,
///   components, tables, associations, and selectables.
/// - [org.hibernate.boot.mapping.internal.relational] contains binding-time table
///   reference contracts and implementations used by table, value, association,
///   and key binding.
/// - [org.hibernate.boot.mapping.internal.context] contains shared
///   pipeline context contracts, implementation state, and adapters that do
///   not belong to one narrow phase.
/// The long-term direction is for `org.hibernate.mapping` to become the honest
/// resolved boot mapping model.  The binding model and views exist to preserve
/// source facts, application context, deferred dependencies, and ordering or
/// correspondence information until `org.hibernate.mapping` can represent those
/// facts directly.
///
/// Audit summary:
///
/// - This package and the `internal` subpackages are not supported user API.
/// - [org.hibernate.boot.mapping.spi] is the supported read-only boundary over
///   selected categorized and binding-model semantics.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.mapping;

import org.hibernate.Internal;
