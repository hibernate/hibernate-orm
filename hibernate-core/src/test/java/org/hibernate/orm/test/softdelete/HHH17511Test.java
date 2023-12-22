/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Jan Schatteman
 */
@JiraKey( "HHH-17511" )
@DomainModel(
		annotatedClasses = {
				HHH17511Test.AUser.class, HHH17511Test.Organization.class, HHH17511Test.OrganizationMember.class
		}
)
@SessionFactory
public class HHH17511Test {

	@BeforeEach
	void setup(SessionFactoryScope scope) {
		Long toBeDeletedOrganizationId = scope.fromTransaction(
				session -> {
					AUser u1 = new AUser();
					u1.setName( "John" );
					AUser u2 = new AUser();
					u2.setName( "Joe" );
					session.persist( u1 );
					session.persist( u2 );

					Organization o1 = new Organization();
					o1.setName( "Acme" );
					Organization o2 = new Organization();
					o2.setName( "Emca" );
					session.persist( o1 );
					session.persist( o2 );

					OrganizationMember om1 = new OrganizationMember();
					om1.setPrimary( Boolean.TRUE );
					om1.setOrganizationId( o1.getId() );
					om1.setUserId( u1.getId() );
					OrganizationMember om2 = new OrganizationMember();
					om2.setPrimary( Boolean.FALSE );
					om2.setOrganizationId( o2.getId() );
					om2.setUserId( u2.getId() );
					session.persist( om1 );
					session.persist( om2 );

					return o2.getId();
				}
		);

		scope.inTransaction(
				session -> {
					Organization o = session.find( Organization.class, toBeDeletedOrganizationId );
					session.remove( o );
				}
		);
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeQuery( "delete from organization_member", Void.class ).executeUpdate();
					session.createNativeQuery( "delete from organization", Void.class ).executeUpdate();
					session.createNativeQuery( "delete from theusers", Void.class).executeUpdate();
				}
		);
	}

	@Test
	public void testSoftDeleteConditionOnJoinedEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<OrganizationMember> query = session.createQuery( "FROM OrganizationMember om INNER JOIN Organization o ON o.id = om.organizationId WHERE om.userId =:userId", OrganizationMember.class);
					query.setTupleTransformer( (tuple, aliases) -> (OrganizationMember) tuple[0] );

					query.setParameter("userId", 1L);
					Assertions.assertEquals(1, query.getResultList().size() );

					// Organization 2 has been soft-deleted so this should not give any results
					query.setParameter("userId", 2L);
					Assertions.assertEquals(0, query.getResultList().size() );
				}
		);
	}

	@Entity(name = "Organization")
	@Table(name = "organization")
	@SoftDelete
	public static class Organization {

		@Id
		@GeneratedValue
		private Long id;
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "OrganizationMember")
	@Table(name = "organization_member")
	@SoftDelete
	public static class OrganizationMember {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "user_id")
		private Long userId;

		@Column(name = "organization_id")
		private Long organizationId;

		@Column(name = "primary_")
		private Boolean primary;

		public Boolean getPrimary() {
			return primary;
		}

		public void setPrimary(Boolean primary) {
			this.primary = primary;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getOrganizationId() {
			return organizationId;
		}

		public void setOrganizationId(Long organizationId) {
			this.organizationId = organizationId;
		}
	}

	@Entity(name = "AUser")
	@Table(name = "theusers")
	@SoftDelete
	public static class AUser {

		@Id
		@GeneratedValue
		private Long id;


		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
