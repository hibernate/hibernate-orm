/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.EntityAliases;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class EntityFetch extends AbstractFetch implements EntityReference {
	private final String sqlTableAlias;
	private final EntityAliases entityAliases;

	private final EntityPersister persister;

	public EntityFetch(
			SessionFactoryImplementor sessionFactory,
			String alias,
			LockMode lockMode,
			AbstractFetchOwner owner,
			String ownerProperty,
			FetchStrategy fetchStrategy,
			String sqlTableAlias,
			EntityAliases entityAliases) {
		super( sessionFactory, alias, lockMode, owner, ownerProperty, fetchStrategy );
		this.sqlTableAlias = sqlTableAlias;
		this.entityAliases = entityAliases;

		final EntityType type = (EntityType) owner.retrieveFetchSourcePersister().getPropertyType( ownerProperty );
		this.persister = sessionFactory.getEntityPersister( type.getAssociatedEntityName() );
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public EntityAliases getEntityAliases() {
		return entityAliases;
	}

	@Override
	public String getSqlTableAlias() {
		return sqlTableAlias;
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return persister;
	}
}
