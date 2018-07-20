/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.serialization;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.proxy.AbstractLazyInitializer;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Selaron
 */
public class EntityProxySerializationTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class, ChildEntity.class };
	}

	@Override
	protected void configure(final Configuration configuration) {
		// enable LL without TX, which used to cause problems when serializing proxies (see HHH-12720)
		configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, Boolean.TRUE.toString() );
	}

	/**
	 * Prepare and persist a {@link SimpleEntity} with two {@link ChildEntity}.
	 */
	@Before
	public void prepare() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();

		try {
			final Number count = (Number) s.createQuery("SELECT count(ID) FROM SimpleEntity").getSingleResult();
			if (count.longValue() > 0L) {
				// entity already added previously
				return;
			}

			final SimpleEntity entity = new SimpleEntity();
			entity.setId( 1L );
			entity.setName( "TheParent" );

			final ChildEntity c1 = new ChildEntity();
			c1.setId( 1L );
			c1.setParent( entity );

			final ChildEntity c2 = new ChildEntity();
			c2.setId( 2L );
			c2.setParent( entity );

			s.save( entity );
			s.save( c1 );
			s.save( c2 );
		}
		finally {
			t.commit();
			s.close();
		}
	}

	/**
	 * Tests that serializing an initialized proxy will serialize the target instead.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testInitializedProxySerializationIfTargetInPersistenceContext() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final ChildEntity child = s.find( ChildEntity.class, 1L );

			final SimpleEntity parent = child.getParent();

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof HibernateProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			// Initialize the proxy
			parent.getName();
			assertTrue( Hibernate.isInitialized( parent ) );

			// serialize/deserialize the proxy
			final SimpleEntity deserializedParent = (SimpleEntity) SerializationHelper.clone( parent );

			// assert the deserialized object is no longer a proxy, but the target of the proxy
			assertFalse( deserializedParent instanceof HibernateProxy );
			assertEquals( "TheParent", deserializedParent.getName() );
		}
		finally {
			if ( t.isActive() ) {
				t.rollback();
			}
			s.close();
		}
	}

	/**
	 * Tests that serializing a proxy which is not initialized
	 * but whose target has been (separately) added to the persistence context
	 * will serialize the target instead.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testUninitializedProxySerializationIfTargetInPersistenceContext() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final ChildEntity child = s.find( ChildEntity.class, 1L );

			final SimpleEntity parent = child.getParent();

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof HibernateProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			// Load the target of the proxy without the proxy being made aware of it
			s.detach( parent );
			s.find( SimpleEntity.class, 1L );
			s.update( parent );

			// assert we still have an uninitialized proxy
			assertFalse( Hibernate.isInitialized( parent ) );

			// serialize/deserialize the proxy
			final SimpleEntity deserializedParent = (SimpleEntity) SerializationHelper.clone( parent );

			// assert the deserialized object is no longer a proxy, but the target of the proxy
			assertFalse( deserializedParent instanceof HibernateProxy );
			assertEquals( "TheParent", deserializedParent.getName() );
		}
		finally {
			if ( t.isActive() ) {
				t.rollback();
			}
			s.close();
		}
	}

	/**
	 * Tests that lazy loading without transaction nor open session is generally
	 * working. The magic is done by {@link AbstractLazyInitializer} who opens a
	 * temporary session.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testProxyInitializationWithoutTX() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final ChildEntity child = s.find( ChildEntity.class, 1L );

			final SimpleEntity parent = child.getParent();

			t.rollback();
			session.close();

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof HibernateProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			assertEquals( "TheParent", parent.getName() );

			// assert we have an initialized proxy now
			assertTrue( Hibernate.isInitialized( parent ) );
		}
		finally {
			if ( t.isActive() ) {
				t.rollback();
			}
			s.close();
		}
	}

	/**
	 * Tests that lazy loading without transaction nor open session is generally
	 * working. The magic is done by {@link AbstractLazyInitializer} who opens a
	 * temporary session.
	 */
	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-12720")
	public void testProxyInitializationWithoutTXAfterDeserialization() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final ChildEntity child = s.find( ChildEntity.class, 1L );

			final SimpleEntity parent = child.getParent();

			// destroy AbstractLazyInitializer internal state
			final SimpleEntity deserializedParent = (SimpleEntity) SerializationHelper.clone( parent );

			t.rollback();
			session.close();

			// assert we have an uninitialized proxy
			assertTrue( deserializedParent instanceof HibernateProxy );
			assertFalse( Hibernate.isInitialized( deserializedParent ) );

			assertEquals( "TheParent", deserializedParent.getName() );

			// assert we have an initialized proxy now
			assertTrue( Hibernate.isInitialized( deserializedParent ) );
		}
		finally {
			if ( t.isActive() ) {
				t.rollback();
			}
			s.close();
		}
	}

	@Entity(name = "SimpleEntity")
	static class SimpleEntity implements Serializable {

		private Long id;

		private String name;

		Set<ChildEntity> children = new HashSet<>();

		@Id
		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@OneToMany(targetEntity = ChildEntity.class, mappedBy = "parent")
		@LazyCollection(LazyCollectionOption.EXTRA)
		@Fetch(FetchMode.SELECT)
		public Set<ChildEntity> getChildren() {
			return children;
		}

		public void setChildren(final Set<ChildEntity> children) {
			this.children = children;
		}

	}

	@Entity(name = "ChildEntity")
	static class ChildEntity {
		private Long id;

		private SimpleEntity parent;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn
		@LazyToOne(LazyToOneOption.PROXY)
		public SimpleEntity getParent() {
			return parent;
		}

		public void setParent(final SimpleEntity parent) {
			this.parent = parent;
		}

	}
}