/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.embeddedid;

import java.util.Arrays;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test avoiding the creating of a foreign key constraint for an embedded identifier that
 * contains a many-to-one relationship, allowing the removal of the base table entity
 * without throwing a foreign-key constraint exception due to historical audit rows.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11107")
public class RelationInsideEmbeddableRemoveTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CorrectChild.class, IncorrectChild.class, Parent.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			// Revision 1
			entityManager.getTransaction().begin();
			Parent parent = new Parent( "Parent" );
			parent.addIncorrectChild( 1 );
			parent.addCorrectChild( 1 );
			entityManager.persist( parent );
			for ( IncorrectChild child : parent.getIncorrectChildren() ) {
				entityManager.persist( child );
			}
			for ( CorrectChild child : parent.getCorrectChildren() ) {
				entityManager.persist( child );
			}
			entityManager.getTransaction().commit();

			// Revision 2
			entityManager.getTransaction().begin();
			for ( IncorrectChild child : parent.getIncorrectChildren() ) {
				entityManager.remove( child );
			}
			parent.getIncorrectChildren().clear();
			for( CorrectChild child : parent.getCorrectChildren() ) {
				entityManager.remove( child );
			}
			parent.getCorrectChildren().clear();
			entityManager.getTransaction().commit();

			// Revision 3
			// This fails because of referential integrity constraints without fix.
			entityManager.getTransaction().begin();
			entityManager.remove( parent );
			entityManager.getTransaction().commit();
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( Parent.class, "Parent" ) );
	}
}
