/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement.deepcollectionelements;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
@FailureExpected( jiraKey = "HHH-3157" )
public class DeepCollectionElementTest extends BaseUnitTestCase {

	@Test
	public void testInitialization() throws Exception {
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
