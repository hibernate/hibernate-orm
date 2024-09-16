/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.mysql;

/**
 * Spatial Dialect for MySQL 5.6 with InnoDB engine.
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 9/13/13
 * @deprecated Use "hibernate.dialect.storage_engine=innodb" environment variable or JVM system property instead.
 */
@Deprecated
public class MySQL56InnoDBSpatialDialect extends MySQL56SpatialDialect {

}
