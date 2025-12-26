/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.override;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.mapping.Collection;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/// The report (HHH-15510) states that `@JoinColumn` defined on the class
/// is not processed when the class is "mapped" using XML - the class is
/// really more just "specified" via XML.
///
/// @author Steve Ebersole
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/jpa/xml/joincolumn-processing.xml"
)
@Jira( "https://hibernate.atlassian.net/browse/HHH-15510" )
public class JoinColumnProcessingTests {
	@Test
	void verifyModel(DomainModelScope modelScope) {
		final var postBinding = modelScope.getEntityBinding( Post.class );

		final var poster = postBinding.getProperty( "poster" );
		final var posterValue = (org.hibernate.mapping.ManyToOne) poster.getValue();
		assertThat( posterValue.getColumns() ).hasSize( 1 );
		assertThat( posterValue.getColumns().get( 0 ).getName() ).isEqualTo( "poster_fk" );

		final var tags = postBinding.getProperty( "tags" );
		final var tagsValue = (Collection) tags.getValue();
		assertThat( tagsValue.getCollectionTable().getName() ).isEqualTo( "post_tags" );
		assertThat( tagsValue.getKey().getColumns() ).hasSize( 1 );
		assertThat( tagsValue.getKey().getColumns().get( 0 ).getName() ).isEqualTo( "post_fk" );
		assertThat( tagsValue.getKey().isCascadeDeleteEnabled() ).isTrue();
		assertThat( tagsValue.getElement().getColumns() ).hasSize( 1 );
		assertThat( tagsValue.getElement().getColumns().get( 0 ).getName() ).isEqualTo( "txt" );
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Post")
	@Table(name="posts")
	public static class Post {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		@JoinColumn(name="poster_fk")
		private Person poster;
		@ElementCollection
		@CollectionTable(name = "post_tags",
				joinColumns = @JoinColumn(name = "post_fk")
		)
		@OnDelete(action = OnDeleteAction.CASCADE)
		@Column(name = "txt")
		private Set<String> tags;
	}
}
