package org.hibernate.test.instrument.cases;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.instrument.domain.Folder;

import java.util.List;
import java.util.Iterator;

import junit.framework.Assert;

/**
 * @author Steve Ebersole
 */
public class TestDirtyCheckExecutable extends AbstractExecutable {
	public void execute() {
		Session s = getFactory().openSession();
		Transaction t = s.beginTransaction();
		Folder pics = new Folder();
		pics.setName("pics");
		Folder docs = new Folder();
		docs.setName("docs");
		s.persist(docs);
		s.persist(pics);
		t.commit();
		s.close();

		s = getFactory().openSession();
		t = s.beginTransaction();
		List list = s.createCriteria(Folder.class).list();
		for ( Iterator iter = list.iterator(); iter.hasNext(); ) {
			Folder f = (Folder) iter.next();
			Assert.assertFalse( f.nameWasread );
		}
		t.commit();
		s.close();

		for ( Iterator iter = list.iterator(); iter.hasNext(); ) {
			Folder f = (Folder) iter.next();
			Assert.assertFalse( f.nameWasread );
		}

		s = getFactory().openSession();
		t = s.beginTransaction();
		s.createQuery("delete from Folder").executeUpdate();
		t.commit();
		s.close();
	}
}
