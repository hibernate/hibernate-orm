/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		OneToOneEmbeddedIdSelfReferenceTest.PkComposite.class,
		OneToOneEmbeddedIdSelfReferenceTest.Element.class,
		OneToOneEmbeddedIdSelfReferenceTest.Tag.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16363" )
public class OneToOneEmbeddedIdSelfReferenceTest {
	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUnlinkedTag(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tag tag = new Tag( new PkComposite( 1L ), "tag_1" );
			session.persist( tag );
			session.persist( new Element( new PkComposite( 2L ), "element_2", tag ) );
		} );
		scope.inTransaction( session -> {
			final Element element = session.find( Element.class, new PkComposite( 2L ) );
			assertThat( element.getTag().getName() ).isEqualTo( "tag_1" );
		} );
	}

	@Test
	public void testSimpleLinkedTags(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tag tag1 = new Tag( new PkComposite( 1L ), "tag_1" );
			final Tag tag2 = new Tag( new PkComposite( 2L ), "tag_2" );
			tag2.setLinkedTag( tag1 );
			session.persist( tag1 );
			session.persist( tag2 );
			session.persist( new Element( new PkComposite( 3L ), "element_3", tag2 ) );
		} );
		scope.inTransaction( session -> {
			final Element element = session.find( Element.class, new PkComposite( 3L ) );
			assertThat( element.getTag().getName() ).isEqualTo( "tag_2" );
			assertThat( element.getTag().getLinkedTag().getName() ).isEqualTo( "tag_1" );
			assertThat( element.getTag().getLinkedTag().getLinkedTag() ).isNull();
		} );
	}

	@Test
	public void testRecursiveLinkedTags(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tag tag1 = new Tag( new PkComposite( 1L ), "tag_1" );
			final Tag tag2 = new Tag( new PkComposite( 2L ), "tag_2" );
			tag1.setLinkedTag( tag2 );
			tag2.setLinkedTag( tag1 );
			session.persist( tag1 );
			session.persist( tag2 );
			session.persist( new Element( new PkComposite( 3L ), "element_3", tag2 ) );
		} );
		scope.inTransaction( session -> {
			final Element element = session.find( Element.class, new PkComposite( 3L ) );
			assertThat( element.getTag().getName() ).isEqualTo( "tag_2" );
			assertThat( element.getTag().getLinkedTag().getName() ).isEqualTo( "tag_1" );
			assertThat( element.getTag().getLinkedTag().getLinkedTag() ).isSameAs( element.getTag() );
		} );
	}

	@Embeddable
	public static class PkComposite implements Serializable {
		private Long id;

		public PkComposite() {
		}

		public PkComposite(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "Element" )
	@Table( name = "elements_table" )
	public static class Element {
		@EmbeddedId
		private PkComposite id;

		private String name;

		@ManyToOne( fetch = FetchType.EAGER )
		@JoinColumn( name = "tag_id" )
		private Tag tag;

		public Element() {
		}

		public Element(PkComposite id, String name, Tag tag) {
			this.id = id;
			this.name = name;
			this.tag = tag;
		}

		public Tag getTag() {
			return tag;
		}
	}

	@Entity( name = "Tag" )
	@Table( name = "tags_table" )
	public static class Tag {
		@EmbeddedId
		private PkComposite id;

		private String name;

		@OneToOne( cascade = CascadeType.ALL )
		@JoinColumn( name = "linked_tag_id" )
		private Tag linkedTag;

		public Tag() {
		}

		public Tag(PkComposite id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Tag getLinkedTag() {
			return linkedTag;
		}

		public void setLinkedTag(Tag linkedTag) {
			this.linkedTag = linkedTag;
		}
	}
}
