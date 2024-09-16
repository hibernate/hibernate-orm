/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.mysql;

/**
 * A Dialect for MySQL 5 using InnoDB engine, with support for its spatial features
 *
 * @author Karel Maesen, Geovise BVBA
 * @deprecated Use "hibernate.dialect.storage_engine=innodb" environment variable or JVM system property instead.
 */
@Deprecated
public class MySQL5InnoDBSpatialDialect extends MySQL5SpatialDialect {
}
