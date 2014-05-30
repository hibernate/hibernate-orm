/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.stateless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.util.Date;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gavin King
 */
public class StatelessSessionTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "stateless/Document.hbm.xml" };
	}

	@Test
	public void testCreateUpdateReadDelete() {
		StatelessSession ss = sessionFactory().openStatelessSession();
		Transaction tx = ss.beginTransaction();
		Document doc = new Document("blah blah blah", "Blahs");
		ss.insert(doc);
		assertNotNull( doc.getName() );
		Date initVersion = doc.getLastModified();
		assertNotNull( initVersion );
		tx.commit();
		
		tx = ss.beginTransaction();
		doc.setText("blah blah blah .... blah");
		ss.update(doc);
		assertNotNull( doc.getLastModified() );
		assertNotSame( doc.getLastModified(), initVersion );
		tx.commit();
		
		tx = ss.beginTransaction();
		doc.setText("blah blah blah .... blah blay");
		ss.update(doc);
		tx.commit();
		
		Document doc2 = (Document) ss.get(Document.class.getName(), "Blahs");
		assertEquals("Blahs", doc2.getName());
		assertEquals(doc.getText(), doc2.getText());
				
		doc2 = (Document) ss.createQuery("from Document where text is not null").uniqueResult();
		assertEquals("Blahs", doc2.getName());
		assertEquals(doc.getText(), doc2.getText());
		
		ScrollableResults sr = ss.createQuery("from Document where text is not null")
			.scroll(ScrollMode.FORWARD_ONLY);
		sr.next();
		doc2 = (Document) sr.get(0);
		sr.close();
		assertEquals("Blahs", doc2.getName());
		assertEquals(doc.getText(), doc2.getText());
				
		doc2 = (Document) ss.createSQLQuery("select * from Document")
			.addEntity(Document.class)
			.uniqueResult();
		assertEquals("Blahs", doc2.getName());
		assertEquals(doc.getText(), doc2.getText());
				
		doc2 = (Document) ss.createCriteria(Document.class).uniqueResult();
		assertEquals("Blahs", doc2.getName());
		assertEquals(doc.getText(), doc2.getText());
		
		sr = ss.createCriteria(Document.class).scroll(ScrollMode.FORWARD_ONLY);
		sr.next();
		doc2 = (Document) sr.get(0);
		sr.close();
		assertEquals("Blahs", doc2.getName());
		assertEquals(doc.getText(), doc2.getText());

		tx = ss.beginTransaction();
		ss.delete(doc);
		tx.commit();
		ss.close();
	}

	@Test
	public void testHqlBulk() {
		StatelessSession ss = sessionFactory().openStatelessSession();
		Transaction tx = ss.beginTransaction();
		Document doc = new Document("blah blah blah", "Blahs");
		ss.insert(doc);
		Paper paper = new Paper();
		paper.setColor( "White" );
		ss.insert(paper);
		tx.commit();

		tx = ss.beginTransaction();
		int count = ss.createQuery( "update Document set name = :newName where name = :oldName" )
				.setString( "newName", "Foos" )
				.setString( "oldName", "Blahs" )
				.executeUpdate();
		assertEquals( "hql-update on stateless session", 1, count );
		count = ss.createQuery( "update Paper set color = :newColor" )
				.setString( "newColor", "Goldenrod" )
				.executeUpdate();
		assertEquals( "hql-update on stateless session", 1, count );
		tx.commit();

		tx = ss.beginTransaction();
		count = ss.createQuery( "delete Document" ).executeUpdate();
		assertEquals( "hql-delete on stateless session", 1, count );
		count = ss.createQuery( "delete Paper" ).executeUpdate();
		assertEquals( "hql-delete on stateless session", 1, count );
		tx.commit();
		ss.close();
	}

	@Test
	public void testInitId() {
		StatelessSession ss = sessionFactory().openStatelessSession();
		Transaction tx = ss.beginTransaction();
		Paper paper = new Paper();
		paper.setColor( "White" );
		ss.insert(paper);
		assertNotNull( paper.getId() );
		tx.commit();

		tx = ss.beginTransaction();
		ss.delete( ss.get( Paper.class, paper.getId() ) );
		tx.commit();
		ss.close();
	}

	@Test
	public void testRefresh() {
		StatelessSession ss = sessionFactory().openStatelessSession();
		Transaction tx = ss.beginTransaction();
		Paper paper = new Paper();
		paper.setColor( "whtie" );
		ss.insert( paper );
		tx.commit();
		ss.close();

		ss = sessionFactory().openStatelessSession();
		tx = ss.beginTransaction();
		Paper p2 = ( Paper ) ss.get( Paper.class, paper.getId() );
		p2.setColor( "White" );
		ss.update( p2 );
		tx.commit();
		ss.close();

		ss = sessionFactory().openStatelessSession();
		tx = ss.beginTransaction();
		assertEquals( "whtie", paper.getColor() );
		ss.refresh( paper );
		assertEquals( "White", paper.getColor() );
		ss.delete( paper );
		tx.commit();
		ss.close();
	}

}

