/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa( annotatedClasses = {BidirectionalOneToManyTest.Organization.class, BidirectionalOneToManyTest.User.class} )
@Jira("https://hibernate.atlassian.net/browse/HHH-19963")
public class BidirectionalOneToManyTest {

	@Test
	public void testParentNotTreatedAsBidirectional(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Organization o3 = new Organization( 3L, "o3", null, new ArrayList<>() );
			Organization o1 = new Organization( 1L, "o1", null, new ArrayList<>( Arrays.asList( o3 )) );
			Organization o2 = new Organization( 2L, "o2", o1, new ArrayList<>() );
			entityManager.persist(o3);
			entityManager.persist(o1);
			entityManager.persist(o2);

			User u1 = new User( 1L, o2 );
			User u2 = new User( 2L, o2 );
			entityManager.persist(u1);
			entityManager.persist(u2);
		});

		scope.inTransaction( entityManager -> {
			User user1 = entityManager.find(User.class, 1L);
			Organization ou3 = entityManager.find(Organization.class, 3L);
			assertNull( ou3.getParentOrganization(), "Parent of o3 is null");
			assertEquals(0, ou3.getPredecessorOrganizations().size(), "Predecessors of o3 is empty");
		});
	}

	@Entity(name = "Organization")
	public static class Organization {

		@Id
		private Long id;
		private String name;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "parentorganization_objectId")
		private Organization parentOrganization;

		@ManyToMany(fetch = FetchType.EAGER)
		@JoinTable(name = "organization_predecessor")
		private List<Organization> predecessorOrganizations = new ArrayList<>();

		public Organization() {
		}

		public Organization(Long id, String name, Organization parentOrganization, List<Organization> predecessorOrganizations) {
			this.id = id;
			this.name = name;
			this.parentOrganization = parentOrganization;
			this.predecessorOrganizations = predecessorOrganizations;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Organization getParentOrganization() {
			return parentOrganization;
		}

		public List<Organization> getPredecessorOrganizations() {
			return predecessorOrganizations;
		}
	}

	@Entity(name = "User")
	@Table(name = "usr_tbl")
	public static class User {

		@Id
		private Long id;
		@ManyToOne(fetch = FetchType.EAGER)
		private Organization organization;

		public User() {
		}

		public User(Long id, Organization organization) {
			this.id = id;
			this.organization = organization;
		}

		public Long getId() {
			return id;
		}

		public Organization getOrganization() {
			return organization;
		}
	}
}
