/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.action.internal;

import org.hibernate.OptimisticLockException;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * A BeforeTransactionCompletionProcess impl to verify an entity version as part of
 * before-transaction-completion processing
 *
 * @author Scott Marlow
 */
public class EntityVerifyVersionProcess implements BeforeTransactionCompletionProcess {
	private final Object object;
	private final EntityEntry entry;

	/**
	 * Constructs an EntityVerifyVersionProcess
	 *
	 * @param object The entity instance
	 * @param entry The entity's referenced EntityEntry
	 */
	public EntityVerifyVersionProcess(Object object, EntityEntry entry) {
		this.object = object;
		this.entry = entry;
	}

	@Override
	public void doBeforeTransactionCompletion(SessionImplementor session) {
		final EntityPersister persister = entry.getPersister();

		final Object latestVersion = persister.getCurrentVersion( entry.getId(), session );
		if ( !entry.getVersion().equals( latestVersion ) ) {
			throw new OptimisticLockException(
					object,
					"Newer version [" + latestVersion +
							"] of entity [" + MessageHelper.infoString( entry.getEntityName(), entry.getId() ) +
							"] found in database"
			);
		}
	}
}
