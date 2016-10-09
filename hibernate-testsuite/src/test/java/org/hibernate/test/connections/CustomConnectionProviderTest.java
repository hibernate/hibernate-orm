package org.hibernate.test.connections;

import junit.framework.Assert;
import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.test.id.Car;
import org.hibernate.testing.junit.UnitTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CustomConnectionProviderTest extends UnitTestCase {
	private UserSuppliedConnectionProvider cp;
	private SessionFactoryImplementor sessionFactory;

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CustomConnectionProviderTest.class );
	}

	public CustomConnectionProviderTest(String string) {
		super( string );
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		cp = new UserSuppliedConnectionProvider( ConnectionProviderFactory.newConnectionProvider(), "mySecretData" );
		ConnectionProviderFactory.registerConnectionProviderInstance( cp );

		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.DIALECT, Dialect.getDialect().getClass().getName() );
		cfg.setProperty( Environment.CONNECTION_PROVIDER, cp.getClass().getName() );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		cfg.addResource( "org/hibernate/test/id/Car.hbm.xml" );
		cfg.buildMappings();

		sessionFactory = (SessionFactoryImplementor) cfg.buildSessionFactory();
	}

	public void testSuppliedConnectionProviderUsage() {
		Assert.assertEquals( cp, sessionFactory.getConnectionProvider() );

		Session session = sessionFactory.openSession();
		session.beginTransaction();
		Car car = new Car();
		car.setColor( "black" );
		session.save( car );
		session.getTransaction().commit();
		session.close();

		Assert.assertEquals( "mySecretData", cp.getMyData() );
		Assert.assertTrue( cp.getInvocationCount() > 0 );
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}
}
