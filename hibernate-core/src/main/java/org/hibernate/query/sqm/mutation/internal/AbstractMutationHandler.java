/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.tree.SqmDmlStatement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMutationHandler implements Handler {
	private final SqmDmlStatement<?> sqmDmlStatement;

	private final SessionFactoryImplementor sessionFactory;
	private final EntityPersister entityDescriptor;

	public AbstractMutationHandler(
			SqmDmlStatement<?> sqmDmlStatement,
			SessionFactoryImplementor sessionFactory) {
		this.sqmDmlStatement = sqmDmlStatement;
		this.sessionFactory = sessionFactory;

		final String entityName = sqmDmlStatement.getTarget()
				.getModel()
				.getHibernateEntityName();

		this.entityDescriptor = sessionFactory.getMappingMetamodel().getEntityDescriptor( entityName );

	}

	public SqmDmlStatement<?> getSqmStatement() {
		return sqmDmlStatement;
	}

	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}
}
