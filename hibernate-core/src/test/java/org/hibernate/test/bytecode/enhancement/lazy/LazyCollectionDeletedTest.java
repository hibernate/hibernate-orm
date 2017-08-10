/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-11576" )
@RunWith( BytecodeEnhancerRunner.class )
public class LazyCollectionDeletedTest extends BaseCoreFunctionalTestCase {

    private Long postId;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Post.class, Tag.class, AdditionalDetails.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            Post post = new Post();

            Tag tag1 = new Tag( "tag1" );
            Tag tag2 = new Tag( "tag2" );

            Set<Tag> tagSet = new HashSet<>();
            tagSet.add( tag1 );
            tagSet.add( tag2 );
            post.tags = tagSet;

            AdditionalDetails details = new AdditionalDetails();
            details.post = post;
            details.details = "Some data";
            post.additionalDetails = details;

            postId = (Long) s.save( post );
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            Query query = s.createQuery( "from AdditionalDetails where id=" + postId );
            AdditionalDetails additionalDetails = (AdditionalDetails) query.getSingleResult();
            additionalDetails.details = "New data";
            s.persist( additionalDetails );

            // additionalDetais.post.tags get deleted on commit
        } );

        doInHibernate( this::sessionFactory, s -> {
            Query query = s.createQuery( "from Post where id=" + postId );
            Post retrievedPost = (Post) query.getSingleResult();

            assertFalse( "No tags found", retrievedPost.tags.isEmpty() );
            retrievedPost.tags.forEach( tag -> System.out.println( "Found tag: " + tag ) );
        } );
    }

    // --- //

    @Entity( name = "Tag" )
    @Table( name = "TAG" )
    private static class Tag {

        @Id
        @GeneratedValue
        Long id;

        String name;

        Tag() {
        }

        Tag(String name) {
            this.name = name;
        }
    }

    @Entity( name = "Post" )
    @Table( name = "POST" )
    private static class Post {

        @Id
        @GeneratedValue
        Long id;

        @ManyToMany( cascade = CascadeType.ALL )
        Set<Tag> tags;

        @OneToOne( fetch = FetchType.LAZY, mappedBy = "post", cascade = CascadeType.ALL )
        AdditionalDetails additionalDetails;
    }

    @Entity( name = "AdditionalDetails" )
    @Table( name = "ADDITIONAL_DETAILS" )
    private static class AdditionalDetails {

        @Id
        Long id;

        String details;

        @OneToOne( optional = false )
        @MapsId
        Post post;
    }
}