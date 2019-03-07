/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.hashcode;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.hashcode.WikiImage;
import org.hibernate.envers.test.support.domains.hashcode.WikiPage;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChangingHashcodeTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long pageId;
	private Long imageId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { WikiPage.class, WikiImage.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		//		This is currently a workaround to get the test to pass.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					WikiPage page = new WikiPage( "title", "content" );
					entityManager.persist( page );

					this.pageId = page.getId();
				},

				// Revision 2
				entityManager -> {
					WikiImage image = new WikiImage( "name1" );
					entityManager.persist( image );

					WikiPage page = entityManager.find( WikiPage.class, pageId );
					page.getImages().add( image );

					this.imageId = image.getId();
				},

				// Revision 3
				entityManager -> {
					WikiImage image = entityManager.find( WikiImage.class, imageId );
					image.setName( "name2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( WikiPage.class, pageId ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( WikiImage.class, imageId ), contains( 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfImage() {
		assertThat( getAuditReader().find( WikiImage.class, imageId, 1 ), nullValue() );
		assertThat( getAuditReader().find( WikiImage.class, imageId, 2 ), equalTo( new WikiImage( "name1" ) ) );
		assertThat( getAuditReader().find( WikiImage.class, imageId, 3 ), equalTo( new WikiImage( "name2" ) ) );
	}

	@DynamicTest
	public void testHistoryOfPage() {
		assertThat( getAuditReader().find( WikiPage.class, pageId, 1 ).getImages(), CollectionMatchers.isEmpty() );
		assertThat( getAuditReader().find( WikiPage.class, pageId, 2 ).getImages(), contains( new WikiImage( "name1" ) ) );
		assertThat( getAuditReader().find( WikiPage.class, pageId, 3 ).getImages(), contains( new WikiImage( "name2" ) ) );
	}
}