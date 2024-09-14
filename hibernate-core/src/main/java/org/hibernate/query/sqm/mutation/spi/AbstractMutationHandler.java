/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

		this.entityDescriptor = sessionFactory.getRuntimeMetamodels().getEntityMappingType( entityName );

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
