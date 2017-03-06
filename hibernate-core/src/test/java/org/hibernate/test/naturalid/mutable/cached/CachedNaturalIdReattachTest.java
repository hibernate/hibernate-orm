/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.naturalid.mutable.cached;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests of mutable natural ids stored in second level cache
 * 
 * @author Guenther Demetz
 */
public class CachedNaturalIdReattachTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Another.class, AllCached.class, Company.class};
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	
	
	@Test
	@TestForIssue( jiraKey = "HHH-8154" )
	public void testReattachment() {
		Session session = openSession();
		session.beginTransaction();
		Another it = new Another( "it");
		Company c = new Company("test1");
		session.save(c);
		session.save( it );
		c.setAnother(it);
		session.getTransaction().commit();
		session.close();
		
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("it");
		assertNotNull(it);
		c = (Company) session.bySimpleNaturalId(Company.class).load(it);
		assertNotNull(c);
		session.clear();
		session.lock(c, LockMode.NONE); // reattachment  throws 
//		org.hibernate.PropertyAccessException: IllegalArgumentException occurred calling getter of org.hibernate.test.naturalid.mutable.cached.Another.id
//		at org.hibernate.property.BasicPropertyAccessor$BasicGetter.get(BasicPropertyAccessor.java:187)
//		at org.hibernate.tuple.entity.AbstractEntityTuplizer.getIdentifier(AbstractEntityTuplizer.java:341)
//		at org.hibernate.persister.entity.AbstractEntityPersister.getIdentifier(AbstractEntityPersister.java:4323)
//		at org.hibernate.persister.entity.AbstractEntityPersister.isTransient(AbstractEntityPersister.java:4045)
//		at org.hibernate.engine.internal.ForeignKeys.isTransient(ForeignKeys.java:209)
//		at org.hibernate.engine.internal.ForeignKeys.getEntityIdentifierIfNotUnsaved(ForeignKeys.java:248)
//		at org.hibernate.type.ManyToOneType.disassemble(ManyToOneType.java:214)
//		at org.hibernate.cache.spi.NaturalIdCacheKey.<init>(NaturalIdCacheKey.java:81)
//		at org.hibernate.engine.internal.StatefulPersistenceContext$1.removeSharedNaturalIdCrossReference(StatefulPersistenceContext.java:1930)
//		at org.hibernate.persister.entity.AbstractEntityPersister.handleNaturalIdReattachment(AbstractEntityPersister.java:4032)
//		at org.hibernate.persister.entity.AbstractEntityPersister.afterReassociate(AbstractEntityPersister.java:4004)
//		at org.hibernate.event.internal.AbstractReassociateEventListener.reassociate(AbstractReassociateEventListener.java:100)
//		at org.hibernate.event.internal.DefaultLockEventListener.onLock(DefaultLockEventListener.java:81)
//		at org.hibernate.internal.SessionImpl.fireLock(SessionImpl.java:811)
//		at org.hibernate.internal.SessionImpl.lock(SessionImpl.java:796)
//		at org.hibernate.test.naturalid.mutable.cached.CachedNaturalIdReattachTest.testReattachment(CachedNaturalIdReattachTest.java:76)
		
//	Caused by: java.lang.IllegalArgumentException: object is not an instance of declaring class
//		at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//		at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
//		at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
//		at java.lang.reflect.Method.invoke(Unknown Source)
//		at org.hibernate.property.BasicPropertyAccessor$BasicGetter.get(BasicPropertyAccessor.java:164)
//		... 44 more
		assertTrue(session.contains(c));
		assertTrue(session.contains(c.getAnother()));
	}
}