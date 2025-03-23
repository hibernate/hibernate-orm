/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.deepcollectionelements;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@FailureExpected( jiraKey = "HHH-3157" )
@BaseUnitTest
public class DeepCollectionElementTest {

	@Test
	public void testInitialization() {
		Configuration configuration = new Configuration();
		configuration.addAnnotatedClass( A.class );
		configuration.addAnnotatedClass( B.class );
		configuration.addAnnotatedClass( C.class );
		StandardServiceRegistryImpl serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( configuration.getProperties() );
		try {
			SessionFactory sessionFactory = configuration.buildSessionFactory( serviceRegistry );
			sessionFactory.close();
		}
		finally {
			serviceRegistry.destroy();
		}
	}
}
