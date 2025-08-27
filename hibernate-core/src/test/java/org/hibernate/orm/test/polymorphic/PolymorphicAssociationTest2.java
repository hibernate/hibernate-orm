/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.polymorphic;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				PolymorphicAssociationTest2.Level1.class,
				PolymorphicAssociationTest2.Level2.class,
				PolymorphicAssociationTest2.DerivedLevel2.class,
				PolymorphicAssociationTest2.Level3.class
		}
)
@SessionFactory
public class PolymorphicAssociationTest2 {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Level1 level1 = new Level1();
			level1.setId( 1 );

			DerivedLevel2 level2 = new DerivedLevel2();
			level2.setId( 2 );

			Level3 level3 = new Level3();
			level3.setId( 3 );

			level1.setLevel2Child( level2 );
			level2.setLevel1Parent( level1 );
			level2.setLevel3Child( level3 );
			level3.setLevel2Parent( level2 );

			level3.setName( "initial-name" );

			session.persist( level1 );
			session.persist( level2 );
			session.persist( level3 );
		} );
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Level3 level3 = session.getReference( Level3.class, 3 );
			Level2 level2 = level3.getLevel2Parent();
			assertThat( level2 ).isNotNull();
			final Level3 level3Child = level2.getLevel3Child();
			assertThat( level3Child ).extracting( "id" ).isEqualTo( 3 );
		} );

		scope.inTransaction( session -> {
			Level1 level1 = session.getReference( Level1.class, 1 );
			Level2 level2 = level1.getLevel2Child();
			assertThat( level2 ).isNotNull();
			assertThat( level2.getLevel1Parent() ).extracting( "id" ).isEqualTo( 1 );
		} );
	}

	@Entity(name = "Level1")
	public static class Level1 {

		@Id
		private Integer id;

		@OneToOne(mappedBy = "level1Parent")
		private Level2 level2Child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level2 getLevel2Child() {
			return level2Child;
		}

		public void setLevel2Child(Level2 level2Child) {
			this.level2Child = level2Child;
		}
	}

	@Entity(name = "Level2")
	public static class Level2 {

		@Id
		private Integer id;

		@OneToOne
		private Level1 level1Parent;

		@OneToOne(mappedBy = "level2Parent")
		private Level3 level3Child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level3 getLevel3Child() {
			return level3Child;
		}

		public void setLevel3Child(Level3 level3Child) {
			this.level3Child = level3Child;
		}

		public Level1 getLevel1Parent() {
			return level1Parent;
		}

		public void setLevel1Parent(Level1 level1Parent) {
			this.level1Parent = level1Parent;
		}
	}

	@Entity(name = "DerivedLevel2")
	public static class DerivedLevel2 extends Level2 {

	}

	@Entity(name = "Level3")
	static class Level3 {

		@Id
		private Integer id;

		@Basic
		private String name;

		@OneToOne(fetch = FetchType.LAZY)
		private Level2 level2Parent;

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

		public Level2 getLevel2Parent() {
			return level2Parent;
		}

		public void setLevel2Parent(Level2 level2Parent) {
			this.level2Parent = level2Parent;
		}
	}
}
