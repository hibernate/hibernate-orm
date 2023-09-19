/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Marcel Overdijk
 */
public class MultipleSessionFactoriesProxyTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] { "proxy/DataPoint.hbm.xml" };
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.SESSION_FACTORY_NAME, "sf-name" ); // explicitly define the session factory name
		cfg.setProperty( Environment.SESSION_FACTORY_NAME_IS_JNDI, "false" ); // do not bind it to jndi
	}

	@Test @TestForIssue(jiraKey="HHH-17172")
	public void testProxySerializationWithMultipleSessionFactories() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Container container = new Container( "container" );
		container.setOwner( new Owner( "owner" ) );
		container.setInfo( new Info( "blah blah blah" ) );
		container.getDataPoints().add( new DataPoint( new BigDecimal( 1 ), new BigDecimal( 1 ), "first data point" ) );
		container.getDataPoints().add( new DataPoint( new BigDecimal( 2 ), new BigDecimal( 2 ), "second data point" ) );
		s.persist(container);
		s.flush();
		s.clear();

		container = s.load( Container.class, container.getId());
		assertFalse( Hibernate.isInitialized(container) );
		container.getId();
		assertFalse( Hibernate.isInitialized(container) );
		container.getName();
		assertTrue( Hibernate.isInitialized(container) );

		t.commit();
		s.close();

		// Serialize the container.
		byte[] bytes = SerializationHelper.serialize(container);

		// Now deserialize, which works at this stage, because the session factory UUID in the
		// serialized object is the same as the current session factory.
		SerializationHelper.deserialize(bytes);

		// Now rebuild the session factory (it will get a new unique UUID).
		// As configured for this test an explicit session factory name is used ("sf-name"),
		// so we would expect the deserialization to work after rebuilding the session factory.
		// Rebuilding the session factory simulates multiple JVM instances with different session
		// factories (different UUID's, but same name!) trying to deserialize objects from a
		// centralized cache (e.g. Hazelcast).
		rebuildSessionFactory();

		// But this fails with:
		// java.lang.IllegalStateException: Could not identify any active SessionFactory having UUID 6a97f04e-1aeb-4790-9de6-19ee1e069bab
		// As it cannot retrieve a matching session factory by the UUID.
		// However, it should internally use the session factory name and not the UUID.
		SerializationHelper.deserialize(bytes);

	}
}
