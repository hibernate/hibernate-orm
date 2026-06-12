/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/// Contracts describing the categorized domain model.
///
/// Categorization is the boot-time phase between source collection and binding.  Source
/// collection assembles the available managed classes, packages, and XML mappings.  The
/// categorizer interprets those inputs through the Hibernate Models infrastructure and
/// exposes persistent types, entity hierarchies, persistent attributes, key mappings,
/// and persistence-unit scoped registrations in the contracts in this package.
///
/// These contracts intentionally model what later binding steps need to know about the
/// domain model.  They do not own the source registries used while categorizing, such
/// as the `ClassDetailsRegistry` or `AnnotationDescriptorRegistry`; those
/// are categorization services exposed through [org.hibernate.boot.models.categorize.spi.CategorizationContext].
///
/// @see org.hibernate.boot.models.AvailableResources
/// @see org.hibernate.boot.models.categorize.spi.DomainModelCategorizer
/// @see org.hibernate.boot.models.categorize.spi.CategorizedDomainModel
///
/// @author Steve Ebersole
package org.hibernate.boot.models.categorize.spi;
