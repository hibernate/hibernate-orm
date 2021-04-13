/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import org.hibernate.dialect.PostgreSQL95Dialect;

/**
 * Extends the {@code PostgreSQL95Dialect} to add support for the Postgis spatial types, functions and operators .
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 */
public class PostgisPG95Dialect extends PostgreSQL95Dialect implements PGSpatialDialectTrait {
}
