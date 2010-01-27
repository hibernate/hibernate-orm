package org.hibernate.test.annotations.onetoone.hhh4851;

import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class HHH4851Test extends TestCase {

	public void testHHH4851() throws Exception {
		Session session = openSession();
		Transaction trx = session.beginTransaction();
		Owner org = new Owner();
		org.setName( "root" );
		session.saveOrUpdate( org );

		ManagedDevice lTerminal = new ManagedDevice();
		lTerminal.setName( "test" );
		lTerminal.setOwner( org );
		session.saveOrUpdate( lTerminal );

		Device terminal = new Device();
		terminal.setTag( "test" );
		terminal.setOwner( org );
		try {
			session.saveOrUpdate( terminal );
		}
		catch ( PropertyValueException e ) {
			fail( "not-null checking should not be raised: " + e.getMessage() );
		}
		trx.commit();
		session.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CHECK_NULLABILITY, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Hardware.class,
				DeviceGroupConfig.class,
				Hardware.class,
				ManagedDevice.class,
				Device.class,
				Owner.class
		};
	}
}
