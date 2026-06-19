/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

public class IsEmptyWithManyToManyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Project.class,
			Developer.class
		};
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Developer dev1 = new Developer();
			dev1.companyId = "CMP1";
			dev1.developerId = 1001L;
			dev1.name = "John Doe";
			entityManager.persist( dev1 );

			Project emptyProject = new Project();
			emptyProject.orgId = "ORG1";
			emptyProject.projectId = 2001L;
			emptyProject.title = "Empty Project";
			entityManager.persist( emptyProject );

			Project projectWithDev = new Project();
			projectWithDev.orgId = "ORG1";
			projectWithDev.projectId = 2002L;
			projectWithDev.title = "Project With Developer";
			projectWithDev.developers.add( dev1 );
			entityManager.persist( projectWithDev );
		});
	}

	@Test
	public void testIsEmptyWithManyToMany() {
		List<Project> projectsWithDevelopers = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"select p from Project p where p.developers is not empty", Project.class )
					.getResultList();
		});

		assertEquals( 1, projectsWithDevelopers.size() );

		List<Project> projectsWithoutDevelopers = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"select p from Project p where p.developers is empty", Project.class )
					.getResultList();
		});

		assertEquals( 1, projectsWithoutDevelopers.size() );
	}

	@Entity(name = "Project")
	@IdClass(ProjectId.class)
	public static class Project {

		@Id
		private String orgId;

		@Id
		private Long projectId;

		private String title;

		@ManyToMany
		private final List<Developer> developers = new ArrayList<>();
	}

	public static class ProjectId implements Serializable {
		private String orgId;
		private Long projectId;

		public ProjectId() {
		}

		public ProjectId(String orgId, Long projectId) {
			this.orgId = orgId;
			this.projectId = projectId;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ProjectId that = (ProjectId) o;

			return Objects.equals(orgId, that.orgId) && Objects.equals(projectId, that.projectId);
		}

		@Override
		public int hashCode() {
            return Objects.hash( orgId, projectId );
		}
	}

	@Entity(name = "Developer")
	@IdClass(DeveloperId.class)
	public static class Developer {

		@Id
		private String companyId;

		@Id
		private Long developerId;

		private String name;
	}

	public static class DeveloperId implements Serializable {
		private String companyId;
		private Long developerId;

		public DeveloperId() {
		}

		public DeveloperId(String companyId, Long developerId) {
			this.companyId = companyId;
			this.developerId = developerId;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			DeveloperId that = (DeveloperId) o;

			return Objects.equals(companyId, that.companyId) && Objects.equals(developerId, that.developerId);
		}

		@Override
		public int hashCode() {
            return Objects.hash( companyId, developerId );
		}
	}
}
