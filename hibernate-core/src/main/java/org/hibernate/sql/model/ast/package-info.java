/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// SQL AST and builders used with flushed-based mutations (persist, merge, etc.).
///
/// The AST is modeled by [org.hibernate.sql.model.ast.TableMutation] and its subtypes.
///
/// The [org.hibernate.sql.model.ast.builder] package contains all the AST builders.
///
/// > **Note:** This package is temporarily used by both the legacy ActionQueue
/// > and the new graph-based ActionQueue during the transition to the new graph-based
/// > one.  Care should be taken when making changes in this package to verify with both.
///
/// @author Steve Ebersole
package org.hibernate.sql.model.ast;
