/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.mysql;

/**
 * Spatial Dialect for MySQL 5.6 with InnoDB engine.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 9/13/13
 */
public class MySQL56InnoDBSpatialDialect extends MySQL56SpatialDialect {

	@Override
	public boolean supportsCascadeDelete() {
		return true;
	}

	@Override
	public String getTableTypeString() {
		return " ENGINE=InnoDB";
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}

}
