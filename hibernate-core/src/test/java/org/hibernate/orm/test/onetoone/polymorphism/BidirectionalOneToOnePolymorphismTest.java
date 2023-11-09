/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.onetoone.polymorphism;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import org.assertj.core.api.InstanceOfAssertFactories;

@DomainModel(annotatedClasses = {
		BidirectionalOneToOnePolymorphismTest.Level1.class,
		BidirectionalOneToOnePolymorphismTest.Level2.class,
		BidirectionalOneToOnePolymorphismTest.DerivedLevel2.class,
		BidirectionalOneToOnePolymorphismTest.Level3.class
})
@SessionFactory
public class BidirectionalOneToOnePolymorphismTest {

	@Test
	public void persistAndLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Level1 level1 = new Level1();
			level1.setId( 1 );

			DerivedLevel2 level2 = new DerivedLevel2();
			level2.setId( 2 );
			level1.setDerivedLevel2( level2 );
			level2.setLevel1( level1 );

			Level3 level3 = new Level3();
			level3.setId( 3 );
			level2.setLevel3( level3 );
			level3.setLevel2( level2 );

			session.persist( level1 );
			session.persist( level2 );
			session.persist( level3 );
		} );

		// This succeeds, so the information was properly saved
		scope.inTransaction( session -> assertThat( session.getReference( Level1.class, 1 ) )
				.extracting( Level1::getDerivedLevel2 )
				.isNotNull() );

		// This succeeds too, so unproxying works at least in some cases
		scope.inTransaction( session -> assertThat( session.getReference( Level2.class, 2 ) )
				.extracting( Hibernate::unproxy, InstanceOfAssertFactories.type( DerivedLevel2.class ) )
				.extracting( DerivedLevel2::getLevel1 )
				.extracting( Level1::getDerivedLevel2 )
				.isNotNull() );

		// This fails for some reason
		scope.inTransaction( session -> assertThat( session.getReference( Level3.class, 3 ) )
				.extracting( Level3::getLevel2 )
				.extracting( Hibernate::unproxy, InstanceOfAssertFactories.type( DerivedLevel2.class ) )
				.extracting( DerivedLevel2::getLevel1 )
				.extracting( Level1::getDerivedLevel2 )
				.isNotNull() );
	}

	@Entity(name = "Level1")
	static class Level1 {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "level1")
		private DerivedLevel2 derivedLevel2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public DerivedLevel2 getDerivedLevel2() {
			return derivedLevel2;
		}

		public void setDerivedLevel2(DerivedLevel2 derivedLevel2) {
			this.derivedLevel2 = derivedLevel2;
		}
	}

	@Entity(name = "Level2")
	static class Level2 {

		@Id
		private Integer id;

		@OneToOne(mappedBy = "level2")
		private Level3 level3;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level3 getLevel3() {
			return level3;
		}

		public void setLevel3(Level3 level3) {
			this.level3 = level3;
		}

	}

	@Entity(name = "DerivedLevel2")
	static class DerivedLevel2 extends Level2 {

		@OneToOne
		private Level1 level1;

		public Level1 getLevel1() {
			return level1;
		}

		public void setLevel1(Level1 level1) {
			this.level1 = level1;
		}

	}

	@Entity(name = "Level3")
	static class Level3 {

		@Id
		private Integer id;

		@OneToOne(fetch = FetchType.LAZY)
		private Level2 level2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level2 getLevel2() {
			return level2;
		}

		public void setLevel2(Level2 level2) {
			this.level2 = level2;
		}

	}
}
