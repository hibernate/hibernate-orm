/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.cases;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.instrument.domain.Folder;

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
