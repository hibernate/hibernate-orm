/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.hashcode;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChangingHashcode extends BaseEnversJPAFunctionalTestCase {
	private Long pageId;
	private Long imageId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {WikiPage.class, WikiImage.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		WikiPage page = new WikiPage( "title", "content" );
		em.persist( page );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		WikiImage image = new WikiImage( "name1" );
		em.persist( image );

		page = em.find( WikiPage.class, page.getId() );
		page.getImages().add( image );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		image = em.find( WikiImage.class, image.getId() );
		image.setName( "name2" );

		em.getTransaction().commit();

		pageId = page.getId();
		imageId = image.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( WikiPage.class, pageId ) );
		assert Arrays.asList( 2, 3 ).equals( getAuditReader().getRevisions( WikiImage.class, imageId ) );
	}

	@Test
	public void testHistoryOfImage() {
		assert getAuditReader().find( WikiImage.class, imageId, 1 ) == null;
		assert getAuditReader().find( WikiImage.class, imageId, 2 ).equals( new WikiImage( "name1" ) );
		assert getAuditReader().find( WikiImage.class, imageId, 3 ).equals( new WikiImage( "name2" ) );
	}

	@Test
	public void testHistoryOfPage() {
		assert getAuditReader().find( WikiPage.class, pageId, 1 ).getImages().size() == 0;
		assert getAuditReader().find( WikiPage.class, pageId, 2 ).getImages().equals(
				TestTools.makeSet(
						new WikiImage(
								"name1"
						)
				)
		);
		assert getAuditReader().find( WikiPage.class, pageId, 3 ).getImages().equals(
				TestTools.makeSet(
						new WikiImage(
								"name2"
						)
				)
		);
	}
}