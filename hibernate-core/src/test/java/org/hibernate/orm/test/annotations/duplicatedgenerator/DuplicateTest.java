/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.duplicatedgenerator;

import org.hibernate.DuplicateMappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.orm.test.annotations.Company;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class DuplicateTest  {
	@Test
	public void testDuplicateEntityName() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistry serviceRegistry = null;
		SessionFactory sf = null;
		try {
			cfg.addAnnotatedClass( Flight.class );
			cfg.addAnnotatedClass( org.hibernate.orm.test.annotations.Flight.class );
			cfg.addAnnotatedClass( Company.class );
			cfg.addResource( "org/hibernate/orm/test/annotations/orm.xml" );
			cfg.addResource( "org/hibernate/orm/test/annotations/duplicatedgenerator/orm.xml" );
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
			sf = cfg.buildSessionFactory( serviceRegistry );
			Assert.fail( "Should not be able to map the same entity name twice" );
		}
		catch (DuplicateMappingException ae) {
			//success
		}
		finally {
			if (sf != null){
				sf.close();
			}
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}
}
