package org.hibernate.test.instrument.cases;
import junit.framework.Assert;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.instrument.domain.EntityWithOneToOnes;
import org.hibernate.test.instrument.domain.OneToOneNoProxy;
import org.hibernate.test.instrument.domain.OneToOneProxy;

/**
 *
 * @author Gail Badner
 */
public class TestSharedPKOneToOneExecutable extends AbstractExecutable {

	protected String[] getResources() {
		return new String[] {"org/hibernate/test/instrument/domain/SharedPKOneToOne.hbm.xml"};
	}

	public void execute() {
		Session s = getFactory().openSession();
		Transaction t = s.beginTransaction();
		EntityWithOneToOnes root = new EntityWithOneToOnes( "root" );
		OneToOneProxy oneToOneProxy = new OneToOneProxy( "oneToOneProxy" );
		root.setOneToOneProxy( oneToOneProxy );
		oneToOneProxy.setEntity( root );
		OneToOneNoProxy oneToOneNoProxy = new OneToOneNoProxy( "oneToOneNoProxy" );
		root.setOneToOneNoProxy( oneToOneNoProxy );
		oneToOneNoProxy.setEntity( root );

		s.save( root );
		t.commit();
		s.close();

		// NOTE : oneToOneProxy is mapped with lazy="proxy"; oneToOneNoProxy with lazy="no-proxy"...

		s = getFactory().openSession();
		t = s.beginTransaction();
		// load root
		root = ( EntityWithOneToOnes ) s.load( EntityWithOneToOnes.class, root.getId() );
		Assert.assertFalse( Hibernate.isInitialized( root ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "name" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "oneToOneProxy" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "oneToOneNoProxy" ) );

		root.getName();
		Assert.assertTrue( Hibernate.isInitialized( root ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( root, "name" ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( root, "oneToOneProxy" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "oneToOneNoProxy" ) );

		// get a handle to the oneToOneProxy proxy reference (and make certain that
		// this does not force the lazy properties of the root entity
		// to get initialized.
		root.getOneToOneProxy();
		Assert.assertTrue( Hibernate.isInitialized( oneToOneProxy ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( root.getOneToOneProxy(), "name" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "oneToOneNoProxy" ) );

		root.getOneToOneNoProxy();
		Assert.assertTrue( Hibernate.isPropertyInitialized( root, "oneToOneNoProxy" ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( root.getOneToOneNoProxy(), "name") );

		s.delete( root );
		t.commit();
		s.close();
	}
}
