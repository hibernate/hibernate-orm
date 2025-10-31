/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test that an updated detached entity will still properly track {@code withModifiedFlag}
 * values correctly rather than always triggering that a field was modified so that both a
 * detached and attached entity result in the same {@code withModifiedFlag} settings.
 *
 * @author Chris Cranford
 */
@JiraKey("HHH-8973")
@EnversTest
@Jpa(annotatedClasses = { DetachedEntityTest.Project.class })
public class DetachedEntityTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// revision 1 - persist the project entity
			final Project project = new Project( 1, "fooName" );
			em.persist( project );
		} );

		// revision 2 to 6 - update the detached project entity.
		for ( int i = 0; i < 5; ++i ) {
			final int index = i;
			scope.inTransaction( em -> {
				final Project project = em.find( Project.class, 1 );
				em.detach( project );
				project.setName( "fooName" + ( index + 2 ) );
				em.merge( project );
			} );
		}
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4, 5, 6 ), auditReader.getRevisions( Project.class, 1 ) );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			for ( Integer revision : Arrays.asList( 1, 2, 3, 4, 5, 6 ) ) {
				final Project project = auditReader.find( Project.class, 1, revision );
				if ( revision == 1 ) {
					assertEquals( new Project( 1, "fooName" ), project );
				}
				else {
					assertEquals( new Project( 1, "fooName" + revision ), project );
				}
			}
		} );
	}

	@Test
	public void testModifiedFlagChangesForProjectType(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final List results = auditReader.createQuery()
					.forRevisionsOfEntity( Project.class, false, true )
					.add( AuditEntity.property( "type" ).hasChanged() )
					.addProjection( AuditEntity.revisionNumber() )
					.addOrder( AuditEntity.revisionNumber().asc() )
					.getResultList();
			assertEquals( Arrays.asList( 1 ), results );
		} );
	}

	@Test
	public void testModifiedFlagChangesForProjectName(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final List results = auditReader.createQuery()
					.forRevisionsOfEntity( Project.class, false, true )
					.add( AuditEntity.property( "name" ).hasChanged() )
					.addProjection( AuditEntity.revisionNumber() )
					.addOrder( AuditEntity.revisionNumber().asc() )
					.getResultList();
			assertEquals( Arrays.asList( 1, 2, 3, 4, 5, 6 ), results );
		} );
	}

	@Entity(name = "Project")
	@Audited(withModifiedFlag = true)
	public static class Project {
		@Id
		private Integer id;
		private String name;
		private String type;

		Project() {

		}

		Project(Integer id, String name) {
			this( id, name, "fooType" );
		}

		Project(Integer id, String name, String type) {
			this.id = id;
			this.name = name;
			this.type = type;
		}

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

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Project project = (Project) o;

			if ( id != null ? !id.equals( project.id ) : project.id != null ) {
				return false;
			}
			if ( name != null ? !name.equals( project.name ) : project.name != null ) {
				return false;
			}
			return type != null ? type.equals( project.type ) : project.type == null;
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + ( name != null ? name.hashCode() : 0 );
			result = 31 * result + ( type != null ? type.hashCode() : 0 );
			return result;
		}
	}
}
