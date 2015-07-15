package org.hibernate.test.instrument.cases;
import org.junit.Assert;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.instrument.domain.Document;
import org.hibernate.test.instrument.domain.Folder;
import org.hibernate.test.instrument.domain.Owner;

/**
 * @author Andrei Ivanov
 */
public class TestLazyBasicPropertyAccessExecutable extends AbstractExecutable {
	protected String[] getResources() {
		return new String[] {"org/hibernate/test/instrument/domain/DocumentsPropAccess.hbm.xml"};
	}

	public void execute() {
		Session s = getFactory().openSession();
		Transaction t = s.beginTransaction();
		Owner o = new Owner();
		Document doc = new Document();
		Folder fol = new Folder();
		o.setName( "gavin" );
		doc.setName( "Hibernate in Action" );
		doc.setSummary( "blah" );
		doc.updateText( "blah blah" );
		fol.setName( "books" );
		doc.setOwner( o );
		doc.setFolder( fol );
		fol.getDocuments().add( doc );
		Assert.assertTrue( Hibernate.isPropertyInitialized( doc, "summary" ) );
		s.persist( o );
		s.persist( fol );
		t.commit();
		s.close();

		s = getFactory().openSession();
		s.getTransaction().begin();
		// update with lazy property initialized
		doc.setName( "Doc Name" );
		doc.setSummary( "u" );
		s.update( doc );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.getTransaction().begin();
		// merge with lazy property initialized and updated
		doc.setName( "Doc Name 1" );
		doc.setSummary( "v" );
		Document docManaged = (Document) s.merge( doc );
		Assert.assertEquals( "v", docManaged.getSummary() );
		Assert.assertTrue( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.getTransaction().begin();
		// get the Document with an uninitialized summary
		docManaged = (Document) s.get( Document.class, doc.getId() );
		Assert.assertFalse( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		// merge with lazy property initialized in doc; uninitialized in docManaged.
		doc.setSummary( "w" );
		Assert.assertSame( docManaged, s.merge( doc ) );
		Assert.assertEquals( "w", docManaged.getSummary() );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.getTransaction().begin();
		// get the Document with an uninitialized summary
		docManaged = (Document) s.get( Document.class, doc.getId() );
		Assert.assertFalse( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		// initialize docManaged.getSummary
		Assert.assertEquals( "w", docManaged.getSummary() );
		Assert.assertTrue( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		// merge with lazy property initialized in both doc and docManaged.
		doc.setSummary( "x" );
		Assert.assertSame( docManaged, s.merge( doc ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		Assert.assertEquals( "x", docManaged.getSummary() );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.getTransaction().begin();
		// get the Document with an uninitialized summary
		Document docWithLazySummary = (Document) s.get( Document.class, doc.getId() );
		Assert.assertFalse( Hibernate.isPropertyInitialized( docWithLazySummary, "summary" ) );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.getTransaction().begin();
		// summary should still be uninitialized.
		Assert.assertFalse( Hibernate.isPropertyInitialized( docWithLazySummary, "summary" ) );
		docWithLazySummary.setName( "new name" );
		// merge the Document with an uninitialized summary
		docManaged = (Document) s.merge( docWithLazySummary );
		Assert.assertFalse( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.getTransaction().begin();
		// get the Document with an uninitialized summary
		docManaged = (Document) s.get( Document.class, doc.getId() );
		Assert.assertFalse( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		// initialize docManaged.getSummary
		Assert.assertEquals( "x", docManaged.getSummary() );
		Assert.assertTrue( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		// merge the Document with an uninitialized summary
		Assert.assertFalse( Hibernate.isPropertyInitialized( docWithLazySummary, "summary" ) );
		docManaged = (Document) s.merge( docWithLazySummary );
		Assert.assertTrue( Hibernate.isPropertyInitialized( docManaged, "summary" ) );
		Assert.assertEquals( "x", docManaged.getSummary() );
		s.getTransaction().commit();
		s.close();
	}
}
