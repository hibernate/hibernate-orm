//$Id$
package org.hibernate.test.annotations.identifiercollection;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;


/**
 * @author Emmanuel Bernard
 */
public class IdentifierCollectionTest extends TestCase {
	public void testIdBag() throws Exception {
		Passport passport = new Passport();
		passport.setName( "Emmanuel Bernard" );
		Stamp canada = new Stamp();
		canada.setCountry( "Canada" );
		passport.getStamps().add( canada );
		passport.getVisaStamp().add( canada );
		Stamp norway = new Stamp();
		norway.setCountry( "Norway" );
		passport.getStamps().add( norway );
		passport.getStamps().add(canada);
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( passport );
		s.flush();
		//s.clear();
		passport = (Passport) s.get( Passport.class, passport.getId() );
		int canadaCount = 0;
		for ( Stamp stamp : passport.getStamps() ) {
			if ( "Canada".equals( stamp.getCountry() ) ) canadaCount++;
		}
		assertEquals( 2, canadaCount );
		assertEquals( 1, passport.getVisaStamp().size() );
		tx.rollback();
		s.close();
	}
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Passport.class,
				Stamp.class
		};
	}
}
