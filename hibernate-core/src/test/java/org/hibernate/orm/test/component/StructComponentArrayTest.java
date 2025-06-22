/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component;

import java.util.List;

import org.hibernate.annotations.Struct;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@DomainModel(
		annotatedClasses = {
				StructComponentArrayTest.Book.class,
				StructComponentArrayTest.Label.class
		}
)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructAggregate.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
public class StructComponentArrayTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Publisher ebookPublisher = new Publisher();
					ebookPublisher.setName( "eprint" );

					Publisher paperPublisher = new Publisher();
					paperPublisher.setName( "paperbooks" );

					Book book = new Book();
					book.title = "Hibernate";
					book.author = "Steve";
					book.publishers = new Publisher[] { ebookPublisher, paperPublisher };
					book.labels = List.of( new Label( "kind", "Technical" ), new Label( "level", "Beginner" ) );

					session.persist( book );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "from Book" ).list();
				}
		);
	}


	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		private Publisher[] publishers;
		private List<Label> labels;
	}

	@Embeddable
	@Struct( name = "publisher_type")
	public static class Publisher {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	@Struct( name = "label_type")
	public static class Label {

		private String name;
		@Column(name = "val")
		private String value;

		public Label() {
		}

		public Label(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

}
