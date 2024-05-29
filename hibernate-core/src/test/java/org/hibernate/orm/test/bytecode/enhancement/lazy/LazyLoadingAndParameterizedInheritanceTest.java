/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@JiraKey("HHH-18151")
public class LazyLoadingAndParameterizedInheritanceTest extends BaseCoreFunctionalTestCase {

	private Long oneId;
	private Long threeId;

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				One.class,
				AbsOne.class,
				AbsTwo.class,
				Three.class,
				Two.class,
		};
	}

	@Before
	public void before() {
		inTransaction( s -> {
			final var one = new One();
			final var two = new Two();
			final var three = new Three();
			one.setTwo( two );
			two.getOnes().add( one );
			two.setThree( three );
			three.getTwos().add( two );

			s.persist( one );
			s.persist( two );
			s.persist( three );
			oneId = one.getId();
			threeId = three.getId();
		} );
	}

	@After
	public void after() {
		inTransaction( session -> {
			session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( One.class ) )
					.executeUpdate();
			session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( Two.class ) )
					.executeUpdate();
			session.createMutationQuery( session.getCriteriaBuilder().createCriteriaDelete( Three.class ) )
					.executeUpdate();
		} );
	}

	@Test
	public void test() {
		inTransaction( s -> {
			One one = s.find( One.class, oneId );
			Two two = one.getTwo();
			final var three = two.getThree();

			final var actualThree = s.find( Three.class, threeId );
			assertThat( actualThree ).isNotNull();
			assertThat( actualThree.getTwos().iterator().next().getId() ).isEqualTo( two.getId() );

			//That is the actual test. If three == null ==> lazy load was not performed.
			assertThat( three ).isNotNull();
		} );
	}

	@Entity(name = "One")
	public static class One extends AbsOne<Two> {
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

		@OneToMany(mappedBy = "three")
		private Set<Two> twos = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Two> getTwos() {
			return twos;
		}

		public void setTwos(Set<Two> twos) {
			this.twos = twos;
		}
	}
}
