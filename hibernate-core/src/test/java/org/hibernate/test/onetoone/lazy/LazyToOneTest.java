/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.lazy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import java.util.Date;

import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;

@TestForIssue(jiraKey = "HHH-12842")
public class LazyToOneTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Post.class, PostDetails.class};
    }

    @Before
    public void setUp() throws Exception {
        doInHibernate( this::sessionFactory, s -> {
            Post post = new Post();
            post.setDetails(new PostDetails());
            post.getDetails().setCreatedBy("ME");
            post.getDetails().setCreatedOn(new Date());
            post.setTitle("title");
            s.persist(post);
        } );
    }

    @Test
    public void testOneToOneLazyLoading() {
        doInHibernate( this::sessionFactory, s -> {
            PostDetails post = (PostDetails) s.createQuery("select a from PostDetails a").getResultList().get(0);
            assertFalse(isInitialized(post.post));
        } );
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @MapsId
        @OneToOne(fetch = FetchType.LAZY, mappedBy = "details", optional = false)
        private Post post;

        public PostDetails() {}

        public PostDetails(String createdBy) {
            createdOn = new Date();
            this.createdBy = createdBy;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @PrimaryKeyJoinColumn
        @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private PostDetails details;


        public void setDetails(PostDetails details) {
            if (details == null) {
                if (this.details != null) {
                    this.details.setPost(null);
                }
            }
            else {
                details.setPost(this);
            }
            this.details = details;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public PostDetails getDetails() {
            return details;
        }
    }

}
