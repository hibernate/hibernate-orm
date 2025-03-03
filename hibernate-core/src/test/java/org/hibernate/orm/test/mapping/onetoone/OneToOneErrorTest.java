/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import org.hibernate.AnnotationException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.orm.test.annotations.onetoone.Show;
import org.hibernate.orm.test.annotations.onetoone.ShowDescription;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneErrorTest {
	@Test
	public void testWrongOneToOne() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Show.class )
				.addAnnotatedClass( ShowDescription.class );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistry serviceRegistry = null;
		SessionFactory sessionFactory = null;
		try {
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			Assert.fail( "Wrong mappedBy does not fail property" );
		}
		catch (AnnotationException e) {
			//success
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}

		}
	}
}
