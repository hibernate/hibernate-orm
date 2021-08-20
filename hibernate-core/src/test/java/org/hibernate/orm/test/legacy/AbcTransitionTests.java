/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.legacy;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaDelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				AbcTransitionTests.EntityA.class,
				AbcTransitionTests.EntityB.class,
				AbcTransitionTests.EntityC1.class,
				AbcTransitionTests.EntityC2.class,
				AbcTransitionTests.EntityD.class

		}
)
@SessionFactory
public class AbcTransitionTests {

	/**
	 * @see ABCTest#testSubclassing
	 */
	@Test
	public void testSubclassing(SessionFactoryScope scope) {
		final EntityC1 created = scope.fromTransaction( (session) -> {
			final EntityC1 entityC1 = new EntityC1( 1, "some text", "a b name", 1, "a c1 name", "a c1 address" );
			session.save( entityC1 );
			return entityC1;
		} );

		scope.inTransaction( (session) -> {
			assertThat( session.createQuery( "from EntityC2 c where 1=1 or 1=1" ).list() ).hasSize( 0 );

			final Object queried = session.createQuery( "from EntityA e where e.id = :id" )
					.setParameter( "id", created.id )
					.uniqueResult();
			assertThat( queried ).isNotNull();
			assertThat( queried ).isInstanceOf( EntityC1.class );

			final EntityA loaded = session.get( EntityA.class, created.id );
			assertThat( loaded ).isNotNull();
			assertThat( loaded ).isInstanceOf( EntityC1.class );
		} );
	}

	@AfterAll
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final HibernateCriteriaBuilder criteriaBuilder = scope.getSessionFactory().getCriteriaBuilder();
			session.createQuery( criteriaBuilder.createCriteriaDelete( EntityA.class ) ).executeUpdate();
			session.createQuery( criteriaBuilder.createCriteriaDelete( EntityD.class ) ).executeUpdate();
		} );
	}

	@Entity( name = "EntityA" )
	@Table( name = "tbl_a" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "clazz_discriminata" )
	@DiscriminatorValue( "entity-a" )
	public static class EntityA {
		@Id
		Integer id;
		String text;

		public EntityA() {
		}

		public EntityA(Integer id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Entity( name = "EntityB" )
	@Table( name = "tbl_b" )
	@DiscriminatorValue( "entity-b" )
	public static class EntityB extends EntityA {
		String bName;
		int count;

		public EntityB() {
		}

		public EntityB(int id, String text, String bName, int count) {
			super( id, text );
			this.count = count;
		}
	}

	@Entity( name = "EntityC1" )
	@Table( name = "tbl_c1" )
	@DiscriminatorValue( "entity-c1" )
	public static class EntityC1 extends EntityB {
		String cName;
		String cAddress;

		public EntityC1() {
		}

		public EntityC1(int id, String text, String bName, int count, String name, String address) {
			super( id, text, bName, count );
			this.cName = name;
			this.cAddress = address;
		}
	}




	@Entity( name = "EntityC2" )
	@Table( name = "tbl_c2" )
	@DiscriminatorValue( "entity-c2" )
	public static class EntityC2 extends EntityB {
		String c2Name;
		String c2Address;

		public EntityC2() {
		}

		public EntityC2(Integer id, String text, String bName, int count, String name, String address) {
			super( id, text, bName, count );
			this.c2Name = name;
			this.c2Address = address;
		}
	}

	@Entity( name = "EntityD" )
	@Table( name = "tbl_d" )
	public static class EntityD {
		@Id
		Integer id;
		String text;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "rev_fk" )
		EntityA reverse;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "inv_fk" )
		EntityA inverse;

		public EntityD() {
		}

		public EntityD(Integer id, String text) {
			this.id = id;
			this.text = text;
		}
	}

}
