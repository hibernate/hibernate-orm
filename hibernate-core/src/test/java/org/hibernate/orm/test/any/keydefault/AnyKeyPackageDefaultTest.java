package org.hibernate.orm.test.any.keydefault;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The key type of an {@link Any} or {@link ManyToAny} mapping defaults to the
 * {@link AnyKeyJavaClass} declared on the package, unless specified by the attribute.
 *
 * @author Vincent Bouthinon
 */
@DomainModel(annotatedClasses = {
		AnyKeyPackageDefaultTest.Shelf.class,
		AnyKeyPackageDefaultTest.Book.class,
		AnyKeyPackageDefaultTest.Label.class
})
@SessionFactory
@JiraKey("HHH-20319")
class AnyKeyPackageDefaultTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void keyTypes(DomainModelScope scope) {
		final PersistentClass shelf = scope.getDomainModel().getEntityBinding( Shelf.class.getName() );

		final var content = (org.hibernate.mapping.Any) shelf.getProperty( "content" ).getValue();
		assertThat( keyJavaType( content ) ).isEqualTo( Long.class );

		final var label = (org.hibernate.mapping.Any) shelf.getProperty( "label" ).getValue();
		assertThat( keyJavaType( label ) ).isEqualTo( String.class );

		final var items = (org.hibernate.mapping.Collection) shelf.getProperty( "items" ).getValue();
		assertThat( keyJavaType( (org.hibernate.mapping.Any) items.getElement() ) ).isEqualTo( Long.class );
	}

	@Test
	void roundTrip(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = new Book( 1L );
			final var label = new Label( "NEW" );
			session.persist( book );
			session.persist( label );
			final var shelf = new Shelf();
			shelf.content = book;
			shelf.label = label;
			shelf.items.add( book );
			session.persist( shelf );
		} );

		scope.inTransaction( session -> {
			final var shelf = session.createQuery( "from Shelf", Shelf.class ).getSingleResult();
			assertThat( shelf.content ).isInstanceOf( Book.class );
			assertThat( shelf.label ).isInstanceOf( Label.class );
			assertThat( shelf.items ).singleElement().isInstanceOf( Book.class );
		} );
	}

	private static Class<?> keyJavaType(org.hibernate.mapping.Any any) {
		return any.getKeyDescriptor().resolve().getDomainJavaType().getJavaTypeClass();
	}

	@Entity(name = "Shelf")
	@Table(name = "SHELF")
	static class Shelf {
		@Id
		@GeneratedValue
		Long id;

		@Any
		@JoinColumn(name = "CONTENT_ID")
		@Column(name = "CONTENT_TYPE")
		Object content;

		@Any
		@AnyKeyJavaClass(String.class)
		@JoinColumn(name = "LABEL_ID")
		@Column(name = "LABEL_TYPE")
		Object label;

		@ManyToAny
		@Column(name = "ITEM_TYPE")
		@JoinTable(name = "SHELF_ITEMS",
				joinColumns = @JoinColumn(name = "SHELF_ID"),
				inverseJoinColumns = @JoinColumn(name = "ITEM_ID"))
		Set<Object> items = new HashSet<>();
	}

	@Entity(name = "Book")
	@Table(name = "BOOK")
	static class Book {
		@Id
		Long id;

		Book() {
		}

		Book(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Label")
	@Table(name = "LABEL")
	static class Label {
		@Id
		String code;

		Label() {
		}

		Label(String code) {
			this.code = code;
		}
	}
}
