/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import jakarta.persistence.SqlResultSetMapping;

/**
 * Registration of a SqlResultSetMapping while processing managed resources as part of
 * building the domain metamodel
 *
 * @author Steve Ebersole
 */
public record SqlResultSetMappingRegistration(String name, SqlResultSetMapping configuration) {
}
