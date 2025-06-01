/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.sqm.mutation.internal.Handler;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMutationHandler implements Handler {
	private final SqmDeleteOrUpdateStatement<?> sqmDeleteOrUpdateStatement;

	private final SessionFactoryImplementor sessionFactory;
	private final EntityMappingType entityDescriptor;

	public AbstractMutationHandler(
			SqmDeleteOrUpdateStatement<?> sqmDeleteOrUpdateStatement,
			SessionFactoryImplementor sessionFactory) {
		this.sqmDeleteOrUpdateStatement = sqmDeleteOrUpdateStatement;
		this.sessionFactory = sessionFactory;

		final String entityName = sqmDeleteOrUpdateStatement.getTarget()
				.getModel()
				.getHibernateEntityName();

		this.entityDescriptor = sessionFactory.getMappingMetamodel().getEntityDescriptor( entityName );

	}

	public SqmDeleteOrUpdateStatement<?> getSqmDeleteOrUpdateStatement() {
		return sqmDeleteOrUpdateStatement;
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}
}
