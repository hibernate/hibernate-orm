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

import java.io.Serializable;

import org.junit.Test;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests of mutable natural ids stored in second level cache
 * 
 * @author Guenther Demetz
 * @author Steve Ebersole
 */
public abstract class CachedMutableNaturalIdTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {A.class, Another.class, AllCached.class, B.class};
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testNaturalIdChangedWhileAttached() {
		Session session = openSession();
		session.beginTransaction();
		Another it = new Another( "it" );
		session.save( it );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId( Another.class ).load( "it" );
		assertNotNull( it );
		// change it's name
		it.setName( "it2" );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId( Another.class ).load( "it" );
		assertNull( it );
		it = (Another) session.bySimpleNaturalId( Another.class ).load( "it2" );
		assertNotNull( it );
		session.delete( it );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testNaturalIdChangedWhileDetached() {
		Session session = openSession();
		session.beginTransaction();
		Another it = new Another( "it" );
		session.save( it );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId( Another.class ).load( "it" );
		assertNotNull( it );
		session.getTransaction().commit();
		session.close();

		it.setName( "it2" );

		session = openSession();
		session.beginTransaction();
		session.update( it );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId( Another.class ).load( "it" );
		assertNull( it );
		it = (Another) session.bySimpleNaturalId( Another.class ).load( "it2" );
		assertNotNull( it );
		session.delete( it );
		session.getTransaction().commit();
		session.close();
	}
	
	
	
	@Test
	public void testNaturalIdRecachingWhenNeeded() {
		Session session = openSession();
		session.getSessionFactory().getStatistics().clear();
		session.beginTransaction();
		Another it = new Another( "it");
		session.save( it );
		Serializable id = it.getId();
		session.getTransaction().commit();
		session.close();
		
		session = openSession();
		for (int i=0; i < 10; i++) {
			session.beginTransaction();
			it = (Another) session.byId(Another.class).load(id);
			if (i == 9) {
				it.setName("name" + i);
			}
			it.setSurname("surname" + i); // changing something but not the natural-id's
			session.getTransaction().commit();
		}
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("it");
		assertNull(it);
		assertEquals(0, session.getSessionFactory().getStatistics().getNaturalIdCacheHitCount());
		it = (Another) session.byId(Another.class).load(id);
		session.delete(it);
		session.getTransaction().commit();
		
		// finally there should be only 2 NaturalIdCache puts : 1. insertion, 2. when updating natural-id from 'it' to 'name9'
		assertEquals(2, session.getSessionFactory().getStatistics().getNaturalIdCachePutCount());
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7245" )
	public void testNaturalIdChangeAfterResolveEntityFrom2LCache() {
			Session session = openSession();
			session.beginTransaction();
			AllCached it = new AllCached( "it" );
			
			session.save( it );
			Serializable id = it.getId();
			session.getTransaction().commit();
			session.close();

			session = openSession();
			session.beginTransaction();
			it = (AllCached) session.byId( AllCached.class ).load( id );

			it.setName( "it2" );
			it = (AllCached) session.bySimpleNaturalId( AllCached.class ).load( "it" );
			assertNull( it );
			it = (AllCached) session.bySimpleNaturalId( AllCached.class ).load( "it2" );
			assertNotNull( it );
			session.delete( it );
			session.getTransaction().commit();
			session.close();
	}
	
	@Test
	public void testReattachementUnmodifiedInstance() {
		Session session = openSession();
		session.beginTransaction();
		A a = new A();
		B b = new B();
		b.naturalid = 100;
		session.persist( a );
		session.persist( b ); 
		b.assA = a;
		a.assB.add( b );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.buildLockRequest(LockOptions.NONE).lock(b);
		// org.hibernate.PropertyAccessException: could not get a field value by reflection getter of org.hibernate.test.naturalid.mutable.cached.A.oid
//		at org.hibernate.property.DirectPropertyAccessor$DirectGetter.get(DirectPropertyAccessor.java:62)
//		at org.hibernate.tuple.entity.AbstractEntityTuplizer.getIdentifier(AbstractEntityTuplizer.java:341)
//		at org.hibernate.persister.entity.AbstractEntityPersister.getIdentifier(AbstractEntityPersister.java:4425)
//		at org.hibernate.persister.entity.AbstractEntityPersister.isTransient(AbstractEntityPersister.java:4147)
//		at org.hibernate.engine.internal.ForeignKeys.isTransient(ForeignKeys.java:209)
//		at org.hibernate.engine.internal.ForeignKeys.getEntityIdentifierIfNotUnsaved(ForeignKeys.java:248)
//		at org.hibernate.type.ManyToOneType.disassemble(ManyToOneType.java:214)
//		at org.hibernate.cache.spi.NaturalIdCacheKey.<init>(NaturalIdCacheKey.java:84)
//		at org.hibernate.engine.internal.StatefulPersistenceContext$1.removeSharedNaturalIdCrossReference(StatefulPersistenceContext.java:1991)
//		at org.hibernate.persister.entity.AbstractEntityPersister.handleNaturalIdReattachment(AbstractEntityPersister.java:4134)
//		at org.hibernate.persister.entity.AbstractEntityPersister.afterReassociate(AbstractEntityPersister.java:4106)
//		at org.hibernate.event.internal.AbstractReassociateEventListener.reassociate(AbstractReassociateEventListener.java:100)
//		at org.hibernate.event.internal.DefaultLockEventListener.onLock(DefaultLockEventListener.java:81)
//		at org.hibernate.internal.SessionImpl.fireLock(SessionImpl.java:811)
//		at org.hibernate.internal.SessionImpl.fireLock(SessionImpl.java:804)
//		at org.hibernate.internal.SessionImpl.access$11(SessionImpl.java:803)
//		at org.hibernate.internal.SessionImpl$LockRequestImpl.lock(SessionImpl.java:2365)
//		at org.hibernate.test.naturalid.mutable.cached.CachedMutableNaturalIdTest.testSimone(CachedMutableNaturalIdTest.java:308)
//		at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//		at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
//		at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
//		at java.lang.reflect.Method.invoke(Unknown Source)
//		at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:45)
//		at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:15)
//		at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:42)
//		at org.hibernate.testing.junit4.ExtendedFrameworkMethod.invokeExplosively(ExtendedFrameworkMethod.java:63)
//		at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:20)
//		at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:28)
//		at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:30)
//		at org.junit.internal.runners.statements.FailOnTimeout$StatementThread.run(FailOnTimeout.java:62)
//	Caused by: java.lang.IllegalArgumentException: Can not set long field org.hibernate.test.naturalid.mutable.cached.A.oid to java.lang.Long
//		at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(Unknown Source)
//		at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(Unknown Source)
//		at sun.reflect.UnsafeFieldAccessorImpl.ensureObj(Unknown Source)
//		at sun.reflect.UnsafeLongFieldAccessorImpl.getLong(Unknown Source)
//		at sun.reflect.UnsafeLongFieldAccessorImpl.get(Unknown Source)
//		at java.lang.reflect.Field.get(Unknown Source)
//		at org.hibernate.property.DirectPropertyAccessor$DirectGetter.get(DirectPropertyAccessor.java:59)
//		... 29 more


		session.delete(b.assA);
		session.delete(b);
		
		session.getTransaction().commit();
		session.close();
	}

}

