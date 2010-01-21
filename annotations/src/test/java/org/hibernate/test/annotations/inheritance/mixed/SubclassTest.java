//$Id$
package org.hibernate.test.annotations.inheritance.mixed;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class SubclassTest extends TestCase {

	public SubclassTest(String x) {
		super( x );
	}

	public void testDefault() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		File doc = new Document( "Enron Stuff To Shred", 1000 );
		Folder folder = new Folder( "Enron" );
		s.persist( doc );
		s.persist( folder );
		try {
			tx.commit();
		}
		catch (SQLGrammarException e) {
			System.err.println( e.getSQLException().getNextException() );
		}
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		List result = s.createCriteria( File.class ).list();
		assertNotNull( result );
		assertEquals( 2, result.size() );
		File f2 = (File) result.get( 0 );
		checkClassType( f2, doc, folder );
		f2 = (File) result.get( 1 );
		checkClassType( f2, doc, folder );
		s.delete( result.get( 0 ) );
		s.delete( result.get( 1 ) );
		tx.commit();
		s.close();
	}

	private void checkClassType(File fruitToTest, File f, Folder a) {
		if ( fruitToTest.getName().equals( f.getName() ) ) {
			assertFalse( fruitToTest instanceof Folder );
		}
		else if ( fruitToTest.getName().equals( a.getName() ) ) {
			assertTrue( fruitToTest instanceof Folder );
		}
		else {
			fail( "Result does not contains the previously inserted elements" );
		}
	}

	/**
	 * @see org.hibernate.test.annotations.TestCase#getAnnotatedClasses()
	 */
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				File.class,
				Folder.class,
				Document.class,
				SymbolicLink.class
		};
	}

}
