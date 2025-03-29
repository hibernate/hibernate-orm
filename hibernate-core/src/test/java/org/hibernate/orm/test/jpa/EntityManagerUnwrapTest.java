/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;


/**
 * @author Chris Cranford
 */
public class EntityManagerUnwrapTest extends EntityManagerFactoryBasedFunctionalTest {

	@Test
	public void testUnwrapSession() {
		final EntityManagerFactory entityManagerFactory = entityManagerFactoryScope().getEntityManagerFactory();
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager.unwrap( Session.class );
			entityManager.unwrap( SessionImplementor.class );
			entityManager.unwrap( SharedSessionContractImplementor.class );

			entityManager.unwrap( PersistenceContext.class );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testUnwrapSessionFactory() {
		final EntityManagerFactory entityManagerFactory = entityManagerFactory();

		entityManagerFactory.unwrap( SessionFactory.class );
		entityManagerFactory.unwrap( SessionFactoryImplementor.class );

		entityManagerFactory.unwrap( SessionFactoryServiceRegistry.class );
		entityManagerFactory.unwrap( ServiceRegistry.class );

		entityManagerFactory.unwrap( JdbcServices.class );

		entityManagerFactory.unwrap( jakarta.persistence.Cache.class );
		entityManagerFactory.unwrap( org.hibernate.Cache.class );

		entityManagerFactory.unwrap( jakarta.persistence.metamodel.Metamodel.class );
		entityManagerFactory.unwrap( Metamodel.class );
		entityManagerFactory.unwrap( MappingMetamodel.class );
		entityManagerFactory.unwrap( RuntimeMetamodelsImplementor.class );

		entityManagerFactory.unwrap( QueryEngine.class );
	}
}
