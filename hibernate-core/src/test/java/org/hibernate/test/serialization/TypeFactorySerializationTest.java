/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.serialization;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.TypeFactory;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class TypeFactorySerializationTest extends BaseUnitTestCase {
	private static String NAME = "test name";

	@Test
	public void testTypeFactoryDeserializedAfterSessionFactoryClosed() {
		Configuration cfg = new Configuration()
				.setProperty( AvailableSettings.SESSION_FACTORY_NAME, NAME )
				.setProperty( AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, "false" ); // default is true
		SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			// SessionFactory is registered.
			assertSame( factory, SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( NAME ) );

			// get reference to the TypeFactory and serialize it
			TypeFactory typeFactory = factory.getTypeResolver().getTypeFactory();
			byte[] typeFactoryBytes = SerializationHelper.serialize( typeFactory );

			// close the SessionFactory
			factory.close();

			// now try to deserialize the TypeFactory.. it should fail
			try {
				typeFactory = (TypeFactory) SerializationHelper.deserialize( typeFactoryBytes );
				fail();
			}
			catch (HibernateException expected) {
			}
		}
		finally {
			if ( factory != null && factory.isOpen() ) {
				factory.close();
			}
		}
	}
}
