/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.joinedsubclass;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12645")
public class JoinedSubclassWithSameAttributeNamesTest extends BaseCoreFunctionalTestCase {
	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseEntity {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "EntityA")
	public static class EntityA extends BaseEntity {
		@OneToMany(fetch = FetchType.LAZY)
		private Set<EntityC> attributes;

		public Set<EntityC> getAttributes() {
			return attributes;
		}

		public void setAttributes(Set<EntityC> attributes) {
			this.attributes = attributes;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB extends BaseEntity {
		@OneToMany(fetch = FetchType.LAZY)
		private Set<EntityC> attributes;

		public Set<EntityC> getAttributes() {
			return attributes;
		}

		public void setAttributes(Set<EntityC> attributes) {
			this.attributes = attributes;
		}
	}

	@Entity(name = "EntityC")
	public static class EntityC {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		EntityC() {
		}

		EntityC(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BaseEntity.class, EntityA.class, EntityB.class, EntityC.class };
	}

	@Test
	public void testHQL() {
		doInHibernate( this::sessionFactory, session -> {
			Set<EntityC> attributes = new HashSet<>();
			attributes.add( new EntityC( "acme" ) );
			for ( EntityC attribute : attributes ) {
				session.save( attribute );
			}

			EntityA a = new EntityA();
			a.setAttributes( attributes );
			session.save( a );
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<BaseEntity> bases = session.createQuery( "FROM BaseEntity e LEFT JOIN FETCH e.attributes" ).getResultList();
			assertEquals( 1, bases.size() );
			assertTyping( EntityA.class, bases.get( 0 ) );

			EntityA a = (EntityA) bases.get( 0 );
			assertEquals( 1, a.getAttributes().size() );
			assertEquals( "acme", a.getAttributes().iterator().next().getName() );
		} );
	}
}
