//$Id$
package org.hibernate.test.annotations.inheritance.joined;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassTest extends TestCase {

	public JoinedSubclassTest(String x) {
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
		tx.commit();
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

	public void testManyToOneOnAbstract() throws Exception {
		Folder f = new Folder();
		f.setName( "data" );
		ProgramExecution remove = new ProgramExecution();
		remove.setAction( "remove" );
		remove.setAppliesOn( f );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( f );
		s.persist( remove );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		remove = (ProgramExecution) s.get( ProgramExecution.class, remove.getId() );
		assertNotNull( remove );
		assertNotNull( remove.getAppliesOn().getName() );
		s.delete( remove );
		s.delete( remove.getAppliesOn() );
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

	public void testJoinedAbstractClass() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		s.getTransaction().begin();
		Sweater sw = new Sweater();
		sw.setColor( "Black" );
		sw.setSize( 2 );
		sw.setSweat( true );
		s.persist( sw );
		s.getTransaction().commit();
		s.clear();

		s = openSession();
		s.getTransaction().begin();
		sw = (Sweater) s.get( Sweater.class, sw.getId() );
		s.delete( sw );
		s.getTransaction().commit();
		s.close();
	}

	public void testInheritance() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		String eventPK = "event1";
		EventInformation event = (EventInformation) session.get( EventInformation.class, eventPK );
		if ( event == null ) {
			event = new EventInformation();
			event.setNotificationId( eventPK );
			session.persist( event );
		}
		String alarmPK = "alarm1";
		Alarm alarm = (Alarm) session.get( Alarm.class, alarmPK );
		if ( alarm == null ) {
			alarm = new Alarm();
			alarm.setNotificationId( alarmPK );
			alarm.setEventInfo( event );
			session.persist( alarm );
		}
		transaction.commit();
		session.close();
	}

//	public void testManyToOneAndJoin() throws Exception {
//		Session session = openSession();
//		Transaction transaction = session.beginTransaction();
//		Parent parent = new Parent();
//		session.persist( parent );
//		PropertyAsset property = new PropertyAsset();
//		property.setParent( parent );
//		property.setPrice( 230000d );
//		FinancialAsset financial = new FinancialAsset();
//		financial.setParent( parent );
//		financial.setPrice( 230000d );
//		session.persist( financial );
//		session.persist( property );
//		session.flush();
//		session.clear();
//		parent = (Parent) session.get( Parent.class, parent.getId() );
//		assertNotNull( parent );
//		assertEquals( 1, parent.getFinancialAssets().size() );
//		assertEquals( 1, parent.getPropertyAssets().size() );
//		assertEquals( property.getId(), parent.getPropertyAssets().iterator().next() );
//		transaction.rollback();
//		session.close();
//	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] {
				//"org/hibernate/test/annotations/inheritance/joined/Asset.hbm.xml"
		};
	}

	/**
	 * @see org.hibernate.test.annotations.TestCase#getMappings()
	 */
	protected Class[] getMappings() {
		return new Class[]{
				File.class,
				Folder.class,
				Document.class,
				SymbolicLink.class,
				ProgramExecution.class,
				Clothing.class,
				Sweater.class,
				EventInformation.class,
				Alarm.class,
				//Asset.class,
				//Parent.class,
				//PropertyAsset.class,
				//FinancialAsset.class
		};
	}

}
