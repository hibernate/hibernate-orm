/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				ReloadAssociatedEntitiesTest.SimpleOne.class,
				ReloadAssociatedEntitiesTest.SimpleTwo.class,
				ReloadAssociatedEntitiesTest.SimpleThree.class,
				ReloadAssociatedEntitiesTest.ConcreteOne.class,
				ReloadAssociatedEntitiesTest.ConcreteTwo.class,
				ReloadAssociatedEntitiesTest.ConcreteThree.class,
				ReloadAssociatedEntitiesTest.AbsOne.class,
				ReloadAssociatedEntitiesTest.AbsTwo.class,
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@JiraKey("HHH-18565")
public class ReloadAssociatedEntitiesTest {

	private Long oneId;
	private Long threeId;
	private Long simpleOneId;
	private Long simpleThreeId;

	@BeforeEach
	public void before(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final var three = new ConcreteThree();
			final var two = new ConcreteTwo( "two", three );
			final var one = new ConcreteOne( "one", two );
			two.getOnes().add( one );
			three.getTwos().add( two );

			s.persist( one );
			s.persist( two );
			s.persist( three );
			oneId = one.getId();
			threeId = three.getId();

			final var simpleThree = new SimpleThree( "simple three" );
			final var simpleTwo = new SimpleTwo( "simple two", simpleThree );
			final var simpleOne = new SimpleOne( "simple one", simpleTwo );

			s.persist( simpleOne );
			s.persist( simpleTwo );
			s.persist( simpleThree );
			simpleOneId = simpleOne.getId();
			simpleThreeId = simpleThree.getId();
		} );
	}

	@AfterEach
	void after(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void reloadToOneFromSimpleEntity(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			SimpleOne one = s.createQuery(
							"select o from SimpleOne o join fetch o.two t where o.id = :oneId",
							SimpleOne.class
					)
					.setParameter( "oneId", simpleOneId ).getSingleResult();

			assertThat( one ).isNotNull();
			assertThat( Hibernate.isInitialized( one ) ).isTrue();
			assertThat( Hibernate.isPropertyInitialized( one, "two" ) ).isTrue();
			SimpleTwo two = one.getTwo();
			assertThat( Hibernate.isInitialized( two.getThree() ) ).isFalse();

			SimpleOne one2 = s.createQuery(
							"select o from SimpleOne o join fetch o.two t join fetch t.three rh where o.id = :oneId",
							SimpleOne.class
					)
					.setParameter( "oneId", simpleOneId ).getSingleResult();

			assertThat( one2 ).isNotNull();
			assertThat( one2 ).isSameAs( one );
			assertThat( Hibernate.isInitialized( one2 ) ).isTrue();
			assertThat( Hibernate.isPropertyInitialized( one2, "two" ) ).isTrue();
			SimpleTwo two2 = one2.getTwo();
			assertThat( two2 ).isSameAs( two );
			assertThat( Hibernate.isInitialized( two2.getThree() ) ).isTrue();
		} );
	}

	@Test
	public void reloadToOneFromParameterizedEntity(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			ConcreteOne one = s.createQuery(
							"select o from ConcreteOne o join fetch o.two t where o.id = :oneId",
							ConcreteOne.class
					)
					.setParameter( "oneId", oneId ).getSingleResult();

			assertThat( one ).isNotNull();

			ConcreteOne one2 = s.createQuery(
							"select o from ConcreteOne o join fetch o.two t join fetch t.three rh where o.id = :oneId",
							ConcreteOne.class
					)
					.setParameter( "oneId", oneId ).getSingleResult();

			assertThat( one2 ).isNotNull();
			assertThat( one2 ).isSameAs( one );
		} );
	}

	@Test
	public void reloadToManyFromParameterizedEntity(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			ConcreteThree three = s.createQuery(
							"select t from ConcreteThree t join t.twos tw where t.id = :threeId",
							ConcreteThree.class
					)
					.setParameter( "threeId", threeId ).getSingleResult();

			assertThat( three ).isNotNull();

			ConcreteThree three1 = s.createQuery(
							"select t from ConcreteThree t join fetch t.twos tw join fetch tw.ones o where t.id = :threeId",
							ConcreteThree.class
					)
					.setParameter( "threeId", threeId ).getSingleResult();

			assertThat( three1 ).isNotNull();
			assertThat( three1 ).isSameAs( three );
		} );
	}

	@Test
	public void reloadToManyFromSimpleEntity(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			SimpleThree three = s.createQuery(
							"select t from SimpleThree t join t.twos tw where t.id = :threeId",
							SimpleThree.class
					)
					.setParameter( "threeId", simpleThreeId ).getSingleResult();

			assertThat( three ).isNotNull();

			SimpleThree three1 = s.createQuery(
							"select t from SimpleThree t join fetch t.twos tw join fetch tw.ones o where t.id = :threeId",
							SimpleThree.class
					)
					.setParameter( "threeId", simpleThreeId ).getSingleResult();

			assertThat( three1 ).isNotNull();
			assertThat( three1 ).isSameAs( three );
		} );
	}

	@Entity(name = "ConcreteOne")
	public static class ConcreteOne extends AbsOne<ConcreteTwo> {

		public ConcreteOne() {
		}

		public ConcreteOne(String name, ConcreteTwo two) {
			super( name, two );
			two.getOnes().add( this );
		}
	}

	@MappedSuperclass
	public static abstract class AbsOne<TWO extends AbsTwo<?, ?>> {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "two_id")
		private TWO two;

		public AbsOne() {
		}

		public AbsOne(String name, TWO two) {
			this.two = two;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public TWO getTwo() {
			return two;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "ConcreteTwo")
	public static class ConcreteTwo extends AbsTwo<ConcreteOne, ConcreteThree> {
		public ConcreteTwo() {
		}

		public ConcreteTwo(String name, ConcreteThree concreteThree) {
			super( name, concreteThree );
		}
	}

	@MappedSuperclass
	public static abstract class AbsTwo<ONE extends AbsOne<?>, THREE> {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "three_id")
		private THREE three;

		@OneToMany(mappedBy = "two")
		private Set<ONE> ones = new HashSet<>();

		public AbsTwo() {
		}

		public AbsTwo(String name, THREE three) {
			this.name = name;
			this.three = three;
		}

		public Long getId() {
			return id;
		}


		public THREE getThree() {
			return three;
		}


		public Set<ONE> getOnes() {
			return ones;
		}

	}

	@Entity(name = "ConcreteThree")
	public static class ConcreteThree {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(mappedBy = "three")
		private Set<ConcreteTwo> twos = new HashSet<>();

		public ConcreteThree() {
		}

		public ConcreteThree(String name, Set<ConcreteTwo> twos) {
			this.name = name;
			this.twos = twos;
		}

		public Long getId() {
			return id;
		}

		public Set<ConcreteTwo> getTwos() {
			return twos;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "SimpleOne")
	public static class SimpleOne {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "two_id")
		private SimpleTwo two;

		public SimpleOne() {
		}

		public SimpleOne(String name, SimpleTwo two) {
			this.name = name;
			this.two = two;
			two.ones.add( this );
		}

		public Long getId() {
			return id;
		}


		public SimpleTwo getTwo() {
			return two;
		}

	}

	@Entity(name = "SimpleTwo")
	public static class SimpleTwo {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "three_id")
		private SimpleThree three;

		@OneToMany(mappedBy = "two")
		private Set<SimpleOne> ones = new HashSet<>();

		public SimpleTwo() {
		}

		public SimpleTwo(String name, SimpleThree three) {
			this.name = name;
			this.three = three;
			three.twos.add( this );
		}

		public Long getId() {
			return id;
		}

		public SimpleThree getThree() {
			return three;
		}

		public Set<SimpleOne> getOnes() {
			return ones;
		}

	}

	@Entity(name = "SimpleThree")
	public static class SimpleThree {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(mappedBy = "three")
		private Set<SimpleTwo> twos = new HashSet<>();

		public SimpleThree() {
		}

		public SimpleThree(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public Set<SimpleTwo> getTwos() {
			return twos;
		}

	}
}
