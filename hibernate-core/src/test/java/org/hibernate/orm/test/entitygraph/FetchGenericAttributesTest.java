/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isInitialized;

@DomainModel(
		annotatedClasses = {
				FetchGenericAttributesTest.AbsOne.class,
				FetchGenericAttributesTest.AbsTwo.class,
				FetchGenericAttributesTest.One.class,
				FetchGenericAttributesTest.Two.class,
				FetchGenericAttributesTest.Three.class
		}
)
@SessionFactory
@JiraKey("HHH-18151")
public class FetchGenericAttributesTest {

	private Long oneId;
	private Long threeId;

	@BeforeAll
	void before(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final var one = new One();
					final var two = new Two();
					final var three = new Three();
					one.setTwo( two );
					two.getOnes().add( one );
					two.setThree( three );
					three.getTwos().add( two );
					one.setOneProp( "oneProp" );
					two.setTwoProp( "twoProp" );
					three.setThreeProp( "threeProp" );

					session.persist( one );
					session.persist( two );
					session.persist( three );
					oneId = one.getId();
					threeId = three.getId();
				}
		);
	}

	@AfterAll
	void after(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( One.class ) )
							.executeUpdate();
					session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( Two.class ) )
							.executeUpdate();
					session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( Three.class ) )
							.executeUpdate();
				}
		);
	}

	@Test
	@JiraKey("HHH-18151")
	void HHH18151WithGraphParser(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					One one = session.byId( One.class )
							.with( GraphParser.parse( One.class, "two(three)", session ), GraphSemantic.FETCH )
							.load( oneId );

					assertThat( one.getId(), is( oneId ) );
					assertThat( one.getTwo(), isInitialized() );
					assertThat( one.getTwo().getThree(), isInitialized() );
				}
		);
	}

	@Test
	@JiraKey("HHH-18151")
	void HHH18151WithEntityGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final var oneGraph = session.createEntityGraph( One.class );
					final var twoSubgraph = oneGraph.addSubgraph( "two" );
					twoSubgraph.addAttributeNode( "three" );
					One one = session.byId( One.class )
							.with( oneGraph, GraphSemantic.FETCH )
							.load( oneId );

					assertThat( one.getId(), is( oneId ) );
					assertThat( one.getTwo(), isInitialized() );
					assertThat( one.getTwo().getThree(), isInitialized() );
				}
		);
	}

	@Entity(name = "One")
	public static class One extends AbsOne<Two> {
		private String oneProp;

		public String getOneProp() {
			return oneProp;
		}

		public void setOneProp(String oneProp) {
			this.oneProp = oneProp;
		}
	}

	@MappedSuperclass
	public static abstract class AbsOne<TWO extends AbsTwo<?, ?>> {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "two_id")
		private TWO two;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public TWO getTwo() {
			return two;
		}

		public void setTwo(TWO two) {
			this.two = two;
		}
	}

	@Entity(name = "Two")
	public static class Two extends AbsTwo<One, Three> {
		private String twoProp;

		public String getTwoProp() {
			return twoProp;
		}

		public void setTwoProp(String twoProp) {
			this.twoProp = twoProp;
		}
	}

	@MappedSuperclass
	public static abstract class AbsTwo<ONE extends AbsOne<?>, THREE> {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "three_id")
		private THREE three;

		@OneToMany(mappedBy = "two")
		private Set<ONE> ones = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public THREE getThree() {
			return three;
		}

		public void setThree(THREE three) {
			this.three = three;
		}

		public Set<ONE> getOnes() {
			return ones;
		}

		public void setOnes(Set<ONE> ones) {
			this.ones = ones;
		}
	}

	@Entity(name = "Three")
	public static class Three {
		@Id
		@GeneratedValue
		private Long id;

		private String threeProp;

		@OneToMany(mappedBy = "three")
		private Set<Two> twos = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getThreeProp() {
			return threeProp;
		}

		public void setThreeProp(String threeProp) {
			this.threeProp = threeProp;
		}

		public Set<Two> getTwos() {
			return twos;
		}

		public void setTwos(Set<Two> twos) {
			this.twos = twos;
		}
	}
}
