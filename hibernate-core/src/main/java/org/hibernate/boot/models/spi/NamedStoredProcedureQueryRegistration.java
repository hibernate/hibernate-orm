/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import jakarta.persistence.NamedStoredProcedureQuery;

/**
 * @author Steve Ebersole
 */
public record NamedStoredProcedureQueryRegistration(String name, NamedStoredProcedureQuery configuration) {
}
