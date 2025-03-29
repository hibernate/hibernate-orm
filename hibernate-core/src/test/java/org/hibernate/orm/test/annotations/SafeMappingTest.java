/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.models.AccessTypeDeterminationException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;

import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class SafeMappingTest {
	@Test
	public void testDeclarativeMix() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( IncorrectEntity.class );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistry serviceRegistry = null;
		SessionFactory sessionFactory = null;
		try {
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			fail( "Entity wo id should fail" );
		}
		catch (AnnotationException | AccessTypeDeterminationException e) {
			//success
		}
		finally {
			if( sessionFactory !=null){
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
			cfg.getStandardServiceRegistryBuilder().getBootstrapServiceRegistry().close();
		}
	}
}
