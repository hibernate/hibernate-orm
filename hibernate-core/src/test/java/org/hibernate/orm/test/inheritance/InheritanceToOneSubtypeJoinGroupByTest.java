/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		InheritanceToOneSubtypeJoinGroupByTest.Base.class,
		InheritanceToOneSubtypeJoinGroupByTest.EntityA.class,
		InheritanceToOneSubtypeJoinGroupByTest.EntityB.class,
		InheritanceToOneSubtypeJoinGroupByTest.WhitelistEntry.class,
} )
@SessionFactory
public class InheritanceToOneSubtypeJoinGroupByTest {
	@Test
	void testGroupByA(SessionFactoryScope scope) {
		scope.inSession( session -> {
			//noinspection removal
			final EntityA result = session.createQuery(
					"SELECT a FROM WhitelistEntry we JOIN we.primaryKey.a a group by a",
					EntityA.class
			).getSingleResult();
			assertThat( result.getAName() ).isEqualTo( "a" );
		} );
	}

	@Test
	void testGroupByB(SessionFactoryScope scope) {
		scope.inSession( session -> {
			//noinspection removal
			final Tuple result = session.createQuery(
					"SELECT b.id, b.bName FROM WhitelistEntry we JOIN we.primaryKey.b b group by b",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 1, String.class ) ).isEqualTo( "b" );
		} );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA a = new EntityA();
			a.setAName( "a" );
			final EntityB b = new EntityB();
			b.setBName( "b" );
			final WhitelistEntry whitelistEntry = new WhitelistEntry();
			whitelistEntry.setName( "whitelistEntry" );
			final WhitelistEntryPK primaryKey = new WhitelistEntryPK();
			primaryKey.setA( a );
			primaryKey.setB( b );
			whitelistEntry.setPrimaryKey( primaryKey );
			session.persist( a );
			session.persist( b );
			session.persist( whitelistEntry );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@SuppressWarnings("unused")
	@Entity(name = "Base")
	@Inheritance
	public static class Base {
		@Id
		@GeneratedValue
		private Long id;
	}


	@Entity(name = "EntityA")
	public static class EntityA extends Base {
		private String aName;

		public String getAName() {
			return aName;
		}

		public void setAName(String name) {
			this.aName = name;
		}
	}

	@SuppressWarnings("unused")
	@Entity(name = "EntityB")
	public static class EntityB extends Base {
		private String bName;

		public String getBName() {
			return bName;
		}

		public void setBName(String name) {
			this.bName = name;
		}
	}

	@Embeddable
	public static class WhitelistEntryPK {
		@ManyToOne
		private EntityB b;

		@ManyToOne
		private EntityA a;

		public WhitelistEntryPK() {
		}

		public EntityB getB() {
			return b;
		}

		public void setB(EntityB b) {
			this.b = b;
		}

		public EntityA getA() {
			return a;
		}

		public void setA(EntityA a) {
			this.a = a;
		}
	}

	@Entity(name = "WhitelistEntry")
	public static class WhitelistEntry {
		@EmbeddedId
		private WhitelistEntryPK primaryKey;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public WhitelistEntryPK getPrimaryKey() {
			return primaryKey;
		}

		public void setPrimaryKey(WhitelistEntryPK primaryKey) {
			this.primaryKey = primaryKey;
		}
	}
}
