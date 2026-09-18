/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/// Source-facing entry points for collecting the model resources that categorization
/// should consider.
///
/// This package sits between ORM boot source declarations and model categorization.
/// It normalizes explicit persistence-unit declarations, programmatic persistence
/// configuration, package metadata, annotated classes, and XML mapping bindings into
/// the [org.hibernate.boot.pipeline.internal.source.PreparedMappingSources] contract.
/// Categorization then interprets those source resources.
///
/// ## Persistence-unit entry points
///
/// The bootstrap entry point selects a
/// [org.hibernate.boot.pipeline.internal.source.PersistenceUnitSources] subtype
/// according to who is responsible for discovering the unit's type inventory:
///
///   - Bootstrap by persistence-unit name uses
///     [org.hibernate.boot.pipeline.internal.source.PersistenceUnitSources.Standalone].
///     `HibernatePersistenceProvider.createEntityManagerFactory(String, Map)` and
///     `generateSchema(String, Map)` locate the named unit in `persistence.xml`
///     and select this adapter. Metadata tooling inspecting a parsed unit uses the same path.
///     Collection starts with the descriptor's declarations and supplements them by scanning
///     eligible persistence-unit archives. Excluding unlisted classes suppresses type scanning
///     of the root, while referenced archives remain eligible.
///   - Bootstrap by a container (EE, Quarkus, etc.) uses
///     [org.hibernate.boot.pipeline.internal.source.PersistenceUnitSources.Container].
///     `HibernatePersistenceProvider.createContainerEntityManagerFactory(PersistenceUnitInfo, Map)`
///     and `generateSchema(PersistenceUnitInfo, Map)` select this adapter for the supplied
///     container information. The container supplies the authoritative class, package-descriptor,
///     and module-descriptor inventory, so collection does not invoke provider type scanning.
///
/// Both paths discover `META-INF/orm.xml` in the persistence-unit root and referenced
/// archives when XML mapping is enabled, independently of the exclusion of unlisted classes.
/// Default XML discovery does not search unrelated classloader-visible archives.
///
/// [org.hibernate.boot.pipeline.internal.BootstrapPipeline] uses the selected adapter
/// for factory creation, schema generation, and metadata inspection. It invokes the
/// class-transformer registration hook, resolves settings, establishes discovery services,
/// and calls `collect()`. The transformer hook delegates to the descriptor for
/// `Container` and does nothing for `Standalone`.
///
/// The two `collect()` overloads do not select different discovery contracts.
/// Either overload can be used with either subtype. The three-argument overload accepts
/// already-resolved bootstrap and mapping settings plus a discovery context; the two-argument
/// overload derives mapping settings from the bootstrap settings and descriptor, then delegates
/// to the three-argument overload on the same instance. Full bootstrap uses the three-argument
/// form. The subtype always determines whether provider type scanning occurs.
///
/// ## Programmatic entry points
///
/// [ConfigurationMappingProcessor] adapts
/// [jakarta.persistence.PersistenceConfiguration] through `declared()`, collecting
/// the supplied classes, package descriptors, module descriptors, and XML mapping declarations.
/// For [org.hibernate.jpa.HibernatePersistenceConfiguration], bootstrap instead uses
/// `discover()`, which supplements those declarations using the configured archive
/// boundaries and discovers default XML mappings when enabled. These operations produce
/// [org.hibernate.boot.pipeline.internal.source.MappingSources] directly; they do not
/// select a `PersistenceUnitSources` subtype.
/// ## Collection and preparation
///
/// Each collection path produces a `MappingSources` accumulator with separate class,
/// package-descriptor, module-descriptor, and XML inputs. Collection may access archives,
/// but model-detail resolution and XML binding happen during preparation into
/// `PreparedMappingSources`. Categorization consumes the prepared resources.
///
/// Callers needing source preparation without full bootstrap may also pass a
/// `PersistenceUnitSources` adapter directly to `PreparedMappingSources.from()`.
/// That path collects using the adapter's discovery contract and then prepares the result;
/// it does not invoke the class-transformer registration hook.
///
/// @author Steve Ebersole
package org.hibernate.boot.pipeline.internal.source;
