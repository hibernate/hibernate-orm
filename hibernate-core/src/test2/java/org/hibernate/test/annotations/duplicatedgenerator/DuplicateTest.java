/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.duplicatedgenerator;

import org.hibernate.AnnotationException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class DuplicateTest  {
    @Test
	public void testDuplicateEntityName() throws Exception {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistry serviceRegistry = null;
		SessionFactory sf = null;
		try {
			cfg.addAnnotatedClass( Flight.class );
			cfg.addAnnotatedClass( org.hibernate.test.annotations.Flight.class );
			cfg.addResource( "org/hibernate/test/annotations/orm.xml" );
			cfg.addResource( "org/hibernate/test/annotations/duplicatedgenerator/orm.xml" );
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
			sf = cfg.buildSessionFactory( serviceRegistry );
            Assert.fail( "Should not be able to map the same entity name twice" );
		}
		catch (AnnotationException ae) {
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
