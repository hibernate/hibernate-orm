/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa;

import javax.persistence.metamodel.Metamodel;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.metamodel.spi.DomainMetamodel;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.junit.Test;

/**
 * @author Chris Cranford
 */
public class EntityManagerUnwrapTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testUnwrapSession() {
		getOrCreateEntityManager().unwrap( Session.class );
		getOrCreateEntityManager().unwrap( SessionImplementor.class );
		getOrCreateEntityManager().unwrap( SharedSessionContractImplementor.class );

		getOrCreateEntityManager().unwrap( PersistenceContext.class );
	}

	@Test
	public void testUnwrapSessionFactory() {
		entityManagerFactory().unwrap( SessionFactory.class );
		entityManagerFactory().unwrap( SessionFactoryImplementor.class );

		entityManagerFactory().unwrap( SessionFactoryServiceRegistry.class );
		entityManagerFactory().unwrap( ServiceRegistry.class );

		entityManagerFactory().unwrap( JdbcServices.class );

		entityManagerFactory().unwrap( javax.persistence.Cache.class );
		entityManagerFactory().unwrap( org.hibernate.Cache.class );

		entityManagerFactory().unwrap( javax.persistence.metamodel.Metamodel.class );
		entityManagerFactory().unwrap( Metamodel.class );
		entityManagerFactory().unwrap( MetamodelImplementor.class );
		entityManagerFactory().unwrap( DomainMetamodel.class );

		entityManagerFactory().unwrap( QueryEngine.class );
	}
}
