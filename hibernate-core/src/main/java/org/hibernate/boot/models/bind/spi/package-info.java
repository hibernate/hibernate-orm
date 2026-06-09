/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/// Contracts for binding the categorized domain model into Hibernate's boot-time
/// mapping model.
///
/// Binding is the phase after categorization.  It consumes
/// [org.hibernate.boot.models.categorize.spi.CategorizedDomainModel] contracts and
/// produces or updates Hibernate mapping objects such as [org.hibernate.mapping.PersistentClass],
/// [org.hibernate.mapping.Property], and [org.hibernate.mapping.Table].  The SPI in
/// this package provides access to binding services, binding state, binding options,
/// and table references shared by the binders.
///
/// These contracts should work from categorized metadata and binding sources.  They
/// should not repeat source collection or re-categorize annotations and XML mappings.
///
/// @see org.hibernate.boot.models.categorize.spi.CategorizedDomainModel
/// @see org.hibernate.boot.models.bind.spi.BindingCoordinator
/// @see org.hibernate.boot.models.bind.spi.BindingContext
/// @see org.hibernate.boot.models.bind.spi.BindingState
///
/// @author Steve Ebersole
package org.hibernate.boot.models.bind.spi;
