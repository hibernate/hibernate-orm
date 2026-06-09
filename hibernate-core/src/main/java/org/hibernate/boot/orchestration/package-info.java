/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Coordinates the high-level flow from normalized bootstrap inputs toward
 * SessionFactory creation.
 * <p>
 * This package is intentionally about phase ordering, not about the mechanics of
 * any one phase.  Entry-point-specific code should adapt its inputs into request
 * objects.  The orchestration layer then delegates to focused components for
 * source collection, categorization, binding, option resolution, and eventual
 * factory construction.
 * <p>
 * The current PoC slice is deliberately narrow: settings are resolved by
 * {@link org.hibernate.boot.settings.BootstrapSettingsResolver}, and then
 * {@link org.hibernate.boot.orchestration.MetadataResolver} turns resolved
 * settings, source contributions, and a service registry into
 * {@link org.hibernate.boot.orchestration.ResolvedMetadata} by running source
 * resource creation, categorization, binding, metadata registration, ordering,
 * and validation in order.  {@link org.hibernate.boot.orchestration.SessionFactoryBuilder}
 * is the next gross target and will own construction of the runtime
 * SessionFactory from resolved metadata and factory settings.
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.orchestration;
