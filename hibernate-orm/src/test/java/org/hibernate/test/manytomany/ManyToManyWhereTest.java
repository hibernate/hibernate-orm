/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomany;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Verifies that @ManyToMany relationships with a @Where clause properly
 * loads the collection for issue HHH-9084.
 *
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-9084" )
public class ManyToManyWhereTest extends BaseCoreFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Advertisement.class,
            Attachment.class,
            PageAdvertisement.class,
            SubjectAdvertisement.class
        };
    }

    @Test
    public void testManyToManyWithWhereConditional() {
        Session session = openSession();
        Transaction transaction = session.beginTransaction();
        // create advertisements
        Advertisement advertisement1 = new Advertisement();
        Advertisement advertisement2 = new Advertisement();
        session.saveOrUpdate( advertisement1 );
        session.saveOrUpdate( advertisement2 );
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
        session.saveOrUpdate( a1 );
        session.saveOrUpdate( a2 );
        transaction.commit();
        session.close();

        // create page advertisement relationships with advertisements
        session = openSession();
        transaction = session.beginTransaction();
        List<Advertisement> advertisements = (List<Advertisement>) session.createQuery( "FROM Advertisement" ).list();
        assertEquals( advertisements.size(), 2 );
        for ( Advertisement advertisement : advertisements ) {
            PageAdvertisement pageAd = new PageAdvertisement();
            pageAd.setAdvertisement( advertisement );
            session.persist( pageAd );
        }
        transaction.commit();
        session.close();

        // query relationships and verify @Where condition fragment applies correctly.
        session = openSession();
        transaction = session.beginTransaction();
        List<PageAdvertisement> ads = (List<PageAdvertisement>) session.createQuery( "FROM PageAdvertisement" ).list();
        assertEquals( ads.size(), 2 );
        for ( PageAdvertisement ad : ads ) {
            // there is only 1 not deleted attachment
            assertEquals( ad.getAdvertisement().getAttachments().size(), 1 );
            for( Attachment attachment : ad.getAdvertisement().getAttachments() ) {
                // each attachment linked with two advertisements
                assertEquals( attachment.getAdvertisements().size(), 2 );
            }
        }
        transaction.commit();
        session.close();
    }
}
