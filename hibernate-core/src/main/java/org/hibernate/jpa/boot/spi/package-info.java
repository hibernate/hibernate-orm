/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * SPI settings and contracts used by Hibernate while acting as a JPA
 * persistence provider.
 * <p>
 * This package includes symbolic setting names, contributor contracts, and
 * provider-specific extension points consumed by the bootstrap pipeline.
 * One-shot bootstrap entry points use
 * {@link org.hibernate.boot.pipeline.internal.BootstrapPipeline}.
 *
 * @author Steve Ebersole
 */
package org.hibernate.jpa.boot.spi;
