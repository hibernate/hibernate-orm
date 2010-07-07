//$Id: InterfaceProxyTest.java 15736 2008-12-27 00:49:42Z gbadner $
package org.hibernate.test.interfaceproxy;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class InterfaceProxyTest extends FunctionalTestCase {
	
	public InterfaceProxyTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "interfaceproxy/Item.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( InterfaceProxyTest.class );
	}
	
	public void testInterfaceProxies() {
		
		if ( ! getDialect().supportsExpectedLobUsagePattern() ) {
			reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return;
		}
		
		Session s = openSession( new DocumentInterceptor() );
		Transaction t = s.beginTransaction();
		Document d = new DocumentImpl();
		d.setName("Hibernate in Action");
		d.setContent( s.getLobHelper().createBlob( "blah blah blah".getBytes() ) );
		Long did = (Long) s.save(d);
		SecureDocument d2 = new SecureDocumentImpl();
		d2.setName("Secret");
		d2.setContent( s.getLobHelper().createBlob( "wxyz wxyz".getBytes() ) );
		// SybaseASE15Dialect only allows 7-bits in a byte to be inserted into a tinyint 
		// column (0 <= val < 128)		
		d2.setPermissionBits( (byte) 127 );
		d2.setOwner("gavin");
		Long d2id = (Long) s.save(d2);
		t.commit();
		s.close();

		s = openSession( new DocumentInterceptor() );
		t = s.beginTransaction();
		d = (Document) s.load(ItemImpl.class, did);
		assertEquals( did, d.getId() );
		assertEquals( "Hibernate in Action", d.getName() );
		assertNotNull( d.getContent() );
		
		d2 = (SecureDocument) s.load(ItemImpl.class, d2id);
		assertEquals( d2id, d2.getId() );
		assertEquals( "Secret", d2.getName() );
		assertNotNull( d2.getContent() );
		
		s.clear();
		
		d = (Document) s.load(DocumentImpl.class, did);
		assertEquals( did, d.getId() );
		assertEquals( "Hibernate in Action", d.getName() );
		assertNotNull( d.getContent() );
		
		d2 = (SecureDocument) s.load(SecureDocumentImpl.class, d2id);
		assertEquals( d2id, d2.getId() );
		assertEquals( "Secret", d2.getName() );
		assertNotNull( d2.getContent() );
		assertEquals( "gavin", d2.getOwner() );
		
		//s.clear();
		
		d2 = (SecureDocument) s.load(SecureDocumentImpl.class, did);
		assertEquals( did, d2.getId() );
		assertEquals( "Hibernate in Action", d2.getName() );
		assertNotNull( d2.getContent() );
		
		try {
			d2.getOwner(); //CCE
			assertFalse(true);
		}
		catch (ClassCastException cce) {
			//correct
		}

		s.createQuery( "delete ItemImpl" ).executeUpdate();
		t.commit();
		s.close();
	}
}

