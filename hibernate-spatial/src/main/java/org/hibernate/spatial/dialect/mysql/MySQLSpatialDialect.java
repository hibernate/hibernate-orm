/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * A Dialect for MySQL with support for its spatial features
 *
 * @author Karel Maesen, Boni Gopalan
 */
@Deprecated
public class MySQLSpatialDialect extends MySQLDialect implements SpatialDialect {

}
