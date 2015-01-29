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
public class TestLazyBasicPropertyUpdateExecutable extends AbstractExecutable {
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
		Assert.assertTrue( Hibernate.isPropertyInitialized( doc, "summary" ) );
		s.persist(o);
		s.persist(fol);
		t.commit();

		s.evict(doc);

		doc.setSummary("u");
		s.merge(doc);
		
		s.close();
	}
}
