/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Support for resolving bootstrap settings that affect source collection,
/// categorization, binding, and SessionFactory construction.
///
/// This package is a normalization boundary between loosely typed bootstrap
/// configuration and the model-building steps that need stable decisions.  It
/// exposes frequently used settings as grouped named values while still carrying
/// raw configuration values forward for later bootstrap stages.
///
/// @author Steve Ebersole
package org.hibernate.boot.pipeline.internal.settings;
