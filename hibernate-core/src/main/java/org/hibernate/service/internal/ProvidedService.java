/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

/**
 * A service provided as-is.
 *
 * @author Steve Ebersole
 */
public record ProvidedService<R>(Class<R> serviceRole, R service) {
}
