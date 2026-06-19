/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Internal proof package for a future public binding-layer extension SPI.
///
/// The contracts in this package intentionally resemble the eventual supported
/// replacement shape for `org.hibernate.binder.AttributeBinder` and
/// `org.hibernate.binder.TypeBinder`: contributors act on capability-oriented
/// targets rather than directly mutating mapping objects.  For now, however,
/// these types are strictly internal.  They let ORM-owned annotations such as
/// `@NaturalId`, `@Collate`, and `@TenantId` exercise the target/facet model
/// while ORM still adapts the calls to current internal contribution records
/// and materializers.
///
/// Extension authors should not depend on this package.  The public SPI should
/// be introduced only after the binding model, contribution ordering,
/// diagnostics, and compatibility rules have settled enough to support a stable
/// contract.
///
/// @author Steve Ebersole
@Internal
package org.hibernate.boot.models.mapping.internal.extension;

import org.hibernate.Internal;
