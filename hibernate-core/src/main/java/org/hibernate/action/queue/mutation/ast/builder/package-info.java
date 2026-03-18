/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Builders for graph-based table mutations.
///
/// This package provides builder interfaces for constructing graph-based
/// table mutations that work with {@link org.hibernate.action.queue.meta.EntityTableDescriptor}
/// instead of {@link org.hibernate.sql.model.TableMapping}.
///
/// Key features:
/// <ul>
///     <li>Accept pre-normalized {@link org.hibernate.action.queue.meta.EntityTableDescriptor}</li>
///     <li>Work with {@link org.hibernate.action.queue.meta.ColumnDescriptor} for pre-normalized column names</li>
///     <li>Provide transitional methods accepting {@link org.hibernate.metamodel.mapping.SelectableMapping}</li>
///     <li>Build {@link org.hibernate.action.queue.mutation.ast.TableMutation} instances</li>
/// </ul>
///
/// The builder types are:
/// <ul>
///     <li>{@link org.hibernate.action.queue.mutation.ast.builder.GraphTableInsertBuilder} - INSERT builders</li>
///     <li>{@link org.hibernate.action.queue.mutation.ast.builder.GraphTableUpdateBuilder} - UPDATE builders</li>
///     <li>{@link org.hibernate.action.queue.mutation.ast.builder.GraphTableDeleteBuilder} - DELETE builders</li>
/// </ul>
///
/// Used by mutation decomposers to build mutations from EntityPersister-provided
/// TableDescriptor instances.
///
/// @see org.hibernate.action.queue.meta.EntityTableDescriptor
/// @see org.hibernate.action.queue.mutation.ast
package org.hibernate.action.queue.mutation.ast.builder;
