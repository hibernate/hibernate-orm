/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.readonly;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Model:
 *
 * Container
 * |-- N : 1 -- noProxyOwner (property-ref="name" lazy="no-proxy" cascade="all")
 * |-- N : 1 -- proxyOwner (property-ref="name" lazy="proxy" cascade="all")
 * |-- N : 1 -- nonLazyOwner (property-ref="name" lazy="false" cascade="all")
 * |-- N : 1 -- noProxyInfo" (lazy="no-proxy" cascade="all")
 * |-- N : 1 -- proxyInfo (lazy="proxy" cascade="all"
 * |-- N : 1 -- nonLazyInfo" (lazy="false" cascade="all")
 * |
 * |-- 1 : N -- lazyDataPoints" (lazy="true" inverse="false" cascade="all")
 * |-- 1 : N -- nonLazySelectDataPoints" (lazy="false" inverse="false" cascade="all" fetch="select")
 * |-- 1 : N -- nonLazyJoinDataPoints" (lazy="false" inverse="false" cascade="all" fetch="join")
 *
 * Note: the following many-to-one properties use a property-ref so they are
 * initialized, regardless of how the lazy attribute is mapped:
 * noProxyOwner, proxyOwner, nonLazyOwner
 *
 * @author Gail Badner
 */
public class ReadOnlySessionLazyNonLazyTest extends AbstractReadOnlyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "readonly/DataPoint.hbm.xml" };
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testExistingModifiableAfterSetSessionReadOnly() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();

		t = s.beginTransaction();
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		Container c = ( Container ) s.load( Container.class, cOrig.getId() );
		assertSame( cOrig, c );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		c = ( Container ) s.get( Container.class, cOrig.getId() );
		assertSame( cOrig, c );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.refresh( cOrig );
		assertSame( cOrig, c );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.evict( cOrig );
		c = ( Container ) s.get( Container.class, cOrig.getId()  );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		expectedReadOnlyObjects.add(c.getLazyDataPoints().iterator().next() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();

	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testExistingReadOnlyAfterSetSessionModifiable() {

		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();
		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		Container c = ( Container ) s.get( Container.class, cOrig.getId()  );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( false );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		//expectedReadOnlyObjects.add(c.getLazyDataPoints().iterator().next() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();

	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testExistingReadOnlyAfterSetSessionModifiableExisting() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();
		DataPoint lazyDataPointOrig = ( DataPoint ) cOrig.getLazyDataPoints().iterator().next();
		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		Container c = ( Container ) s.get( Container.class, cOrig.getId()  );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( false );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		DataPoint lazyDataPoint = ( DataPoint ) s.get( DataPoint.class, lazyDataPointOrig.getId() );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		assertSame( lazyDataPoint, c.getLazyDataPoints().iterator().next() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();

	}

	@SuppressWarnings( {"unchecked"})
	@Test
 	public void testExistingReadOnlyAfterSetSessionModifiableExistingEntityReadOnly() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();
		DataPoint lazyDataPointOrig = ( DataPoint ) cOrig.getLazyDataPoints().iterator().next();
		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		Container c = ( Container ) s.get( Container.class, cOrig.getId()  );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( false );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		DataPoint lazyDataPoint = ( DataPoint ) s.get( DataPoint.class, lazyDataPointOrig.getId() );
		s.setDefaultReadOnly( false );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		assertSame( lazyDataPoint, c.getLazyDataPoints().iterator().next() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		expectedReadOnlyObjects.add( lazyDataPoint );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}


	@SuppressWarnings( {"unchecked"})
	@Test
	public void testExistingReadOnlyAfterSetSessionModifiableProxyExisting() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();
		DataPoint lazyDataPointOrig = ( DataPoint ) cOrig.getLazyDataPoints().iterator().next();
		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		Container c = ( Container ) s.get( Container.class, cOrig.getId()  );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( false );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		DataPoint lazyDataPoint = ( DataPoint ) s.load( DataPoint.class, lazyDataPointOrig.getId() );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		assertSame( lazyDataPoint, c.getLazyDataPoints().iterator().next() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();

	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testExistingReadOnlyAfterSetSessionModifiableExistingProxyReadOnly() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();
		DataPoint lazyDataPointOrig = ( DataPoint ) cOrig.getLazyDataPoints().iterator().next();
		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		Container c = ( Container ) s.get( Container.class, cOrig.getId()  );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( false );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		DataPoint lazyDataPoint = ( DataPoint ) s.load( DataPoint.class, lazyDataPointOrig.getId() );
		s.setDefaultReadOnly( false );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		assertSame( lazyDataPoint, c.getLazyDataPoints().iterator().next() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		expectedReadOnlyObjects.add( lazyDataPoint );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDefaultModifiableWithReadOnlyQueryForEntity() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		Container c = ( Container ) s.createQuery( "from Container where id=" + cOrig.getId() )
				.setReadOnly( true ).uniqueResult();
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		//expectedReadOnlyObjects.add(c.getLazyDataPoints().iterator().next() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDefaultReadOnlyWithModifiableQueryForEntity() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		Container c = ( Container ) s.createQuery( "from Container where id=" + cOrig.getId() )
				.setReadOnly( false ).uniqueResult();
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet();
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		expectedReadOnlyObjects.add(c.getLazyDataPoints().iterator().next() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDefaultReadOnlyWithQueryForEntity() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		Container c = ( Container ) s.createQuery( "from Container where id=" + cOrig.getId() )
				.uniqueResult();
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		expectedReadOnlyObjects.add(c.getLazyDataPoints().iterator().next() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDefaultModifiableWithQueryForEntity() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		Container c = ( Container ) s.createQuery( "from Container where id=" + cOrig.getId() )
				.uniqueResult();
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet();
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getNoProxyInfo() ) );
		Hibernate.initialize( c.getNoProxyInfo() );
		expectedInitializedObjects.add( c.getNoProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getProxyInfo() ) );
		Hibernate.initialize( c.getProxyInfo() );
		expectedInitializedObjects.add( c.getProxyInfo() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		assertFalse( Hibernate.isInitialized( c.getLazyDataPoints() ) );
		Hibernate.initialize( c.getLazyDataPoints() );
		expectedInitializedObjects.add( c.getLazyDataPoints().iterator().next() );
		//expectedReadOnlyObjects.add(c.getLazyDataPoints().iterator().next() );
		checkContainer( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDefaultModifiableWithReadOnlyQueryForCollectionEntities() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		DataPoint dp = ( DataPoint ) s.createQuery( "select c.lazyDataPoints from Container c join c.lazyDataPoints where c.id=" + cOrig.getId() )
				.setReadOnly( true ).uniqueResult();
		assertTrue( s.isReadOnly( dp ) );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testDefaultReadOnlyWithModifiableFilterCollectionEntities() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		Container c = ( Container ) s.get( Container.class, cOrig.getId() );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		List list = s.createFilter( c.getLazyDataPoints(), "" )
				.setMaxResults(1)
				.setReadOnly( false )
				.list();
		assertEquals( 1, list.size() );
		assertFalse( s.isReadOnly( list.get( 0 ) ) );
		list = s.createFilter( c.getNonLazyJoinDataPoints(), "" )
				.setMaxResults(1)
				.setReadOnly( false )
				.list();
		assertEquals( 1, list.size() );
		assertTrue( s.isReadOnly( list.get( 0 ) ) );
		list = s.createFilter( c.getNonLazySelectDataPoints(), "" )
				.setMaxResults(1)
				.setReadOnly( false )
				.list();
		assertEquals( 1, list.size() );
		assertTrue( s.isReadOnly( list.get( 0 ) ) );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDefaultModifiableWithReadOnlyFilterCollectionEntities() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		Container c = ( Container ) s.get( Container.class, cOrig.getId() );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet();
		List list = s.createFilter( c.getLazyDataPoints(), "" )
				.setMaxResults(1)
				.setReadOnly( true )
				.list();
		assertEquals( 1, list.size() );
		assertTrue( s.isReadOnly( list.get( 0 ) ) );
		list = s.createFilter( c.getNonLazyJoinDataPoints(), "" )
				.setMaxResults(1)
				.setReadOnly( true )
				.list();
		assertEquals( 1, list.size() );
		assertFalse( s.isReadOnly( list.get( 0 ) ) );
		list = s.createFilter( c.getNonLazySelectDataPoints(), "" )
				.setMaxResults(1)
				.setReadOnly( true )
				.list();
		assertEquals( 1, list.size() );
		assertFalse( s.isReadOnly( list.get( 0 ) ) );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDefaultReadOnlyWithFilterCollectionEntities() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		Container c = ( Container ) s.get( Container.class, cOrig.getId() );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNoProxyInfo(),
						c.getProxyInfo(),
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						//c.getLazyDataPoints(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		List list = s.createFilter( c.getLazyDataPoints(), "" )
				.setMaxResults( 1 )
				.list();
		assertEquals( 1, list.size() );
		assertTrue( s.isReadOnly( list.get( 0 ) ) );
		list = s.createFilter( c.getNonLazyJoinDataPoints(), "" )
				.setMaxResults( 1 )
				.list();
		assertEquals( 1, list.size() );
		assertTrue( s.isReadOnly( list.get( 0 ) ) );
		list = s.createFilter( c.getNonLazySelectDataPoints(), "" )
				.setMaxResults( 1 )
				.list();
		assertEquals( 1, list.size() );
		assertTrue( s.isReadOnly( list.get( 0 ) ) );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDefaultModifiableWithFilterCollectionEntities() {
		Container cOrig = createContainer();
		Set expectedInitializedObjects = new HashSet(
				Arrays.asList(
						cOrig,
						cOrig.getNoProxyInfo(),
						cOrig.getProxyInfo(),
						cOrig.getNonLazyInfo(),
						cOrig.getNoProxyOwner(),
						cOrig.getProxyOwner(),
						cOrig.getNonLazyOwner(),
						cOrig.getLazyDataPoints().iterator().next(),
						cOrig.getNonLazyJoinDataPoints().iterator().next(),
						cOrig.getNonLazySelectDataPoints().iterator().next()
				)
		);
		Set expectedReadOnlyObjects = new HashSet();

		Session s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		Transaction t = s.beginTransaction();
		s.save( cOrig );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		checkContainer( cOrig, expectedInitializedObjects, expectedReadOnlyObjects, s );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		Container c = ( Container ) s.get( Container.class, cOrig.getId() );
		assertNotSame( cOrig, c );
		expectedInitializedObjects = new HashSet(
				Arrays.asList(
						c,
						c.getNonLazyInfo(),
						c.getNoProxyOwner(),
						c.getProxyOwner(),
						c.getNonLazyOwner(),
						c.getNonLazyJoinDataPoints().iterator().next(),
						c.getNonLazySelectDataPoints().iterator().next()
				)
		);
		expectedReadOnlyObjects = new HashSet();
		List list = s.createFilter( c.getLazyDataPoints(), "" )
				.setMaxResults( 1 )
				.list();
		assertEquals( 1, list.size() );
		assertFalse( s.isReadOnly( list.get( 0 ) ) );
		list = s.createFilter( c.getNonLazyJoinDataPoints(), "" )
				.setMaxResults( 1 )
				.list();
		assertEquals( 1, list.size() );
		assertFalse( s.isReadOnly( list.get( 0 ) ) );
		list = s.createFilter( c.getNonLazySelectDataPoints(), "" )
				.setMaxResults( 1 )
				.list();
		assertEquals( 1, list.size() );
		assertFalse( s.isReadOnly( list.get( 0 ) ) );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from DataPoint").executeUpdate();
		s.createQuery("delete from Container").executeUpdate();
		s.createQuery("delete from Info").executeUpdate();
		s.createQuery("delete from Owner").executeUpdate();
		t.commit();
		s.close();
	}

	private Container createContainer() {
		Container c = new Container( "container" );
		c.setNoProxyInfo( new Info( "no-proxy info" ) );
		c.setProxyInfo( new Info( "proxy info" ) );
		c.setNonLazyInfo( new Info( "non-lazy info" ) );
		c.setNoProxyOwner( new Owner( "no-proxy owner" ) );
		c.setProxyOwner( new Owner( "proxy owner" ) );
		c.setNonLazyOwner( new Owner( "non-lazy owner" ) );
		c.getLazyDataPoints().add( new DataPoint( new BigDecimal( 1 ), new BigDecimal( 1 ), "lazy data point" ) );
		c.getNonLazyJoinDataPoints().add( new DataPoint( new BigDecimal( 2 ), new BigDecimal( 2 ), "non-lazy join data point" ) );
		c.getNonLazySelectDataPoints().add( new DataPoint( new BigDecimal( 3 ), new BigDecimal( 3 ), "non-lazy select data point" ) );
		return c;
	}

	private void checkContainer(Container c, Set expectedInitializedObjects, Set expectedReadOnlyObjects, Session s) {
		checkObject( c, expectedInitializedObjects, expectedReadOnlyObjects, s );
		if ( ! expectedInitializedObjects.contains( c ) ) {
			return;
		}
		checkObject( c.getNoProxyInfo(), expectedInitializedObjects, expectedReadOnlyObjects, s);
		checkObject( c.getProxyInfo(), expectedInitializedObjects, expectedReadOnlyObjects, s);
		checkObject( c.getNonLazyInfo(), expectedInitializedObjects, expectedReadOnlyObjects, s );
		checkObject( c.getNoProxyOwner(), expectedInitializedObjects, expectedReadOnlyObjects, s );
		checkObject( c.getProxyOwner(), expectedInitializedObjects, expectedReadOnlyObjects, s );
		checkObject( c.getNonLazyOwner(), expectedInitializedObjects, expectedReadOnlyObjects, s );
		if ( Hibernate.isInitialized( c.getLazyDataPoints() ) ) {
			for ( Iterator it=c.getLazyDataPoints().iterator(); it.hasNext(); ) {
				checkObject( it.next(), expectedInitializedObjects, expectedReadOnlyObjects, s );
			}
		}
		for ( Iterator it=c.getNonLazyJoinDataPoints().iterator(); it.hasNext(); ) {
			checkObject( it.next(), expectedInitializedObjects, expectedReadOnlyObjects, s );
		}
		for ( Iterator it=c.getNonLazySelectDataPoints().iterator(); it.hasNext(); ) {
			checkObject( it.next(), expectedInitializedObjects, expectedReadOnlyObjects, s );
		}
	}

	private void checkObject(Object entityOrProxy, Set expectedInitializedObjects, Set expectedReadOnlyObjects, Session s) {
		boolean isExpectedToBeInitialized = expectedInitializedObjects.contains( entityOrProxy );
		boolean isExpectedToBeReadOnly = expectedReadOnlyObjects.contains( entityOrProxy );
		SessionImplementor si = (SessionImplementor) s;
		assertEquals( isExpectedToBeInitialized, Hibernate.isInitialized( entityOrProxy ) );
		assertEquals( isExpectedToBeReadOnly, s.isReadOnly( entityOrProxy ) );
		if ( Hibernate.isInitialized( entityOrProxy ) ) {
			Object entity = ( entityOrProxy instanceof HibernateProxy ?
					( ( HibernateProxy ) entityOrProxy ).getHibernateLazyInitializer().getImplementation( si ) :
					entityOrProxy
			);
			assertNotNull( entity );
			assertEquals( isExpectedToBeReadOnly, s.isReadOnly( entity ));
		}
	}

}
