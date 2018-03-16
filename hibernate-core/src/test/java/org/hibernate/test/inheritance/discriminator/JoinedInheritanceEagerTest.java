/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.inheritance.discriminator;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * Test cases for joined inheritance with eager fetching.
 *
 * @author Christian Beikov
 */
public class JoinedInheritanceEagerTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				BaseEntity.class,
				EntityA.class,
				EntityB.class,
				EntityC.class,
				EntityD.class
		};
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			EntityC entityC = new EntityC( 1L );
			EntityD entityD = new EntityD( 2L );

			EntityB entityB = new EntityB( 3L );
			entityB.setRelation( entityD );

			EntityA entityA = new EntityA( 4L );
			entityA.setRelation( entityC );

			session.persist( entityC );
			session.persist( entityD );
			session.persist( entityA );
			session.persist( entityB );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12375")
	public void joinFindEntity() {
		doInHibernate( this::sessionFactory, session -> {
			EntityA entityA = session.get( EntityA.class, 4L );
			Assert.assertTrue( Hibernate.isInitialized( entityA.getRelation() ) );
			Assert.assertFalse( Hibernate.isInitialized( entityA.getAttributes() ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12375")
	public void joinFindParenEntity() {
		doInHibernate( this::sessionFactory, session -> {
			BaseEntity baseEntity = session.get( BaseEntity.class, 4L );
			Assert.assertThat( baseEntity, notNullValue() );
			Assert.assertThat( baseEntity, instanceOf( EntityA.class ) );
			Assert.assertTrue( Hibernate.isInitialized( ( (EntityA) baseEntity ).getRelation() ) );
			Assert.assertFalse( Hibernate.isInitialized( ( (EntityA) baseEntity ).getAttributes() ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			BaseEntity baseEntity = session.get( BaseEntity.class, 3L );
			Assert.assertThat( baseEntity, notNullValue() );
			Assert.assertThat( baseEntity, instanceOf( EntityB.class ) );
			Assert.assertTrue( Hibernate.isInitialized( ( (EntityB) baseEntity ).getRelation() ) );
			Assert.assertFalse( Hibernate.isInitialized( ( (EntityB) baseEntity ).getAttributes() ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12375")
	public void joinUnrelatedCollectionOnBaseType() {
		final Session s = openSession();
		s.getTransaction().begin();

		try {
			s.createQuery( "from BaseEntity b join b.attributes" ).list();
			Assert.fail( "Expected a resolution exception for property 'attributes'!" );
		}
		catch (IllegalArgumentException ex) {
			Assert.assertTrue( ex.getMessage().contains( "could not resolve property: attributes " ) );
		}
		finally {
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12375")
	public void selectBaseType() {
		doInHibernate( this::sessionFactory, session -> {
			List result = session.createQuery( "from BaseEntity" ).list();
			Assert.assertEquals(result.size(), 2);
		} );
	}

	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseEntity {
		@Id
		private Long id;

		public BaseEntity() {
		}

		public BaseEntity(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "EntityA")
	public static class EntityA extends BaseEntity {
		@OneToMany(fetch = FetchType.LAZY)
		private Set<EntityC> attributes;
		@ManyToOne(fetch = FetchType.EAGER)
		private EntityC relation;

		public EntityA() {
		}

		public EntityA(Long id) {
			super( id );
		}

		public void setRelation(EntityC relation) {
			this.relation = relation;
		}

		public EntityC getRelation() {
			return relation;
		}

		public Set<EntityC> getAttributes() {
			return attributes;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB extends BaseEntity {
		@OneToMany(fetch = FetchType.LAZY)
		private Set<EntityD> attributes;
		@ManyToOne(fetch = FetchType.EAGER)
		private EntityD relation;

		public EntityB() {
		}

		public EntityB(Long id) {
			super( id );
		}

		public void setRelation(EntityD relation) {
			this.relation = relation;
		}

		public EntityD getRelation() {
			return relation;
		}

		public Set<EntityD> getAttributes() {
			return attributes;
		}
	}

	@Entity(name = "EntityC")
	public static class EntityC {
		@Id
		private Long id;

		public EntityC() {
		}

		public EntityC(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "EntityD")
	public static class EntityD {
		@Id
		private Long id;

		public EntityD() {
		}

		public EntityD(Long id) {
			this.id = id;
		}
	}
}
