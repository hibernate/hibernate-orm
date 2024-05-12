/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-11147")
@DomainModel(
		annotatedClasses = {
				LazyCollectionDeletedAllowProxyTest.Post.class,
				LazyCollectionDeletedAllowProxyTest.Tag.class,
				LazyCollectionDeletedAllowProxyTest.AdditionalDetails.class,
				LazyCollectionDeletedAllowProxyTest.Label.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class LazyCollectionDeletedAllowProxyTest {

	private Long postId;

	@Test
	public void updatingAnAttributeDoesNotDeleteLazyCollectionsTest(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Query query = s.createQuery( "from AdditionalDetails where id = :id" );
			query.setParameter( "id", postId );
			AdditionalDetails additionalDetails = (AdditionalDetails) query.getSingleResult();
			additionalDetails.setDetails( "New data" );
			s.persist( additionalDetails );
		} );

		scope.inTransaction( s -> {
			Query query = s.createQuery( "from Post where id = :id" );
			query.setParameter( "id", postId );
			Post retrievedPost = (Post) query.getSingleResult();

			assertFalse( retrievedPost.getTags().isEmpty(), "No tags found" );
			retrievedPost.getTags().forEach( tag -> assertNotNull( tag ) );
		} );

		scope.inTransaction( s -> {
			Query query = s.createQuery( "from AdditionalDetails where id = :id" );
			query.setParameter( "id", postId );
			AdditionalDetails additionalDetails = (AdditionalDetails) query.getSingleResult();

			Post post = additionalDetails.getPost();
			assertIsEnhancedProxy( post );
			post.setMessage( "new message" );
		} );

		scope.inTransaction( s -> {
			Query query = s.createQuery( "from Post where id = :id" );
			query.setParameter( "id", postId );
			Post retrievedPost = (Post) query.getSingleResult();

			assertEquals( "new message", retrievedPost.getMessage() );
			assertFalse( retrievedPost.getTags().isEmpty(), "No tags found" );
			retrievedPost.getTags().forEach( tag -> {
				assertNotNull( tag );
				assertFalse( tag.getLabels().isEmpty(), "No Labels found" );
			} );

		} );
	}

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
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

	@AfterEach
	public void cleanData(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
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
	static class Tag {

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
	static class Post {

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
	static class AdditionalDetails {

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
