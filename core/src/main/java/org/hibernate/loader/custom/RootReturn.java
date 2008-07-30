/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.loader.custom;

import org.hibernate.LockMode;
import org.hibernate.loader.EntityAliases;

/**
 * Represents a return which names a "root" entity.
 * <p/>
 * A root entity means it is explicitly a "column" in the result, as opposed to
 * a fetched association.
 *
 * @author Steve Ebersole
 */
public class RootReturn extends NonScalarReturn {
	private final String entityName;
	private final EntityAliases entityAliases;

	public RootReturn(
			String alias,
			String entityName,
			EntityAliases entityAliases,
			LockMode lockMode) {
		super( alias, lockMode );
		this.entityName = entityName;
		this.entityAliases = entityAliases;
	}

	public String getEntityName() {
		return entityName;
	}

	public EntityAliases getEntityAliases() {
		return entityAliases;
	}
}
