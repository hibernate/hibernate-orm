/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// AST representations for graph-based table mutations.
///
/// This package contains the Abstract Syntax Tree representations of table
/// mutations (INSERT, UPDATE, DELETE) that work with
/// {@link org.hibernate.action.queue.meta.EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.ast.MutatingTableReference}.
///
/// Key differences from {@link org.hibernate.sql.model.ast}:
/// <ul>
///     <li>Uses {@link org.hibernate.action.queue.meta.EntityTableDescriptor} directly</li>
///     <li>No MutatingTableReference wrapper</li>
///     <li>Pre-normalized table and column names</li>
///     <li>Designed for graph-based ActionQueue</li>
/// </ul>
///
/// The mutation types are:
/// <ul>
///     <li>{@link org.hibernate.action.queue.mutation.ast.TableInsert} - INSERT operations</li>
///     <li>{@link org.hibernate.action.queue.mutation.ast.TableUpdate} - UPDATE operations</li>
///     <li>{@link org.hibernate.action.queue.mutation.ast.TableDelete} - DELETE operations</li>
/// </ul>
///
/// These AST nodes are created by builders from
/// {@link org.hibernate.action.queue.mutation.ast.builder} and generate
/// {@link org.hibernate.action.queue.mutation.jdbc.JdbcOperation} instances
/// for execution.
///
/// @see org.hibernate.action.queue.meta.EntityTableDescriptor
/// @see org.hibernate.action.queue.mutation.ast.builder
package org.hibernate.action.queue.mutation.ast;
