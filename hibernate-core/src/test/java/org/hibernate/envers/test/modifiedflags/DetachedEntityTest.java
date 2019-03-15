/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test that an updated detached entity will still properly track {@code withModifiedFlag}
 * values correctly rather than always triggering that a field was modified so that both a
 * detached and attached entity result in the same {@code withModifiedFlag} settings.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-8973")
@Disabled("Requires discussion about SingleIdEntityLoader#loadDatabaseSnapshot to work like 5.x")
public class DetachedEntityTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] { Project.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inSession(
				session -> {
					// Revision 1 - Persist the project entity
					session.getTransaction().begin();
					final Project project = new Project( 1, "fooName" );
					session.save( session );
					session.getTransaction().commit();

					// detach the project entity
					session.clear();

					// Revisions 2 through 6 - Update the detached entity.
					for ( int i = 0; i < 5; ++i ) {
						session.getTransaction().begin();
						project.setName( "fooName" + ( i + 2 ) );
						session.update( project );
						session.getTransaction().commit();

						// re-detach the instance
						session.clear();
					}
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Project.class, 1 ), contains( 1, 2, 3, 4, 5, 6 ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		for ( Integer revision : Arrays.asList( 1, 2, 3, 4, 5, 6 ) ) {
			final Project project = getAuditReader().find( Project.class, 1, revision );
			final String projectName = "fooName" + ( revision > 1 ? revision : "" );
			assertThat( project, equalTo( new Project( 1, projectName ) ) );
		}
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testModifiedFlagChangesForProjectType() {
		assertThat(
				(List<Number>) getAuditReader().createQuery()
						.forRevisionsOfEntity( Project.class, false, true )
						.add( AuditEntity.property( "type" ).hasChanged() )
						.addProjection( AuditEntity.revisionNumber() )
						.addOrder( AuditEntity.revisionNumber().asc() )
						.getResultList(),
				contains( 1 )
		);
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testModifiedFlagChangesForProjectName() {
		assertThat(
				(List<Number>) getAuditReader().createQuery()
						.forRevisionsOfEntity( Project.class, false, true )
						.add( AuditEntity.property( "name" ).hasChanged() )
						.addProjection( AuditEntity.revisionNumber() )
						.addOrder( AuditEntity.revisionNumber().asc() )
						.getResultList(),
				contains( 1, 2, 3, 4, 5, 6 )
		);
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
			return Objects.equals( id, project.id ) &&
					Objects.equals( name, project.name ) &&
					Objects.equals( type, project.type );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name, type );
		}
	}
}
