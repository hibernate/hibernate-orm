/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
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
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"),
				@Setting(name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true"),
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-18565")
public class ReloadAssociatedEntitiesTest {

	private Long oneId;
	private Long threeId;
	private Long simpleOneId;
	private Long simpleThreeId;

	@BeforeAll
	public void before(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final var one = new ConcreteOne();
			final var two = new ConcreteTwo();
			final var three = new ConcreteThree();
			one.setTwo( two );
			two.getOnes().add( one );
			two.setThree( three );
			three.getTwos().add( two );

			s.persist( one );
			s.persist( two );
			s.persist( three );
			oneId = one.getId();
			threeId = three.getId();

			final var simpleOne = new SimpleOne();
			final var simpleTwo = new SimpleTwo();
			final var simpleThree = new SimpleThree();
			simpleOne.setTwo( simpleTwo );
			simpleTwo.getOnes().add( simpleOne );
			simpleTwo.setThree( simpleThree );
			simpleThree.getTwos().add( simpleTwo );

			s.persist( simpleOne );
			s.persist( simpleTwo );
			s.persist( simpleThree );
			simpleOneId = simpleOne.getId();
			simpleThreeId = simpleThree.getId();
		} );
	}

	@AfterAll
	void after(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( ConcreteOne.class ) )
					.executeUpdate();
			session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( ConcreteTwo.class ) )
					.executeUpdate();
			session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( ConcreteThree.class ) )
					.executeUpdate();
		} );
	}

	//--THIS IS THE REPRODUCER--//
	@Test
	public void reloadToOneFromSimpleEntity(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			SimpleOne one = s.createQuery(
							"select o from SimpleOne o join fetch o.two t where o.id = :oneId",
							SimpleOne.class
					)
					.setParameter( "oneId", simpleOneId ).getSingleResult();

			SimpleOne one2 = s.createQuery(
							"select o from SimpleOne o join fetch o.two t join fetch t.three rh where o.id = :oneId",
							SimpleOne.class
					)
					.setParameter( "oneId", simpleOneId ).getSingleResult();

			assertThat( one2 ).isNotNull();
		} );
	}
	//--THIS IS THE REPRODUCER--//

	//--FUTURE PROOF--//
	@Test
	public void reloadToOneFromParameterizedEntity(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			ConcreteOne one = s.createQuery(
							"select o from ConcreteOne o join fetch o.two t where o.id = :oneId",
							ConcreteOne.class
					)
					.setParameter( "oneId", oneId ).getSingleResult();

			ConcreteOne one2 = s.createQuery(
							"select o from ConcreteOne o join fetch o.two t join fetch t.three rh where o.id = :oneId",
							ConcreteOne.class
					)
					.setParameter( "oneId", oneId ).getSingleResult();

			assertThat( one2 ).isNotNull();
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

			ConcreteThree three1 = s.createQuery(
							"select t from ConcreteThree t join fetch t.twos tw join fetch tw.ones o where t.id = :threeId",
							ConcreteThree.class
					)
					.setParameter( "threeId", threeId ).getSingleResult();

			assertThat( three1 ).isNotNull();
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

			SimpleThree three1 = s.createQuery(
							"select t from SimpleThree t join fetch t.twos tw join fetch tw.ones o where t.id = :threeId",
							SimpleThree.class
					)
					.setParameter( "threeId", simpleThreeId ).getSingleResult();

			assertThat( three1 ).isNotNull();
		} );
	}
	//--FUTURE PROOF--//

	@Entity(name = "ConcreteOne")
	public static class ConcreteOne extends AbsOne<ConcreteTwo> {
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

	@Entity(name = "ConcreteTwo")
	public static class ConcreteTwo extends AbsTwo<ConcreteOne, ConcreteThree> {
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

	@Entity(name = "ConcreteThree")
	public static class ConcreteThree {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "three")
		private Set<ConcreteTwo> twos = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<ConcreteTwo> getTwos() {
			return twos;
		}

		public void setTwos(Set<ConcreteTwo> twos) {
			this.twos = twos;
		}
	}

	@Entity(name = "SimpleOne")
	public static class SimpleOne {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "two_id")
		private SimpleTwo two;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SimpleTwo getTwo() {
			return two;
		}

		public void setTwo(SimpleTwo two) {
			this.two = two;
		}
	}

	@Entity(name = "SimpleTwo")
	public static class SimpleTwo {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "three_id")
		private SimpleThree three;

		@OneToMany(mappedBy = "two")
		private Set<SimpleOne> ones = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SimpleThree getThree() {
			return three;
		}

		public void setThree(SimpleThree three) {
			this.three = three;
		}

		public Set<SimpleOne> getOnes() {
			return ones;
		}

		public void setOnes(Set<SimpleOne> ones) {
			this.ones = ones;
		}
	}

	@Entity(name = "SimpleThree")
	public static class SimpleThree {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "three")
		private Set<SimpleTwo> twos = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<SimpleTwo> getTwos() {
			return twos;
		}

		public void setTwos(Set<SimpleTwo> twos) {
			this.twos = twos;
		}
	}
}
