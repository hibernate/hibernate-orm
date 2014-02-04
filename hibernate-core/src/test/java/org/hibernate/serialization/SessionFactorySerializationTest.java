/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.serialization;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.SerializationException;
import org.junit.Test;

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
		Reference reference = factory.getReference();
		StringRefAddr refAddr = (StringRefAddr) reference.get( "uuid" );
		String uuid = (String) refAddr.getContent();
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
		Reference reference = factory.getReference();
		StringRefAddr refAddr = (StringRefAddr) reference.get( "uuid" );
		String uuid = (String) refAddr.getContent();
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
