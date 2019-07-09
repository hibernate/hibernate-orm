/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMutationHandler implements Handler {
	private final SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement;
	private final HandlerCreationContext creationContext;

	public AbstractMutationHandler(
			SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement,
			HandlerCreationContext creationContext) {
		this.sqmDeleteOrUpdateStatement = sqmDeleteOrUpdateStatement;
		this.creationContext = creationContext;
	}

	public SqmDeleteOrUpdateStatement getSqmDeleteOrUpdateStatement() {
		return sqmDeleteOrUpdateStatement;
	}

	public EntityPersister getEntityDescriptor() {
		final String entityName = sqmDeleteOrUpdateStatement.getTarget()
				.getReferencedPathSource()
				.getHibernateEntityName();

		return creationContext.getSessionFactory().getMetamodel().getEntityDescriptor( entityName );

	}

	public HandlerCreationContext getCreationContext() {
		return creationContext;
	}
}
