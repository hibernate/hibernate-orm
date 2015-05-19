/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.local;

import org.hibernate.hql.spi.id.IdTableInfo;

/**
 * @author Steve Ebersole
 */
public class IdTableInfoImpl implements IdTableInfo {
	private final String idTableName;

	private final String creationStatement;
	private final String dropStatement;

	public IdTableInfoImpl(
			String idTableName,
			String creationStatement,
			String dropStatement) {
		this.idTableName = idTableName;
		this.creationStatement = creationStatement;
		this.dropStatement = dropStatement;
	}

	@Override
	public String getQualifiedIdTableName() {
		return idTableName;
	}

	public String getIdTableCreationStatement() {
		return creationStatement;
	}

	public String getIdTableDropStatement() {
		return dropStatement;
	}
}
