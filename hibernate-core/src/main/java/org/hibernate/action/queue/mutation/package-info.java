/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Graph-based mutation operations designed for the graph-based ActionQueue.
///
/// This package provides a parallel mutation infrastructure that uses
/// {@link org.hibernate.action.queue.meta.EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.TableMapping}, enabling:
///
/// <ul>
///     <li>Pre-normalized table and column names (computed once at SessionFactory initialization)</li>
///     <li>Elimination of runtime conversion overhead</li>
///     <li>Direct use of EntityPersister-provided TableDescriptor instances</li>
///     <li>Clean separation from legacy TableMapping-based mutations</li>
/// </ul>
///
/// The key types are:
/// <ul>
///     <li>{@link org.hibernate.action.queue.mutation.jdbc.JdbcOperation} - single mutation operation</li>
///     <li>{@link org.hibernate.action.queue.mutation.jdbc.PreparableJdbcOperation} - preparable JDBC operation</li>
///     <li>{@link org.hibernate.action.queue.mutation.jdbc.SelfExecutingJdbcOperation} - self-executing JDBC operation</li>
/// </ul>
///
/// This infrastructure is used by mutation decomposers
/// ({@link org.hibernate.persister.entity.mutation.InsertDecomposer}, etc.) to create
/// {@link org.hibernate.action.queue.op.PlannedOperation} instances for the graph-based
/// ActionQueue.
///
/// Legacy mutation coordinators continue to use the original
/// {@link org.hibernate.sql.model.MutationOperation} infrastructure, ensuring
/// backward compatibility.
///
/// @see org.hibernate.action.queue.meta.EntityTableDescriptor
/// @see org.hibernate.action.queue.mutation.builder
/// @see org.hibernate.action.queue.mutation.ast
package org.hibernate.action.queue.mutation;
