/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that @ManyToMany relationships with a @Where clause properly
 * loads the collection for issue HHH-9084.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-9084")
@DomainModel(
		annotatedClasses = {
				Advertisement.class,
				Attachment.class,
				PageAdvertisement.class,
				SubjectAdvertisement.class
		}
)
@SessionFactory
public class ManyToManyWhereTest {

	@Test
	public void testManyToManyWithWhereConditional(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// create advertisements
					Advertisement advertisement1 = new Advertisement();
					Advertisement advertisement2 = new Advertisement();
					session.persist( advertisement1 );
					session.persist( advertisement2 );
					// create attachment relationships to advertisements
					Attachment a1 = new Attachment();
					a1.setFileName( "memo.txt" );
					a1.setAdvertisements( new LinkedHashSet<>( Arrays.asList( advertisement1, advertisement2 ) ) );
					Attachment a2 = new Attachment();
					a2.setFileName( "mickeymouse.jpg" );
					a2.setDeleted( "true" );
					a2.setAdvertisements( new LinkedHashSet<>( Arrays.asList( advertisement1, advertisement2 ) ) );
					advertisement1.setAttachments( new HashSet<>( Arrays.asList( a1, a2 ) ) );
					advertisement2.setAttachments( new HashSet<>( Arrays.asList( a1, a2 ) ) );
					session.persist( a1 );
					session.persist( a2 );
				}
		);

		scope.inTransaction(
				session -> {
					// create page advertisement relationships with advertisements
					List<Advertisement> advertisements = (List<Advertisement>) session.createQuery( "FROM Advertisement" )
							.list();
					assertEquals( 2, advertisements.size() );
					for ( Advertisement advertisement : advertisements ) {
						PageAdvertisement pageAd = new PageAdvertisement();
						pageAd.setAdvertisement( advertisement );
						session.persist( pageAd );
					}
				}
		);

		scope.inTransaction(
				session -> {
					// query relationships and verify @Where condition fragment applies correctly.

					List<PageAdvertisement> ads = (List<PageAdvertisement>) session.createQuery(
							"FROM PageAdvertisement" ).list();
					assertEquals( 2, ads.size() );
					for ( PageAdvertisement ad : ads ) {
						// there is only 1 not deleted attachment
						assertEquals( 1, ad.getAdvertisement().getAttachments().size() );
						for ( Attachment attachment : ad.getAdvertisement().getAttachments() ) {
							// each attachment linked with two advertisements
							assertEquals( 2, attachment.getAdvertisements().size() );
						}
					}
				}
		);
	}
}
