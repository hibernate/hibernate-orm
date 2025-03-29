/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		VersionSeedUpdateTest.ShortVersionEntity.class,
		VersionSeedUpdateTest.IntVersionEntity.class,
		VersionSeedUpdateTest.LongVersionEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16678" )
public class VersionSeedUpdateTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new ShortVersionEntity( 1L, "name_1" ) );
			session.persist( new IntVersionEntity( 2L, "name_2" ) );
			session.persist( new LongVersionEntity( 3L, "name_3" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ShortVersionEntity" ).executeUpdate();
			session.createMutationQuery( "delete from IntVersionEntity" ).executeUpdate();
			session.createMutationQuery( "delete from LongVersionEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testShortVersionIncrease(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				"update versioned ShortVersionEntity set name='updated_name_1'"
		).executeUpdate() );
		scope.inTransaction( session -> {
			final ShortVersionEntity versionedEntity = session.find( ShortVersionEntity.class, 1L );
			assertThat( versionedEntity.getName() ).isEqualTo( "updated_name_1" );
			assertThat( versionedEntity.getVersion() ).isEqualTo( (short) 1 );
		} );
	}

	@Test
	public void testIntVersionIncrease(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				"update versioned IntVersionEntity set name='updated_name_2'"
		).executeUpdate() );
		scope.inTransaction( session -> {
			final IntVersionEntity versionedEntity = session.find( IntVersionEntity.class, 2L );
			assertThat( versionedEntity.getName() ).isEqualTo( "updated_name_2" );
			assertThat( versionedEntity.getVersion() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testLongVersionIncrease(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				"update versioned LongVersionEntity set name='updated_name_3'"
		).executeUpdate() );
		scope.inTransaction( session -> {
			final LongVersionEntity versionedEntity = session.find( LongVersionEntity.class, 3L );
			assertThat( versionedEntity.getName() ).isEqualTo( "updated_name_3" );
			assertThat( versionedEntity.getVersion() ).isEqualTo( 1L );
		} );
	}

	@Entity( name = "ShortVersionEntity" )
	public static class ShortVersionEntity {
		@Id
		private Long id;

		private String name;

		@Version
		private short version;

		public ShortVersionEntity() {
		}

		public ShortVersionEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public short getVersion() {
			return version;
		}
	}

	@Entity( name = "IntVersionEntity" )
	public static class IntVersionEntity {
		@Id
		private Long id;

		private String name;

		@Version
		private int version;

		public IntVersionEntity() {
		}

		public IntVersionEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public int getVersion() {
			return version;
		}
	}

	@Entity( name = "LongVersionEntity" )
	public static class LongVersionEntity {
		@Id
		private Long id;

		private String name;

		@Version
		private long version;

		public LongVersionEntity() {
		}

		public LongVersionEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public long getVersion() {
			return version;
		}
	}
}
