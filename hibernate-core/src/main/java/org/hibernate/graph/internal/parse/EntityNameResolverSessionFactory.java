/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Steve Ebersole
 */
public class EntityNameResolverSessionFactory implements EntityNameResolver {
	private final SessionFactoryImplementor sessionFactory;

	public EntityNameResolverSessionFactory(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public <T> EntityDomainType<T> resolveEntityName(String entityName) {
		//noinspection unchecked
		return (EntityDomainType<T>) sessionFactory.getJpaMetamodel().findEntityType( entityName );
	}
}
