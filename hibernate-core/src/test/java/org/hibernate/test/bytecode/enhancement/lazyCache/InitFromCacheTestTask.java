/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazyCache;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.cache.BaseRegion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class InitFromCacheTestTask extends AbstractCachingTestTask {
	@Override
	public void execute() {
		EntityPersister p = sessionFactory().getEntityPersister( Document.class.getName() );
		assertTrue( p.hasCache() );
		BaseRegion region = (BaseRegion) p.getCacheAccessStrategy().getRegion();

		Session s = sessionFactory().openSession();
		s.beginTransaction();
		s.persist( new Document("HiA", "Hibernate book", "Hibernate is....") );
		s.getTransaction().commit();
		s.close();

		s = sessionFactory().openSession();
		s.beginTransaction();
		Document d = (Document) s.createQuery( "from Document fetch all properties").uniqueResult();
		assertTrue( Hibernate.isPropertyInitialized( d, "text") );
		assertTrue( Hibernate.isPropertyInitialized( d, "summary") );
		s.getTransaction().commit();
		s.close();

		StandardCacheEntryImpl cacheEntry = (StandardCacheEntryImpl) region.getDataMap().get( p.getCacheAccessStrategy().generateCacheKey( d.getId(), p, sessionFactory(), null ) );
		assertNotNull( cacheEntry );
		sessionFactory().getStatistics().clear();

		s = sessionFactory().openSession();
		s.beginTransaction();
		d = (Document) s.createCriteria(Document.class).uniqueResult();
		assertFalse( Hibernate.isPropertyInitialized( d, "text") );
		assertFalse( Hibernate.isPropertyInitialized(d, "summary") );
		assertEquals( "Hibernate is....", d.getText() );
		assertTrue( Hibernate.isPropertyInitialized(d, "text") );
		assertTrue( Hibernate.isPropertyInitialized(d, "summary") );
		s.getTransaction().commit();
		s.close();

		assertEquals( 2, sessionFactory().getStatistics().getPrepareStatementCount() );

		s = sessionFactory().openSession();
		s.beginTransaction();
		d = s.get(Document.class, d.getId());
		assertFalse( Hibernate.isPropertyInitialized(d, "text") );
		assertFalse( Hibernate.isPropertyInitialized(d, "summary") );
		s.getTransaction().commit();
		s.close();
	}
}
