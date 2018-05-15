/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.serialization;


import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.junit.Test;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.SerializationException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class SessionFactorySerializationTest extends BaseUnitTestCase {
	public static final String NAME = "mySF";

	@Test
	public void testNamedSessionFactorySerialization() throws Exception {
		Configuration cfg = new Configuration()
				.setProperty( AvailableSettings.SESSION_FACTORY_NAME, NAME )
				.setProperty( AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, "false" ); // default is true
		SessionFactory factory = cfg.buildSessionFactory();

		// we need to do some tricking here so that Hibernate thinks the deserialization happens in a
		// different VM
		String uuid = ( (SessionFactoryImplementor) factory ).getUuid();
		// deregister under this uuid...
		SessionFactoryRegistry.INSTANCE.removeSessionFactory( uuid, NAME, false, null );
		// and then register under a different uuid...
		SessionFactoryRegistry.INSTANCE.addSessionFactory( "some-other-uuid", NAME, false, factory, null );

		SessionFactory factory2 = (SessionFactory) SerializationHelper.clone( factory );
		assertSame( factory, factory2 );

		SessionFactoryRegistry.INSTANCE.removeSessionFactory( "some-other-uuid", NAME, false, null );
		factory.close();

		assertFalse( SessionFactoryRegistry.INSTANCE.hasRegistrations() );
	}

	@Test
	public void testUnNamedSessionFactorySerialization() throws Exception {
		// IMPL NOTE : this test is a control to testNamedSessionFactorySerialization
		// 		here, the test should fail based just on attempted uuid resolution
		Configuration cfg = new Configuration()
				.setProperty( AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, "false" ); // default is true
		SessionFactory factory = cfg.buildSessionFactory();

		// we need to do some tricking here so that Hibernate thinks the deserialization happens in a
		// different VM
		String uuid = ( (SessionFactoryImplementor) factory ).getUuid();
		// deregister under this uuid...
		SessionFactoryRegistry.INSTANCE.removeSessionFactory( uuid, null, false, null );
		// and then register under a different uuid...
		SessionFactoryRegistry.INSTANCE.addSessionFactory( "some-other-uuid", null, false, factory, null );

		try {
			SerializationHelper.clone( factory );
			fail( "Expecting an error" );
		}
		catch ( SerializationException expected ) {
		}

		SessionFactoryRegistry.INSTANCE.removeSessionFactory( "some-other-uuid", null, false, null );
		factory.close();

		assertFalse( SessionFactoryRegistry.INSTANCE.hasRegistrations() );
	}
}
