/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Luis Barreiro
 */
@JiraKey("HHH-11576")
@DomainModel(
		annotatedClasses = {
				LazyCollectionDeletedTest.Post.class,
				LazyCollectionDeletedTest.Tag.class,
				LazyCollectionDeletedTest.AdditionalDetails.class
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
public class LazyCollectionDeletedTest {

	private Long postId;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
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
			s.persist( post );
			postId = post.id;
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Query query = s.createQuery( "from AdditionalDetails where id=" + postId );
			AdditionalDetails additionalDetails = (AdditionalDetails) query.getSingleResult();
			additionalDetails.details = "New data";
			s.persist( additionalDetails );

			// additionalDetails.post.tags get deleted on commit
		} );

		scope.inTransaction( s -> {
			Query query = s.createQuery( "from Post where id=" + postId );
			Post retrievedPost = (Post) query.getSingleResult();

			assertFalse( retrievedPost.tags.isEmpty(), "No tags found" );
			retrievedPost.tags.forEach( tag -> System.out.println( "Found tag: " + tag ) );
		} );
	}

	// --- //

	@Entity( name = "Tag" )
	@Table( name = "TAG" )
	static class Tag {

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
	static class Post {

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
	static class AdditionalDetails {

		@Id
		Long id;

		String details;

		@OneToOne( optional = false )
		@MapsId
		Post post;
	}
}
