/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
public class LazyCollectionDeletedAllowProxyTest extends BaseNonConfigCoreFunctionalTestCase {

	private Long postId;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Post.class, Tag.class, AdditionalDetails.class, Label.class };
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );

		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		ssrb.applySetting( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
	}

	@Test
	public void updatingAnAttributeDoesNotDeleteLazyCollectionsTest() {
		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "from AdditionalDetails where id = :id" );
			query.setParameter( "id", postId );
			AdditionalDetails additionalDetails = (AdditionalDetails) query.getSingleResult();
			additionalDetails.setDetails( "New data" );
			s.persist( additionalDetails );
		} );

		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "from Post where id = :id" );
			query.setParameter( "id", postId );
			Post retrievedPost = (Post) query.getSingleResult();

			assertFalse( "No tags found", retrievedPost.getTags().isEmpty() );
			retrievedPost.getTags().forEach( tag -> assertNotNull( tag ) );
		} );

		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "from AdditionalDetails where id = :id" );
			query.setParameter( "id", postId );
			AdditionalDetails additionalDetails = (AdditionalDetails) query.getSingleResult();

			Post post = additionalDetails.getPost();
			assertIsEnhancedProxy( post );
			post.setMessage( "new message" );
		} );

		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "from Post where id = :id" );
			query.setParameter( "id", postId );
			Post retrievedPost = (Post) query.getSingleResult();

			assertEquals( "new message", retrievedPost.getMessage() );
			assertFalse( "No tags found", retrievedPost.getTags().isEmpty() );
			retrievedPost.getTags().forEach( tag -> {
				assertNotNull( tag );
				assertFalse( "No Labels found", tag.getLabels().isEmpty() );
			} );

		} );
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			Post post = new Post();

			Tag tag1 = new Tag( "tag1" );
			Tag tag2 = new Tag( "tag2" );

			Label label1 = new Label( "label1" );
			Label label2 = new Label( "label2" );

			tag1.addLabel( label1 );
			tag2.addLabel( label2 );

			Set<Tag> tagSet = new HashSet<>();
			tagSet.add( tag1 );
			tagSet.add( tag2 );
			post.setTags( tagSet );

			AdditionalDetails details = new AdditionalDetails();
			details.setPost( post );
			post.setAdditionalDetails( details );
			details.setDetails( "Some data" );

			postId = (Long) s.save( post );
		} );
	}

	@After
	public void cleanData() {
		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "from Post" );
			List<Post> posts = query.getResultList();
			posts.forEach( post -> {
				s.delete( post );
			} );
		} );
	}


	private void assertIsEnhancedProxy(Object entity) {
		assertThat( entity, instanceOf( PersistentAttributeInterceptable.class ) );

		final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) entity;
		final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
		assertThat( interceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );
	}

	// --- //

	@Entity(name = "Tag")
	@Table(name = "TAG")
	private static class Tag {

		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToMany(cascade = CascadeType.ALL)
		Set<Label> labels;

		Tag() {
		}

		Tag(String name) {
			this.name = name;
		}

		public Set<Label> getLabels() {
			return labels;
		}

		public void setLabels(Set<Label> labels) {
			this.labels = labels;
		}

		public void addLabel(Label label) {
			if ( this.labels == null ) {
				this.labels = new HashSet<>();
			}
			this.labels.add( label );
		}
	}

	@Entity(name = "Label")
	@Table(name = "LABEL")
	public static class Label {
		@Id
		@GeneratedValue
		Long id;

		String text;

		public Label() {
		}

		public Label(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

	@Entity(name = "Post")
	@Table(name = "POST")
	private static class Post {

		@Id
		@GeneratedValue
		Long id;

		String message;

		@ManyToMany(cascade = CascadeType.ALL)
		Set<Tag> tags;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "post", cascade = CascadeType.ALL)
		AdditionalDetails additionalDetails;

		public Set<Tag> getTags() {
			return tags;
		}

		public void setTags(Set<Tag> tags) {
			this.tags = tags;
		}

		public AdditionalDetails getAdditionalDetails() {
			return additionalDetails;
		}

		public void setAdditionalDetails(AdditionalDetails additionalDetails) {
			this.additionalDetails = additionalDetails;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	@Entity(name = "AdditionalDetails")
	@Table(name = "ADDITIONAL_DETAILS")
	private static class AdditionalDetails {

		@Id
		Long id;

		String details;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
		@MapsId
		Post post;

		public String getDetails() {
			return details;
		}

		public void setDetails(String details) {
			this.details = details;
		}

		public Post getPost() {
			return post;
		}

		public void setPost(Post post) {
			this.post = post;
		}
	}
}
