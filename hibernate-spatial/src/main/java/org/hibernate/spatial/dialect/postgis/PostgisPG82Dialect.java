/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * Extends the {@code PostgreSQL82Dialect} to add support for the Postgis spatial types, functions and operators .
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 */
public class PostgisPG82Dialect extends PostgreSQL82Dialect implements SpatialDialect {
}
