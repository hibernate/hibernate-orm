/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;


@DomainModel(
		annotatedClasses = {
				CollectionInImmutableEmbeddableTest.MainEntity.class,
				CollectionInImmutableEmbeddableTest.ImmutableEmbeddable.class,
				CollectionInImmutableEmbeddableTest.OtherEntity.class,
		}
)
@SessionFactory
public class CollectionInImmutableEmbeddableTest {

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20273")
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var mainEntity = new MainEntity( 1, "main1" );
					var other1 = new OtherEntity( 1, "other1" );
					mainEntity.embeddable.otherEntities.add( other1 );
					session.persist( other1 );
					session.persist( mainEntity );
					session.flush();

					var other2 = new OtherEntity( 2, "other2" );
					session.persist( other2 );
					mainEntity.embeddable = new ImmutableEmbeddable( new HashSet<>() );
					mainEntity.embeddable.otherEntities.add( other1 );
					mainEntity.embeddable.otherEntities.add( other2 );
				}
		);
	}

	@Entity(name = "Customer")
	public static class MainEntity {
		@Id
		public Integer id;

		public String name;

		@Embedded
		public ImmutableEmbeddable embeddable = new ImmutableEmbeddable( new HashSet<>() );

		public MainEntity() {
		}

		public MainEntity(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public ImmutableEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(ImmutableEmbeddable embeddable) {
			this.embeddable = embeddable;
		}
	}

	@Embeddable
	public record ImmutableEmbeddable(
			@OneToMany
			Set<OtherEntity> otherEntities) {
	}

	@Entity(name = "OtherEntity")
	public static class OtherEntity {
		@Id
		public Integer id;

		public String description;

		public OtherEntity() {
		}

		public OtherEntity(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		public Integer getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}
	}
}
