/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.hashcode;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {WikiPage.class, WikiImage.class})
public class ChangingHashcode {
	private Long pageId;
	private Long imageId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			WikiPage page = new WikiPage( "title", "content" );
			em.persist( page );
			pageId = page.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			WikiImage image = new WikiImage( "name1" );
			em.persist( image );

			WikiPage page = em.find( WikiPage.class, pageId );
			page.getImages().add( image );

			imageId = image.getId();
		} );

		// Revision 3
		scope.inTransaction( em -> {
			WikiImage image = em.find( WikiImage.class, imageId );
			image.setName( "name2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( WikiPage.class, pageId ) );
			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( WikiImage.class, imageId ) );
		} );
	}

	@Test
	public void testHistoryOfImage(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertNull( auditReader.find( WikiImage.class, imageId, 1 ) );
			assertEquals( new WikiImage( "name1" ), auditReader.find( WikiImage.class, imageId, 2 ) );
			assertEquals( new WikiImage( "name2" ), auditReader.find( WikiImage.class, imageId, 3 ) );
		} );
	}

	@Test
	public void testHistoryOfPage(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 0, auditReader.find( WikiPage.class, pageId, 1 ).getImages().size() );
			assertEquals(
					TestTools.makeSet( new WikiImage( "name1" ) ),
					auditReader.find( WikiPage.class, pageId, 2 ).getImages()
			);
			assertEquals(
					TestTools.makeSet( new WikiImage( "name2" ) ),
					auditReader.find( WikiPage.class, pageId, 3 ).getImages()
			);
		} );
	}
}
