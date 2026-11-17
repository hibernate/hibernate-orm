package org.hibernate.orm.test.paging.keybased;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Order;
import org.hibernate.query.Page;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Key-based pagination with a case-insensitive order must look for
 * the next page the same way it orders, ignoring the case of the key.
 */
@DomainModel(annotatedClasses = {KeyBasedPagingIgnoreCaseTest.Word.class, KeyBasedPagingIgnoreCaseTest.Label.class})
@SessionFactory
@JiraKey("HHH-21161")
public class KeyBasedPagingIgnoreCaseTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Word( 1, "a" ) );
			session.persist( new Word( 2, "A" ) );
			session.persist( new Word( 3, "B" ) );
			session.persist( new Word( 4, "c" ) );
			session.persist( new Label( 1, "a" ) );
			session.persist( new Label( 2, "B" ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void ascending(SessionFactoryScope scope) {
		assertEquals( List.of( 1, 2, 3, 4 ), idsByPage( scope, Page.first( 1 ).keyedBy( List.of(
				Order.asc( Word.class, "text" ).ignoringCase(),
				Order.asc( Word.class, "id" )
		) ) ) );
	}

	@Test
	public void descending(SessionFactoryScope scope) {
		assertEquals( List.of( 4, 3, 2, 1 ), idsByPage( scope, Page.first( 1 ).keyedBy( List.of(
				Order.desc( Word.class, "text" ).ignoringCase(),
				Order.desc( Word.class, "id" )
		) ) ) );
	}

	@Test
	public void ascendingWithLargerPages(SessionFactoryScope scope) {
		assertEquals( List.of( 1, 2, 3, 4 ), idsByPage( scope, Page.first( 3 ).keyedBy( List.of(
				Order.asc( Word.class, "text" ).ignoringCase(),
				Order.asc( Word.class, "id" )
		) ) ) );
	}

	@Test
	public void convertedKey(SessionFactoryScope scope) {
		final List<Integer> ids = new ArrayList<>();
		scope.inSession( session -> {
			KeyedPage<Label> page = Page.first( 1 ).keyedBy( Order.asc( Label.class, "code" ).ignoringCase() );
			while ( true ) {
				final KeyedResultList<Label> results =
						session.createSelectionQuery( "from Label", Label.class ).getKeyedResultList( page );
				for ( Label label : results.getResultList() ) {
					ids.add( label.id );
				}
				if ( results.isLastPage() || ids.size() > 2 ) {
					break;
				}
				page = results.getNextPage();
			}
		} );
		assertEquals( List.of( 1, 2 ), ids );
	}

	private static List<Integer> idsByPage(SessionFactoryScope scope, KeyedPage<Word> firstPage) {
		final List<Integer> ids = new ArrayList<>();
		scope.inSession( session -> {
			KeyedPage<Word> page = firstPage;
			while ( true ) {
				final KeyedResultList<Word> results =
						session.createSelectionQuery( "from Word", Word.class ).getKeyedResultList( page );
				for ( Word word : results.getResultList() ) {
					ids.add( word.id );
				}
				if ( results.isLastPage() ) {
					break;
				}
				page = results.getNextPage();
			}
		} );
		return ids;
	}

	@Entity(name = "Word")
	public static class Word {
		@Id
		Integer id;
		String text;

		public Word() {
		}

		public Word(Integer id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Entity(name = "Label")
	public static class Label {
		@Id
		Integer id;
		// the stored value differs from the value of the attribute
		@Convert(converter = PrefixConverter.class)
		String code;

		public Label() {
		}

		public Label(Integer id, String code) {
			this.id = id;
			this.code = code;
		}
	}

	public static class PrefixConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute == null ? null : "x" + attribute;
		}

		@Override
		public String convertToEntityAttribute(String column) {
			return column == null ? null : column.substring( 1 );
		}
	}
}
