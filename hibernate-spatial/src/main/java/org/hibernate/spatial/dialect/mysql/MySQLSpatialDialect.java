/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * A Dialect for MySQL with support for its spatial features
 *
 * @author Karel Maesen, Boni Gopalan
 * @deprecated SpatialDialects are no longer needed in Hibernate 6
 */
@Deprecated
public class MySQLSpatialDialect extends MySQLDialect implements SpatialDialect {

}
