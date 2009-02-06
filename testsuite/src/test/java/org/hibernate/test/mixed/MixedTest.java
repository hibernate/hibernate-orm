//$Id: MixedTest.java 15736 2008-12-27 00:49:42Z gbadner $
package org.hibernate.test.mixed;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class MixedTest extends FunctionalTestCase {

	public MixedTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[]{"mixed/Item.hbm.xml"};
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MixedTest.class );
	}

	public void testMixedInheritance() {
		Session s = openSession( new DocumentInterceptor() );
		Transaction t = s.beginTransaction();
		Folder f = new Folder();
		f.setName( "/" );
		s.save( f );

		Document d = new Document();
		d.setName( "Hibernate in Action" );
		d.setContent( Hibernate.createBlob( "blah blah blah".getBytes() ) );
		d.setParent( f );
		Long did = (Long) s.save( d );

		SecureDocument d2 = new SecureDocument();
		d2.setName( "Secret" );
		d2.setContent( Hibernate.createBlob( "wxyz wxyz".getBytes() ) );
		// SybaseASE15Dialect only allows 7-bits in a byte to be inserted into a tinyint 
		// column (0 <= val < 128)
		d2.setPermissionBits( (byte) 127 );
		d2.setOwner( "gavin" );
		d2.setParent( f );
		Long d2id = (Long) s.save( d2 );

		t.commit();
		s.close();

		if ( ! getDialect().supportsExpectedLobUsagePattern() ) {
			reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return;
		}

		s = openSession( new DocumentInterceptor() );
		t = s.beginTransaction();
		Item id = (Item) s.load( Item.class, did );
		assertEquals( did, id.getId() );
		assertEquals( "Hibernate in Action", id.getName() );
		assertEquals( "/", id.getParent().getName() );

		Item id2 = (Item) s.load( Item.class, d2id );
		assertEquals( d2id, id2.getId() );
		assertEquals( "Secret", id2.getName() );
		assertEquals( "/", id2.getParent().getName() );

		id.setName( "HiA" );

		d2 = (SecureDocument) s.load( SecureDocument.class, d2id );
		d2.setOwner( "max" );

		s.flush();

		s.clear();

		d = (Document) s.load( Document.class, did );
		assertEquals( did, d.getId() );
		assertEquals( "HiA", d.getName() );
		assertNotNull( d.getContent() );
		assertEquals( "/", d.getParent().getName() );
		assertNotNull( d.getCreated() );
		assertNotNull( d.getModified() );

		d2 = (SecureDocument) s.load( SecureDocument.class, d2id );
		assertEquals( d2id, d2.getId() );
		assertEquals( "Secret", d2.getName() );
		assertNotNull( d2.getContent() );
		assertEquals( "max", d2.getOwner() );
		assertEquals( "/", d2.getParent().getName() );
		// SybaseASE15Dialect only allows 7-bits in a byte to be inserted into a tinyint 
		// column (0 <= val < 128)
		assertEquals( (byte) 127, d2.getPermissionBits() );
		assertNotNull( d2.getCreated() );
		assertNotNull( d2.getModified() );

		s.delete( d.getParent() );
		s.delete( d );
		s.delete( d2 );

		t.commit();
		s.close();
	}
}

