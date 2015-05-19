/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.cases;
import junit.framework.Assert;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.instrument.domain.Document;
import org.hibernate.test.instrument.domain.Folder;
import org.hibernate.test.instrument.domain.Owner;

/**
 * @author Steve Ebersole
 */
public class TestLazyManyToOneExecutable extends AbstractExecutable {
	public void execute() {
		Session s = getFactory().openSession();
		Transaction t = s.beginTransaction();
		Owner gavin = new Owner();
		Document hia = new Document();
		Folder fol = new Folder();
		gavin.setName("gavin");
		hia.setName("Hibernate in Action");
		hia.setSummary("blah");
		hia.updateText("blah blah");
		fol.setName("books");
		hia.setOwner(gavin);
		hia.setFolder(fol);
		fol.getDocuments().add(hia);
		s.persist(gavin);
		s.persist(fol);
		t.commit();
		s.close();

		s = getFactory().openSession();
		t = s.beginTransaction();
		hia = (Document) s.createCriteria(Document.class).uniqueResult();
		Assert.assertEquals( hia.getFolder().getClass(), Folder.class);
		fol = hia.getFolder();
		Assert.assertTrue( Hibernate.isInitialized(fol) );
		t.commit();
		s.close();

		s = getFactory().openSession();
		t = s.beginTransaction();
		hia = (Document) s.createCriteria(Document.class).uniqueResult();
		Assert.assertSame( hia.getFolder(), s.load(Folder.class, fol.getId()) );
		Assert.assertTrue( Hibernate.isInitialized( hia.getFolder() ) );
		t.commit();
		s.close();

		s = getFactory().openSession();
		t = s.beginTransaction();
		fol = (Folder) s.get(Folder.class, fol.getId());
		hia = (Document) s.createCriteria(Document.class).uniqueResult();
		Assert.assertSame( fol, hia.getFolder() );
		fol = hia.getFolder();
		Assert.assertTrue( Hibernate.isInitialized(fol) );
		t.commit();
		s.close();

		s = getFactory().openSession();
		t = s.beginTransaction();
		fol = (Folder) s.load(Folder.class, fol.getId());
		hia = (Document) s.createCriteria(Document.class).uniqueResult();
		Assert.assertNotSame( fol, hia.getFolder() );
		fol = hia.getFolder();
		Assert.assertTrue( Hibernate.isInitialized(fol) );
		s.delete(hia.getFolder());
		s.delete(hia.getOwner());
		t.commit();
		s.close();
	}
}
