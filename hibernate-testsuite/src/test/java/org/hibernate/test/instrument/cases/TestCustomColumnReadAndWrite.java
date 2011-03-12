package org.hibernate.test.instrument.cases;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.hibernate.Hibernate;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.test.instrument.domain.Document;
import org.hibernate.test.instrument.domain.Folder;
import org.hibernate.test.instrument.domain.Owner;

/**
 * @author Rob.Hasselbaum
 */
public class TestCustomColumnReadAndWrite extends AbstractExecutable {
	public void execute() {
		Session s = getFactory().openSession();
		Transaction t = s.beginTransaction();
		final double SIZE_IN_KB = 20480;
		final double SIZE_IN_MB = SIZE_IN_KB / 1024d;
		Owner o = new Owner();
		Document doc = new Document();
		Folder fol = new Folder();
		o.setName("gavin");
		doc.setName("Hibernate in Action");
		doc.setSummary("blah");
		doc.updateText("blah blah");	
		fol.setName("books");
		doc.setOwner(o);
		doc.setFolder(fol);
		doc.setSizeKb(SIZE_IN_KB);
		fol.getDocuments().add(doc);
		s.persist(o);
		s.persist(fol);
		t.commit();
		s.close();

		s = getFactory().openSession();
		t = s.beginTransaction();
		
		// Check value conversion on insert
		Double sizeViaSql = (Double)s.createSQLQuery("select size_mb from documents").uniqueResult();
		assertEquals( SIZE_IN_MB, sizeViaSql, 0.01d );

		// Test explicit fetch of all properties
		doc = (Document) s.createQuery("from Document fetch all properties").uniqueResult();
		assertTrue( Hibernate.isPropertyInitialized( doc, "sizeKb" ) );
		assertEquals( SIZE_IN_KB, doc.getSizeKb() );
		t.commit();
		s.close();		

		// Test lazy fetch with custom read
		s = getFactory().openSession();
		t = s.beginTransaction();
		doc = (Document) s.get( Document.class, doc.getId() );
		assertFalse( Hibernate.isPropertyInitialized( doc, "sizeKb" ) );
		assertEquals( SIZE_IN_KB, doc.getSizeKb() );
		s.delete(doc);
		s.delete( doc.getOwner() );
		s.delete( doc.getFolder() );
		t.commit();
		s.close();
	}
}
