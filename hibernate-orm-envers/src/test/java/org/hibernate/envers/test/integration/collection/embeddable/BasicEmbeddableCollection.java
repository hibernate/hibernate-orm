/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import junit.framework.Assert;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6613")
public class BasicEmbeddableCollection extends BaseEnversJPAFunctionalTestCase {
	private int id = -1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {DarkCharacter.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - empty element collection
		em.getTransaction().begin();
		DarkCharacter darkCharacter = new DarkCharacter( 1, 1 );
		em.persist( darkCharacter );
		em.getTransaction().commit();

		id = darkCharacter.getId();

		// Revision 2 - adding collection element
		em.getTransaction().begin();
		darkCharacter = em.find( DarkCharacter.class, darkCharacter.getId() );
		darkCharacter.getNames().add( new Name( "Action", "Hank" ) );
		darkCharacter = em.merge( darkCharacter );
		em.getTransaction().commit();

		// Revision 3 - adding another collection element
		em.getTransaction().begin();
		darkCharacter = em.find( DarkCharacter.class, darkCharacter.getId() );
		darkCharacter.getNames().add( new Name( "Green", "Lantern" ) );
		darkCharacter = em.merge( darkCharacter );
		em.getTransaction().commit();

		// Revision 4 - removing single collection element
		em.getTransaction().begin();
		darkCharacter = em.find( DarkCharacter.class, darkCharacter.getId() );
		darkCharacter.getNames().remove( new Name( "Action", "Hank" ) );
		darkCharacter = em.merge( darkCharacter );
		em.getTransaction().commit();

		// Revision 5 - removing all collection elements
		em.getTransaction().begin();
		darkCharacter = em.find( DarkCharacter.class, darkCharacter.getId() );
		darkCharacter.getNames().clear();
		darkCharacter = em.merge( darkCharacter );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionsCount() {
		Assert.assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), getAuditReader().getRevisions( DarkCharacter.class, id ) );
	}

	@Test
	public void testHistoryOfCharacter() {
		DarkCharacter darkCharacter = new DarkCharacter( id, 1 );

		DarkCharacter ver1 = getAuditReader().find( DarkCharacter.class, id, 1 );

		Assert.assertEquals( darkCharacter, ver1 );
		Assert.assertEquals( 0, ver1.getNames().size() );

		darkCharacter.getNames().add( new Name( "Action", "Hank" ) );
		DarkCharacter ver2 = getAuditReader().find( DarkCharacter.class, id, 2 );

		Assert.assertEquals( darkCharacter, ver2 );
		Assert.assertEquals( darkCharacter.getNames(), ver2.getNames() );

		darkCharacter.getNames().add( new Name( "Green", "Lantern" ) );
		DarkCharacter ver3 = getAuditReader().find( DarkCharacter.class, id, 3 );

		Assert.assertEquals( darkCharacter, ver3 );
		Assert.assertEquals( darkCharacter.getNames(), ver3.getNames() );

		darkCharacter.getNames().remove( new Name( "Action", "Hank" ) );
		DarkCharacter ver4 = getAuditReader().find( DarkCharacter.class, id, 4 );

		Assert.assertEquals( darkCharacter, ver4 );
		Assert.assertEquals( darkCharacter.getNames(), ver4.getNames() );

		darkCharacter.getNames().clear();
		DarkCharacter ver5 = getAuditReader().find( DarkCharacter.class, id, 5 );

		Assert.assertEquals( darkCharacter, ver5 );
		Assert.assertEquals( darkCharacter.getNames(), ver5.getNames() );
	}
}
