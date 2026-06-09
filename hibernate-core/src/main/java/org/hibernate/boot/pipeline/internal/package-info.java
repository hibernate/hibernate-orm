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
 * The current PoC slice is deliberately narrow: settings are resolved through
 * {@link org.hibernate.boot.pipeline.internal.settings.SettingsResolver} into phase-specific
 * buckets, and then {@link org.hibernate.boot.pipeline.internal.MetadataResolver}
 * turns resolved settings, source contributions, and a service registry into
 * {@link org.hibernate.boot.pipeline.internal.ResolvedMetadata} by running source
 * resource creation, categorization, binding, metadata registration, ordering,
 * and validation in order.  {@link org.hibernate.boot.pipeline.internal.SessionFactoryPipeline}
 * is the next gross target and will own construction of the runtime
 * SessionFactory from resolved metadata and factory settings.
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.pipeline.internal;
