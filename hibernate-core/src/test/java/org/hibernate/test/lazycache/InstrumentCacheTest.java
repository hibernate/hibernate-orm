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
package org.hibernate.test.lazycache;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.Skip;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
@Skip( condition = InstrumentCacheTest.SkipMatcher.class, message = "Test domain classes not instrumented" )
public class InstrumentCacheTest extends BaseCoreFunctionalTestCase {
	public static class SkipMatcher implements Skip.Matcher {
		@Override
		public boolean isMatch() {
			return ! FieldInterceptionHelper.isInstrumented( Document.class );
		}
	}

	public String[] getMappings() {
		return new String[] { "lazycache/Documents.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	public boolean overrideCacheStrategy() {
		return false;
	}

	@Test
	public void testInitFromCache() {
		Session s;
		Transaction tx;

		s = sessionFactory().openSession();
		tx = s.beginTransaction();
		s.persist( new Document("HiA", "Hibernate book", "Hibernate is....") );
		tx.commit();
		s.close();

		s = sessionFactory().openSession();
		tx = s.beginTransaction();
		s.createQuery("from Document fetch all properties").uniqueResult();
		tx.commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = sessionFactory().openSession();
		tx = s.beginTransaction();
		Document d = (Document) s.createCriteria(Document.class).uniqueResult();
		assertFalse( Hibernate.isPropertyInitialized(d, "text") );
		assertFalse( Hibernate.isPropertyInitialized(d, "summary") );
		assertEquals( "Hibernate is....", d.getText() );
		assertTrue( Hibernate.isPropertyInitialized(d, "text") );
		assertTrue( Hibernate.isPropertyInitialized(d, "summary") );
		tx.commit();
		s.close();

		assertEquals( 2, sessionFactory().getStatistics().getPrepareStatementCount() );

		s = sessionFactory().openSession();
		tx = s.beginTransaction();
		d = (Document) s.get(Document.class, d.getId());
		assertFalse( Hibernate.isPropertyInitialized(d, "text") );
		assertFalse( Hibernate.isPropertyInitialized(d, "summary") );
		tx.commit();
		s.close();
	}

	@Test
	public void testInitFromCache2() {
		Session s;
		Transaction tx;

		s = sessionFactory().openSession();
		tx = s.beginTransaction();
		s.persist( new Document("HiA", "Hibernate book", "Hibernate is....") );
		tx.commit();
		s.close();

		s = sessionFactory().openSession();
		tx = s.beginTransaction();
		s.createQuery("from Document fetch all properties").uniqueResult();
		tx.commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = sessionFactory().openSession();
		tx = s.beginTransaction();
		Document d = (Document) s.createCriteria(Document.class).uniqueResult();
		assertFalse( Hibernate.isPropertyInitialized(d, "text") );
		assertFalse( Hibernate.isPropertyInitialized(d, "summary") );
		assertEquals( "Hibernate is....", d.getText() );
		assertTrue( Hibernate.isPropertyInitialized(d, "text") );
		assertTrue( Hibernate.isPropertyInitialized(d, "summary") );
		tx.commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getPrepareStatementCount() );

		s = sessionFactory().openSession();
		tx = s.beginTransaction();
		d = (Document) s.get(Document.class, d.getId());
		assertTrue( Hibernate.isPropertyInitialized(d, "text") );
		assertTrue( Hibernate.isPropertyInitialized(d, "summary") );
		tx.commit();
		s.close();
	}

}

