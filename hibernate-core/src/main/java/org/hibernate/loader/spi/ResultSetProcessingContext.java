/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public interface ResultSetProcessingContext {
	public SessionImplementor getSession();

	public QueryParameters getQueryParameters();

	public EntityKey getDictatedRootEntityKey();

	public static interface IdentifierResolutionContext {
		public EntityReference getEntityReference();

		public void registerHydratedForm(Object hydratedForm);

		public Object getHydratedForm();

		public void registerEntityKey(EntityKey entityKey);

		public EntityKey getEntityKey();
	}

	public IdentifierResolutionContext getIdentifierResolutionContext(EntityReference entityReference);

	public void registerHydratedEntity(EntityPersister persister, EntityKey entityKey, Object entityInstance);

	public static interface EntityKeyResolutionContext {
		public EntityPersister getEntityPersister();
		public LockMode getLockMode();
		public EntityAliases getEntityAliases();
	}

	public Object resolveEntityKey(EntityKey entityKey, EntityKeyResolutionContext entityKeyContext);


	// should be able to get rid of the methods below here from the interface ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void checkVersion(
			ResultSet resultSet,
			EntityPersister persister,
			EntityAliases entityAliases,
			EntityKey entityKey,
			Object entityInstance) throws SQLException;

	public String getConcreteEntityTypeName(
			ResultSet resultSet,
			EntityPersister persister,
			EntityAliases entityAliases,
			EntityKey entityKey) throws SQLException;

	public void loadFromResultSet(
			ResultSet resultSet,
			Object entityInstance,
			String concreteEntityTypeName,
			EntityKey entityKey,
			EntityAliases entityAliases,
			LockMode acquiredLockMode,
			EntityPersister persister,
			boolean eagerFetch,
			EntityType associationType) throws SQLException;
}
