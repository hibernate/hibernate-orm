/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class EntityReferenceAliasesImpl implements EntityReferenceAliases {
	private final String tableAlias;
	private final EntityAliases columnAliases;

	public EntityReferenceAliasesImpl(String tableAlias, EntityAliases columnAliases) {
		this.tableAlias = tableAlias;
		this.columnAliases = columnAliases;
	}

	@Override
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public EntityAliases getColumnAliases() {
		return columnAliases;
	}
}
