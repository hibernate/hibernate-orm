/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
/// Materializers that consume binding-model views and create boot mapping
/// structures.
///
/// This package is the destination-facing phase of the current
/// `org.hibernate.boot.models.mapping` implementation.  Materializers should
/// consume stable view contracts and apply them to the mapping structures used
/// by later boot and runtime code.  They should not be the place where source
/// facts are rediscovered or where phase ordering is guessed from partially
/// populated state.
///
/// In the eventual `org.hibernate.boot.models.mapping` pipeline, this package is
/// the final phase after resource handling, XML normalization, categorization,
/// model population, and view projection.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.models.mapping.internal.materialize;

import org.hibernate.Internal;
