/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/// Support for categorizing available ORM mapping sources into a domain model that
/// later binding steps can consume.
///
/// Categorization starts from {@link org.hibernate.boot.models.AvailableResources}
/// and interprets class, package, annotation, and XML inputs through Hibernate
/// Models.  The result is a
/// {@link org.hibernate.boot.models.categorize.spi.CategorizedDomainModel}
/// representing entity hierarchies, visible mapped-superclasses, embeddables,
/// persistent attributes, key mappings, and global registrations.
///
/// @author Steve Ebersole
package org.hibernate.boot.models.categorize;
