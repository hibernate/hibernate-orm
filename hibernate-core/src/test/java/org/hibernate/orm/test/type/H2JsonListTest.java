/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator.safeRandomUUID;

@SessionFactory
@DomainModel( annotatedClasses = { H2JsonListTest.Path.class, H2JsonListTest.PathClob.class } )
@RequiresDialect( value = H2Dialect.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16320" )
public class H2JsonListTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Path( List.of( safeRandomUUID(), safeRandomUUID() ) ) );
			session.persist( new PathClob( List.of( safeRandomUUID(), safeRandomUUID() ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Path" ).executeUpdate();
			session.createMutationQuery( "delete from PathClob" ).executeUpdate();
		} );
	}

	@Test
	public void testRetrievalJson(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Path path = session.find( Path.class, 1L );
			assertThat( path ).isNotNull();
			assertThat( path.getRelativePaths() ).hasSize( 2 );
		} );
	}

	@Test
	public void testNativeSyntaxJson(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "insert into paths (relativePaths,id) values (?1 FORMAT JSON, ?2)" )
					.setParameter(
							1,
							"[\"2b099c92-95ff-42e0-9f8c-f08c2518792d\", \"8d2164db-86b4-460a-91d0-bf821a8ca3d7\"]"
					)
					.setParameter( 2, 99L )
					.executeUpdate();
		} );
		scope.inTransaction( session -> {
			final Path path = (Path) session.createNativeQuery(
					"select * from paths_clob where id = 99",
					Path.class
			).getSingleResult();
			assertThat( path ).isNotNull();
			assertThat( path.getRelativePaths() ).hasSize( 2 );
		} );
	}

	@Test
	public void testRetrievalClob(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final PathClob path = session.find( PathClob.class, 1L );
			assertThat( path ).isNotNull();
			assertThat( path.getRelativePaths() ).hasSize( 2 );
		} );
	}

	@Test
	public void testNativeSyntaxClob(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "insert into paths_clob (relativePaths,id) values (?1 FORMAT JSON, ?2)" )
					.setParameter(
							1,
							"[\"2b099c92-95ff-42e0-9f8c-f08c2518792d\", \"8d2164db-86b4-460a-91d0-bf821a8ca3d7\"]"
					)
					.setParameter( 2, 99L )
					.executeUpdate();
		} );
		scope.inTransaction( session -> {
			final NativeQuery<PathClob> nativeQuery = session.createNativeQuery(
					"select * from paths_clob where id = 99",
					PathClob.class
			);
			final PathClob path = nativeQuery.getSingleResult();
			assertThat( path ).isNotNull();
			assertThat( path.getRelativePaths() ).hasSize( 2 );
		} );
	}

	@Entity( name = "Path" )
	@Table( name = "paths" )
	public static class Path {
		@Id
		@GeneratedValue
		public Long id;

		@JdbcTypeCode( SqlTypes.JSON )
		public List<UUID> relativePaths;

		public Path() {
		}

		public Path(List<UUID> relativePaths) {
			this.relativePaths = relativePaths;
		}

		public List<UUID> getRelativePaths() {
			return relativePaths;
		}
	}

	@Entity( name = "PathClob" )
	@Table( name = "paths_clob" )
	public static class PathClob {
		@Id
		@GeneratedValue
		public Long id;

		@JdbcTypeCode( SqlTypes.JSON )
		@Column( columnDefinition = "clob" )
		public List<UUID> relativePaths;

		public PathClob() {
		}

		public PathClob(List<UUID> relativePaths) {
			this.relativePaths = relativePaths;
		}

		public List<UUID> getRelativePaths() {
			return relativePaths;
		}
	}
}
