/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import jakarta.persistence.NamedStoredProcedureQuery;

/**
 * @author Steve Ebersole
 */
public record NamedStoredProcedureQueryRegistration(String name, NamedStoredProcedureQuery configuration) {
}
