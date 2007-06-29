package org.hibernate.test.instrument.cases;

import org.hibernate.Session;
import org.hibernate.Hibernate;
import org.hibernate.Transaction;
import org.hibernate.test.instrument.domain.Owner;
import org.hibernate.test.instrument.domain.Document;
import org.hibernate.test.instrument.domain.Folder;
import junit.framework.Assert;

/**
 * @author Steve Ebersole
 */
public class TestFetchAllExecutable extends AbstractExecutable {
	public void execute() {
		Session s = getFactory().openSession();
		Transaction t = s.beginTransaction();
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
		fol.getDocuments().add(doc);
		s.persist(o);
		s.persist(fol);
		t.commit();
		s.close();

		s = getFactory().openSession();
		t = s.beginTransaction();
		doc = (Document) s.createQuery("from Document fetch all properties").uniqueResult();
		Assert.assertTrue( Hibernate.isPropertyInitialized( doc, "summary" ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( doc, "upperCaseName" ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( doc, "owner" ) );
		Assert.assertEquals( doc.getSummary(), "blah" );
		s.delete(doc);
		s.delete( doc.getOwner() );
		s.delete( doc.getFolder() );
		t.commit();
		s.close();
	}
}
