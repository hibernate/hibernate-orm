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
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Represents a reference to an entity either as a return or as a fetch
 *
 * @author Steve Ebersole
 */
public interface EntityReference
		extends IdentifierDescriptionInjectable, ResultSetProcessingContext.EntityKeyResolutionContext {
	/**
	 * Retrieve the alias associated with the persister (entity/collection).
	 *
	 * @return The alias
	 */
	public String getAlias();

	/**
	 * Retrieve the SQL table alias.
	 *
	 * @return The SQL table alias
	 */
	public String getSqlTableAlias();

	/**
	 * Retrieve the lock mode associated with this return.
	 *
	 * @return The lock mode.
	 */
	public LockMode getLockMode();

	/**
	 * Retrieves the EntityPersister describing the entity associated with this Return.
	 *
	 * @return The EntityPersister.
	 */
	public EntityPersister getEntityPersister();

	public IdentifierDescription getIdentifierDescription();

	/**
	 * Ugh.  *Really* hate this here.
	 *
	 * @return
	 */
	public EntityAliases getEntityAliases();
}
