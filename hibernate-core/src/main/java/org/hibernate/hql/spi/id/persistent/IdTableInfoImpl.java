/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.persistent;

import org.hibernate.hql.spi.id.IdTableInfo;

/**
 * IdTableInfo implementation specific to PersistentTableBulkIdStrategy
 *
 * @author Steve Ebersole
 */
class IdTableInfoImpl implements IdTableInfo {
	private final String idTableName;

	public IdTableInfoImpl(String idTableName) {
		this.idTableName = idTableName;
	}

	@Override
	public String getQualifiedIdTableName() {
		return idTableName;
	}
}
