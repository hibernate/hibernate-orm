/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ElementCollectionLeftJoinSubqueryTest.EntityA.class,
		ElementCollectionLeftJoinSubqueryTest.EntityB.class,
		ElementCollectionLeftJoinSubqueryTest.EmbeddableC.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16928" )
public class ElementCollectionLeftJoinSubqueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var b = new EntityB();
			b.getcCollection().add( new EmbeddableC( 1, "embeddable_1" ) );
			b.getcCollection().add( new EmbeddableC( 2, "embeddable_2" ) );
			session.persist( b );
			var a = new EntityA();
			a.getbCollection().add( b );
			session.persist( a );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	@Test
	public void testNestedJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Tuple> resultList = session.createQuery(
					"select a.id, b.id, c.intValue" +
							" from EntityA a" +
							" left join EntityB b on b.a.id = a.id" +
							" left join b.cCollection as c" +
							" where (c.intValue = (select max(c2.intValue)" +
							" from b.cCollection as c2) or c.intValue is null)",
					Tuple.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).get( 2, Integer.class ) ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Tuple> resultList = session.createQuery(
					"select b.id, c.intValue" +
							" from EntityB b" +
							" left join b.cCollection as c" +
							" where (c.intValue = (select max(c2.intValue)" +
							" from b.cCollection as c2) or c.intValue is null)",
					Tuple.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).get( 1, Integer.class ) ).isEqualTo( 2 );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany
		@JoinColumn( name = "a_id" )
		private List<EntityB> bCollection = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public List<EntityB> getbCollection() {
			return bCollection;
		}
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		@JoinColumn( name = "a_id" )
		private EntityA a;

		@ElementCollection
		@CollectionTable( name = "c_embeddables", joinColumns = { @JoinColumn( name = "id" ) } )
		private final Set<EmbeddableC> cCollection = new HashSet<>();

		public EntityA getA() {
			return a;
		}

		public void setA(EntityA a) {
			this.a = a;
		}

		public Set<EmbeddableC> getcCollection() {
			return cCollection;
		}
	}

	@Embeddable
	public static class EmbeddableC {
		private Integer intValue;

		private String name;

		public EmbeddableC() {
		}

		public EmbeddableC(Integer intValue, String name) {
			this.intValue = intValue;
			this.name = name;
		}

		public Integer getIntValue() {
			return intValue;
		}

		public String getName() {
			return name;
		}
	}
}
