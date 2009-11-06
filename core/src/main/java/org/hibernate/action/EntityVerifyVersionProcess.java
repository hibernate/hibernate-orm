/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.action;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.EntityEntry;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.HibernateException;
import org.hibernate.OptimisticLockException;

/**
 * Verify/Increment the entity version
 *
 * @author Scott Marlow
 */
public class EntityVerifyVersionProcess implements BeforeTransactionCompletionProcess
{
	private final Object object;
	private final EntityEntry entry;

	public EntityVerifyVersionProcess(Object object, EntityEntry entry) {
		this.object = object;
		this.entry = entry;
	}

	/**
	 * Perform whatever processing is encapsulated here before completion of the transaction.
	 *
	 * @param session The session on which the transaction is preparing to complete.
	 */
	public void doBeforeTransactionCompletion(SessionImplementor session) {
		final EntityPersister persister = entry.getPersister();

		Object latestVersion = persister.getCurrentVersion(
			entry.getId(), session
		);
	   if(!entry.getVersion().equals(latestVersion))
			throw new OptimisticLockException(
				"Newer version ("+ latestVersion+
				") of entity ("+entry.getEntityName()+") found in database.  id=" +  entry.getId());
	}
}