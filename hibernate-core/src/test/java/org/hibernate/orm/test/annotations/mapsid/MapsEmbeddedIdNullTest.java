/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mapsid;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SessionFactory
@DomainModel(
		annotatedClasses = {
				MapsEmbeddedIdNullTest.Level0.class,
				MapsEmbeddedIdNullTest.Level1.class,
				MapsEmbeddedIdNullTest.Level2.class,
				MapsEmbeddedIdNullTest.Level1NoMapsId.class,
				MapsEmbeddedIdNullTest.Level1OneToOne.class,
				MapsEmbeddedIdNullTest.Level1NoMapsIdOneToOne.class,
		})
@Jira("https://hibernate.atlassian.net/browse/HHH-19056")
public class MapsEmbeddedIdNullTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Level0 level0 = new Level0();
			Level2 level2 = new Level2();
			Level1 level1 = new Level1( level0, level2 );
			level0.level1s.add( level1 );
			s.persist( level0 );
		} );

		scope.inTransaction( s -> {
			Level0 level0 = new Level0();
			Level2 level2 = new Level2();
			Level1OneToOne level1 = new Level1OneToOne( level0, level2 );
			s.persist( level1 );
		} );

		assertThrows( IdentifierGenerationException.class, () ->
			scope.inTransaction( s -> {
				Level0 level0 = new Level0();
				Level2 level2 = new Level2();
				Level1NoMapsId level1NoMapsId = new Level1NoMapsId();
				level1NoMapsId.level0 = level0;
				level1NoMapsId.level2 = level2;
				s.persist( level1NoMapsId );
			} )
		);

		assertThrows( IdentifierGenerationException.class, () ->
			scope.inTransaction( s -> {
				Level0 level0 = new Level0();
				Level2 level2 = new Level2();
				Level1NoMapsIdOneToOne level1 = new Level1NoMapsIdOneToOne();
				level1.level0 = level0;
				level1.level2 = level2;
				s.persist( level1 );
			} )
		);
	}

	@Entity(name = "Level0")
	public static class Level0 {
		@Id
		@GeneratedValue
		private Integer id;
		@OneToMany(mappedBy = "level0", cascade = CascadeType.ALL)
		private List<Level1> level1s = new ArrayList<>();
	}

	@Entity(name = "Level1")
	public static class Level1 {
		@EmbeddedId
		Level1PK id;
		@MapsId("level0Id")
		@ManyToOne
		private Level0 level0;
		@MapsId("level2Id")
		@ManyToOne(cascade = CascadeType.ALL)
		private Level2 level2;

		public Level1() {
		}

		public Level1(Level0 level0, Level2 level2) {
			super();
			this.level0 = level0;
			this.level2 = level2;
		}
	}

	@Entity(name = "Level1OneToOne")
	public static class Level1OneToOne {
		@EmbeddedId
		Level1PK id;
		@OneToOne
		@MapsId("level0Id")
		private Level0 level0;
		@OneToOne
		@MapsId("level2Id")
		private Level2 level2;

		public Level1OneToOne() {
		}

		public Level1OneToOne(Level0 level0, Level2 level2) {
			super();
			this.level0 = level0;
			this.level2 = level2;
		}
	}


	@Entity(name = "Level1NoMapsId")
	public static class Level1NoMapsId {
		@EmbeddedId
		Level1PK id;
		@ManyToOne
		private Level0 level0;
		@ManyToOne(cascade = CascadeType.ALL)
		private Level2 level2;
	}

	@Entity(name = "Level1NoMapsIdOneToOne")
	public static class Level1NoMapsIdOneToOne {
		@EmbeddedId
		Level1PK id;
		@ManyToOne
		private Level0 level0;
		@ManyToOne(cascade = CascadeType.ALL)
		private Level2 level2;
	}

	@Entity(name = "Level2")
	public static class Level2 {
		@Id
		@GeneratedValue
		private Integer id;
	}

	public static class Level1PK {
		private Integer level0Id;
		private Integer level2Id;

		@Override
		public final boolean equals(Object o) {
			if ( !(o instanceof Level1PK level1PK) ) {
				return false;
			}

			return Objects.equals( level0Id, level1PK.level0Id )
				&& Objects.equals( level2Id, level1PK.level2Id );
		}

		@Override
		public int hashCode() {
			int result = Objects.hashCode( level0Id );
			result = 31 * result + Objects.hashCode( level2Id );
			return result;
		}
	}
}
