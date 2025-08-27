/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.polymorphism;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(annotatedClasses = {
		BidirectionalOneToOnePolymorphismTest.Level1.class,
		BidirectionalOneToOnePolymorphismTest.Level2.class,
		BidirectionalOneToOnePolymorphismTest.DerivedLevel2.class,
		BidirectionalOneToOnePolymorphismTest.Level3.class
})
@SessionFactory
@JiraKey( "HHH-17408" )
public class BidirectionalOneToOnePolymorphismTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
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
				}
		);
	}

	@Test
	public void loadAndUnProxyTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Level1 reference = session.getReference( Level1.class, 1 );
			assertThat( reference )
					.extracting( Level1::getDerivedLevel2 )
					.isNotNull();
		} );

		scope.inTransaction(
				session -> {
					Level2 level2Proxy = session.getReference( Level2.class, 2 );
					assertFalse( Hibernate.isInitialized( level2Proxy ) );

					Object unproxy = Hibernate.unproxy( level2Proxy );
					assertThat( unproxy ).isInstanceOf( DerivedLevel2.class );
					DerivedLevel2 level2 = (DerivedLevel2) unproxy;

					Level1 level1 = level2.getLevel1();
					DerivedLevel2 derivedLevel2 = level1.getDerivedLevel2();
					assertThat( derivedLevel2 ).isNotNull();
					assertThat( derivedLevel2 ).isSameAs( level2 );
				} );

		scope.inTransaction(
				session -> {
					Level3 level3Proxy = session.getReference( Level3.class, 3 );
					assertFalse( Hibernate.isInitialized( level3Proxy ) );

					Object unproxy = Hibernate.unproxy( level3Proxy.getLevel2() );

					assertThat( unproxy ).isInstanceOf( DerivedLevel2.class );
					DerivedLevel2 level2 = (DerivedLevel2) unproxy;

					Level1 level1 = level2.getLevel1();
					DerivedLevel2 derivedLevel2 = level1.getDerivedLevel2();
					assertThat( derivedLevel2 ).isNotNull();
					assertThat( derivedLevel2 ).isSameAs( level2 );
				} );
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
