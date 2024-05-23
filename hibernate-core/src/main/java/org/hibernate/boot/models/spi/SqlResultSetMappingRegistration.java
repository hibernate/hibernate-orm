/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
