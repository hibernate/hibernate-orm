/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping.dynamic;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import jakarta.persistence.sql.ColumnMapping;
import jakarta.persistence.sql.ConstructorMapping;
import jakarta.persistence.sql.EntityMapping;
import org.hibernate.annotations.NaturalId;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for [jakarta.persistence.EntityManagerFactory#getResultSetMappings]
/// based on named mappings.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = { NamedToDynamicTests.Book.class, NamedToDynamicTests.DropDownItem.class })
@SessionFactory
public class NamedToDynamicTests {
	@Test
	void testConstructorConversions(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		var mappingsToBook = sessionFactory.getResultSetMappings( Book.class );
		assertThat( mappingsToBook ).containsKey( "book-dto" );
		var bootDto = (ConstructorMapping<Book>) mappingsToBook.get( "book-dto" );
		assertThat( bootDto.getJavaType() ).isEqualTo( Book.class );
		assertThat( bootDto.arguments() ).hasSize( 4 );

		var mappingsToDropDownItem = sessionFactory.getResultSetMappings( DropDownItem.class );
		assertThat( mappingsToDropDownItem ).containsKey( "book-drop-down" );
		var bookDropDown = (ConstructorMapping<DropDownItem>) mappingsToDropDownItem.get( "book-drop-down" );
		assertThat( bookDropDown.getJavaType() ).isEqualTo( DropDownItem.class );
		assertThat( bookDropDown.arguments() ).hasSize( 2 );
	}

	@Test
	void testColumnConversions(SessionFactoryScope factoryScope) {
		var sessionFactory = factoryScope.getSessionFactory();

		var integerMappings = sessionFactory.getResultSetMappings( Integer.class );
		assertThat( integerMappings ).containsKey( "id" );
		var idMapping = (ColumnMapping<Integer>) integerMappings.get( "id" );
		assertThat( idMapping.getJavaType() ).isEqualTo( Integer.class );
		assertThat( idMapping.columnName() ).isEqualTo( "id" );
	}

	@Test
	void testEntityConversions(SessionFactoryScope factoryScope) {
		var sessionFactory = factoryScope.getSessionFactory();

		var bookMappings = sessionFactory.getResultSetMappings( Book.class );

		assertThat( bookMappings ).containsKey( "book-implicit" );
		var bookImplicitMapping = (EntityMapping<Book>) bookMappings.get( "book-implicit" );
		assertThat( bookImplicitMapping.getJavaType() ).isEqualTo( Book.class );
		assertThat( bookImplicitMapping.discriminatorColumn() ).isNull();
		assertThat( bookImplicitMapping.fields() ).isEmpty();

		assertThat( bookMappings ).containsKey( "book-explicit" );
		var bookExplicitMapping = (EntityMapping<Book>) bookMappings.get( "book-explicit" );
		assertThat( bookExplicitMapping.getJavaType() ).isEqualTo( Book.class );
		assertThat( bookExplicitMapping.discriminatorColumn() ).isNull();
		assertThat( bookExplicitMapping.fields() ).hasSize( 4 );
		// "unfortunately" our memento objects do not keep the ordering
	}

	@SuppressWarnings("FieldCanBeLocal")
	@Entity(name="Book")
	@Table(name="books")
	@SqlResultSetMapping( name = "book-dto",
			classes = @ConstructorResult( targetClass = Book.class,
					columns = {
							@ColumnResult( name="id", type = Integer.class ),
							@ColumnResult( name="name", type = String.class ),
							@ColumnResult( name="isbn", type = String.class ),
							@ColumnResult( name="published", type = LocalDate.class )
					}
			)
	)
	@SqlResultSetMapping( name = "book-drop-down",
			classes = @ConstructorResult( targetClass = DropDownItem.class,
					columns = {
							@ColumnResult( name="id", type = Integer.class ),
							@ColumnResult( name="name", type = String.class )
					}
			)
	)
	@SqlResultSetMapping( name = "id",
			columns = @ColumnResult( name="id", type = Integer.class )
	)
	@SqlResultSetMapping(name = "book-implicit",
			entities = @EntityResult(entityClass = Book.class)
	)
	@SqlResultSetMapping(name = "book-explicit",
			entities = @EntityResult(
					entityClass = Book.class,
					fields = {
							@FieldResult( name = "id", column = "id_"),
							@FieldResult( name = "name", column = "name_"),
							@FieldResult( name = "isbn", column = "isbn_"),
							@FieldResult( name = "published", column = "published_"),
					}
			)
	)
	public static class Book {
		@Id
		private Integer id;
		private String name;
		@NaturalId
		private String isbn;
		private LocalDate published;

		public Book() {
		}

		public Book(Integer id, String name, String isbn, LocalDate published) {
			this.id = id;
			this.name = name;
			this.isbn = isbn;
			this.published = published;
		}
	}

	@SuppressWarnings("FieldCanBeLocal")
	public static class DropDownItem {
		private final Integer key;
		private final String text;

		public DropDownItem(Integer key, String text) {
			this.key = key;
			this.text = text;
		}
	}
}
