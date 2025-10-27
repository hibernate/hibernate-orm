/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.hibernate.Session;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = {
				WhereJoinTableTest.Book.class,
				WhereJoinTableTest.Reader.class
		},
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = WhereJoinTableTest.CollectionClassificationProvider.class
		)
)
public class WhereJoinTableTest {

	public static class CollectionClassificationProvider implements SettingProvider.Provider<CollectionClassification> {
		@Override
		public CollectionClassification getSetting() {
			return CollectionClassification.BAG;
		}
	}

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		//tag::pc-where-persistence-example[]
		scope.inTransaction( entityManager -> {

			entityManager.unwrap( Session.class ).doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate(
							"ALTER TABLE Book_Reader ADD created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
					);
				}
			} );

			//tag::pc-where-join-table-persist-example[]
			Book book = new Book();
			book.setId( 1L );
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vad Mihalcea" );
			entityManager.persist( book );

			Reader reader1 = new Reader();
			reader1.setId( 1L );
			reader1.setName( "John Doe" );
			entityManager.persist( reader1 );

			Reader reader2 = new Reader();
			reader2.setId( 2L );
			reader2.setName( "John Doe Jr." );
			entityManager.persist( reader2 );
			//end::pc-where-join-table-persist-example[]
		} );

		scope.inTransaction( entityManager -> {
			entityManager.unwrap( Session.class ).doWork( connection -> {
						try (Statement statement = connection.createStatement()) {
							//tag::pc-where-join-table-persist-example[]

							statement.executeUpdate(
									"INSERT INTO Book_Reader " +
									"	(book_id, reader_id) " +
									"VALUES " +
									"	(1, 1) "
							);
							statement.executeUpdate(
									"INSERT INTO Book_Reader " +
									"	(book_id, reader_id, created_on) " +
									"VALUES " +
									"	(1, 2, DATEADD('DAY', -10, CURRENT_TIMESTAMP())) "
							);
							//end::pc-where-join-table-persist-example[]
						}
					}
			);

			//tag::pc-where-join-table-fetch-example[]
			Book book = entityManager.find( Book.class, 1L );
			assertThat( book.getCurrentWeekReaders() ).hasSize( 1 );
			//end::pc-where-join-table-fetch-example[]
		} );
	}

	//tag::pc-where-join-table-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		@ManyToMany
		@JoinTable(
				name = "Book_Reader",
				joinColumns = @JoinColumn(name = "book_id"),
				inverseJoinColumns = @JoinColumn(name = "reader_id")
		)
		@SQLJoinTableRestriction("created_on > DATEADD('DAY', -7, CURRENT_TIMESTAMP())")
		private List<Reader> currentWeekReaders = new ArrayList<>();

		//Getters and setters omitted for brevity

		//end::pc-where-join-table-example[]
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

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public List<Reader> getCurrentWeekReaders() {
			return currentWeekReaders;
		}

		//tag::pc-where-join-table-example[]
	}

	@Entity(name = "Reader")
	public static class Reader {

		@Id
		private Long id;

		private String name;

		//Getters and setters omitted for brevity

		//end::pc-where-join-table-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		//tag::pc-where-join-table-example[]
	}
	//end::pc-where-join-table-example[]
}
