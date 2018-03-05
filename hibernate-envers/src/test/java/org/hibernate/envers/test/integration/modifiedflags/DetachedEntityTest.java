/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * Test that an updated detached entity will still properly track {@code withModifiedFlag}
 * values correctly rather than always triggering that a field was modified so that both a
 * detached and attached entity result in the same {@code withModifiedFlag} settings.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-8973")
public class DetachedEntityTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] { Project.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		final Session s = openSession();
		try {
			// revision 1 - persist the project entity
			s.getTransaction().begin();
			final Project project = new Project( 1, "fooName" );
			s.persist( project );
			s.getTransaction().commit();

			// detach the project entity
			s.clear();

			// revision 2 to 6 - update the detached project entity.
			for( int i = 0; i < 5; ++i ) {
				s.getTransaction().begin();
				project.setName( "fooName" + ( i + 2 ) );
				s.update( project );
				s.getTransaction().commit();
				s.clear();
			}
		}
		catch ( Throwable t ) {
			if ( s.getTransaction().isActive() ) {
				s.getTransaction().rollback();
			}
			throw t;
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2, 3, 4, 5, 6 ), getAuditReader().getRevisions( Project.class, 1 ) );
	}

	@Test
	public void testRevisionHistory() {
		for ( Integer revision : Arrays.asList( 1, 2, 3, 4, 5, 6 ) ) {
			final Project project = getAuditReader().find( Project.class, 1, revision );
			if ( revision == 1 ) {
				assertEquals( new Project( 1, "fooName" ), project );
			}
			else {
				assertEquals( new Project( 1, "fooName" + revision ), project );
			}
		}
	}

	@Test
	public void testModifiedFlagChangesForProjectType() {
		final List results = getAuditReader().createQuery()
				.forRevisionsOfEntity( Project.class, false, true )
				.add( AuditEntity.property( "type" ).hasChanged() )
				.addProjection( AuditEntity.revisionNumber() )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();
		assertEquals( Arrays.asList( 1 ), results );
	}

	@Test
	public void testModifiedFlagChangesForProjectName() {
		final List results = getAuditReader().createQuery()
				.forRevisionsOfEntity( Project.class, false, true )
				.add( AuditEntity.property( "name" ).hasChanged() )
				.addProjection( AuditEntity.revisionNumber() )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();
		assertEquals( Arrays.asList( 1, 2, 3, 4, 5, 6 ), results );
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
