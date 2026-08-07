/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.version;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				CallableSqlUpdateWithVersionTest.EntityA.class,
				CallableSqlUpdateWithVersionTest.EntityB.class
		}
)
@SessionFactory
@RequiresDialect(org.hibernate.dialect.H2Dialect.class)
@JiraKey( "HHH-20504" )
public class CallableSqlUpdateWithVersionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeQuery(
									"CREATE ALIAS UPDATE_A FOR \"" + CallableSqlUpdateWithVersionTest.class.getCanonicalName() + ".updateA\";" )
							.executeUpdate();

					EntityA entityA = new EntityA( 1L, "system" );
					EntityB entityB = new EntityB( "system2" );
					entityA.addB( entityB );

					session.persist( entityA );
					session.persist( entityB );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			session.createNativeQuery( "DROP ALIAS IF EXISTS UPDATE_A" ).executeUpdate();
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, 1L );
					assertThat( entityA.getVersion() ).isEqualTo( 0L );
					entityA.getBs().clear();
				}
		);
		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, 1L );
					assertThat( entityA.getVersion() ).isEqualTo( 1L );
					assertThat( entityA.getBs() ).isEmpty();
				}
		);
	}

	@Entity(name = "EntityA")
	@SQLUpdate(sql = "{call UPDATE_A(?, ?, ?, ?)}", callable = true)
	@Table(name = "table_a")
	public static class EntityA {

		@Id
		Long id;

		@Version
		@Column(name = "version")
		long version;

		@Column(name = "created_by")
		String createdBy;


		@ManyToMany
		@JoinTable(name = "a_b", joinColumns = @JoinColumn(name = "a_id"),
				inverseJoinColumns = @JoinColumn(name = "b_id"))
		Set<EntityB> entityBS = new LinkedHashSet<>();

		public EntityA() {
		}

		public EntityA(Long id, String createdBy) {
			this.id = id;
			this.createdBy = createdBy;
		}

		public long getVersion() {
			return version;
		}

		public Set<EntityB> getBs() {
			return entityBS;
		}

		public void addB(EntityB entityB) {
			this.entityBS.add( entityB );
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {

		@Id
		@GeneratedValue
		Long id;

		String name;

		public EntityB() {
		}

		public EntityB(String name) {
			this.name = name;
		}
	}

	public static int updateA(
			Connection con,
			String createdBy,
			long oldVersion,
			long id,
			long newVersion) throws SQLException {
		String sql = """
					update table_a set
						created_by=?,
						version=?
					where
						id=? and version=?
				""";

		try (PreparedStatement ps = con.prepareStatement( sql )) {
			ps.setString( 1, createdBy );
			ps.setLong( 2, oldVersion );
			ps.setLong( 3, id );
			ps.setLong( 4, newVersion );
			return ps.executeUpdate();
		}
	}
}
