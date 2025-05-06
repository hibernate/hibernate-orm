/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@JiraKeyGroup( value = {
		@JiraKey( value = "HHH-6271" ),
		@JiraKey( value = "HHH-14529" )
} )
public class OrmVersion1SupportedTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
	}

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger(
					MethodHandles.lookup(),
					CoreMessageLogger.class,
					"org.hibernate.internal.util.xml.ErrorLogger"
			)
		);

	@Test
	public void testOrm1Support() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH00196" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Light light = new Light();
		light.name = "the light at the end of the tunnel";
		s.persist( light );
		s.flush();
		s.clear();

		assertEquals( 1, s.getNamedQuery( "find.the.light" ).list().size() );
		tx.rollback();
		s.close();

		assertFalse( triggerable.wasTriggered() );
	}

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/annotations/xml/ejb3/orm2.xml" };
	}
}
