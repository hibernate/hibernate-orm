package org.hibernate.orm.test.mapping.userguide;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Executable examples for the Domain Model chapter.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		DomainModelGuideTests.Profile.class,
		DomainModelGuideTests.Author.class,
		DomainModelGuideTests.Entry.class
})
@SessionFactory(useCollectingStatementObserver = true)
public class DomainModelGuideTests {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void immutableEmbeddedValue(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Profile profile = new Profile();
			profile.id = 1L;
			profile.address = new Address( "Original" );
			session.persist( profile );
		} );
		scope.inTransaction( session -> {
			//tag::guide-immutable-embedded-change[]
			Profile profile = session.find( Profile.class, 1L );
			profile.address.city = "Changed";
			//end::guide-immutable-embedded-change[]
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Profile.class, 1L ).address.city ).isEqualTo( "Original" );
		} );
		scope.inTransaction( session -> session.find( Profile.class, 1L ).address = new Address( "Replacement" ) );
		scope.inTransaction( session -> {
			assertThat( session.find( Profile.class, 1L ).address.city ).isEqualTo( "Original" );
		} );
	}

	@Test
	void componentAttributeAndTypePlacement(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Profile profile = new Profile();
			profile.id = 1L;
			profile.otherAddress = new TypeMarkedAddress();
			profile.otherAddress.otherCity = "Original";
			profile.otherAddress.fixed = "Fixed";
			session.persist( profile );
		} );
		scope.inTransaction( session -> {
			Profile profile = session.find( Profile.class, 1L );
			profile.otherAddress.otherCity = "Changed";
			profile.otherAddress.fixed = "Ignored";
		} );
		scope.inTransaction( session -> {
			Profile profile = session.find( Profile.class, 1L );
			assertThat( profile.otherAddress.otherCity ).isEqualTo( "Changed" );
			assertThat( profile.otherAddress.fixed ).isEqualTo( "Fixed" );
		} );
	}

	@Test
	void immutableAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Author author = new Author( 1L, "A", "Original" );
			session.persist( author );
			session.persist( new Author( 2L, "B", "Other" ) );
			Profile profile = new Profile();
			profile.id = 1L;
			profile.author = author;
			profile.editor = author;
			profile.target = author;
			session.persist( profile );
		} );
		scope.inTransaction( session -> {
			//tag::guide-immutable-association-change[]
			Profile profile = session.find( Profile.class, 1L );
			profile.author.setName( "Updated" );
			profile.author = session.find( Author.class, 2L );
			//end::guide-immutable-association-change[]
			profile.editor = session.find( Author.class, 2L );
			profile.target = session.find( Author.class, 2L );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Profile.class, 1L ).author.getCode() ).isEqualTo( "A" );
			assertThat( session.find( Author.class, 1L ).getName() ).isEqualTo( "Updated" );
			assertThat( session.find( Profile.class, 1L ).editor.getCode() ).isEqualTo( "A" );
			assertThat( ((Author) session.find( Profile.class, 1L ).target).getCode() ).isEqualTo( "A" );
		} );
	}

	@Test
	void secondaryTableAndVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Entry entry = new Entry();
			entry.id = 1L;
			entry.title = "Original";
			entry.price = new BigDecimal( "12.50" );
			entry.summary = "First";
			entry.createdBy = "Application";
			entry.published = Year.of( 2026 );
			session.persist( entry );
		} );
		Long version = scope.fromTransaction( session -> session.find( Entry.class, 1L ).version );
		scope.inTransaction( session -> {
			Entry entry = session.find( Entry.class, 1L );
			entry.createdBy = "Ignored";
			entry.summary = "Updated";
		} );
		scope.inTransaction( session -> {
			Entry entry = session.find( Entry.class, 1L );
			assertThat( entry.createdBy ).isEqualTo( "Application" );
			assertThat( entry.summary ).isEqualTo( "Updated" );
			assertThat( entry.version ).isGreaterThan( version );
			assertThat( entry.published ).isEqualTo( Year.of( 2026 ) );
		} );
	}

	@Test
	void identifierBagDuplicates(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//tag::guide-idbag-persist[]
			Profile profile = new Profile();
			profile.id = 1L;
			profile.labels.add( "review" );
			profile.labels.add( "review" );
			session.persist( profile );
			//end::guide-idbag-persist[]
		} );
		scope.getCollectingStatementObserver().clear();
		scope.inTransaction( session -> {
			//tag::guide-idbag-remove[]
			Profile profile = session.find( Profile.class, 1L );
			assertThat( profile.labels ).containsExactly( "review", "review" );
			profile.labels.remove( "review" );
			//end::guide-idbag-remove[]
		} );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
				.anyMatch( sql -> sql.toLowerCase().startsWith( "delete from guide_labels" ) && sql.contains( "label_id=?" ) );
		scope.inTransaction( session -> {
			assertThat( session.find( Profile.class, 1L ).labels ).containsExactly( "review" );
			assertThat( session.createNativeQuery( "select count(*) from guide_labels", Long.class ).getSingleResult() ).isEqualTo( 1L );
		} );
	}

	@Test
	void documentMutation(SessionFactoryScope scope) throws JsonProcessingException {
		scope.inTransaction( session -> {
			Profile profile = new Profile();
			profile.id = 1L;
			profile.preferences.put( "theme", "light" );
			profile.document = "{\"theme\":\"light\"}";
			session.persist( profile );
		} );
		scope.inTransaction( session -> {
			//tag::guide-json-mutate[]
			Profile profile = session.find( Profile.class, 1L );
			profile.preferences.put( "theme", "dark" );
			//end::guide-json-mutate[]
		} );
		String document = scope.fromTransaction( session -> {
			Profile profile = session.find( Profile.class, 1L );
			assertThat( profile.preferences ).containsEntry( "theme", "dark" );
			//tag::guide-json-replace[]
			profile.preferences = new HashMap<>( Map.of( "theme", "blue" ) );
			//end::guide-json-replace[]
			return profile.document;
		} );
		ObjectMapper mapper = new ObjectMapper();
		assertThat( mapper.readTree( document ) ).isEqualTo( mapper.readTree( "{\"theme\":\"light\"}" ) );
		scope.inTransaction( session -> assertThat( session.find( Profile.class, 1L ).preferences ).containsEntry( "theme", "blue" ) );
	}

	@Test
	void equalityWithProxy(SessionFactoryScope scope) {
		Author detached = new Author( 1L, "A", "Author" );
		scope.inTransaction( session -> session.persist( detached ) );
		scope.inTransaction( session -> {
			Author proxy = session.getReference( Author.class, 1L );
			assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
			assertThat( detached.equals( proxy ) ).isTrue();
			assertThat( proxy.equals( detached ) ).isTrue();
			assertThat( proxy.hashCode() ).isEqualTo( detached.hashCode() );
			assertThat( new HashSet<>( List.of( detached ) ) ).contains( proxy );
			assertThat( Hibernate.isInitialized( proxy ) ).isTrue();
			assertThat( session.find( Author.class, 1L ) ).isEqualTo( detached );
		} );
	}

	@Entity(name = "GuideProfile")
	@Table(name = "guide_profile")
	@SequenceGenerator(name = "guide_label_ids", sequenceName = "guide_label_ids")
	public static class Profile {
		@Id
		Long id;
		//tag::guide-immutable-embedded[]
		@Embedded
		@Immutable
		Address address;
		//end::guide-immutable-embedded[]
		//tag::guide-immutable-association[]
		@ManyToOne(fetch = FetchType.LAZY)
		@Immutable
		Author author;
		//end::guide-immutable-association[]
		@OneToOne(fetch = FetchType.LAZY)
		@Immutable
		Author editor;
		@Any
		@AnyDiscriminatorValue(discriminator = "author", entity = Author.class)
		@AnyKeyJavaClass(Long.class)
		@Column(name = "target_type")
		@JoinColumn(name = "target_id")
		@Immutable
		Object target;
		@Embedded
		TypeMarkedAddress otherAddress;
		//tag::guide-idbag-mapping[]
		@ElementCollection
		@CollectionTable(name = "guide_labels", joinColumns = @JoinColumn(name = "profile_id"))
		@Column(name = "label")
		@CollectionId(column = @Column(name = "label_id"), generator = "guide_label_ids")
		@CollectionIdJdbcTypeCode(Types.BIGINT)
		List<String> labels = new ArrayList<>();
		//end::guide-idbag-mapping[]
		//tag::guide-json-mapping[]
		@JdbcTypeCode(SqlTypes.JSON)
		Map<String, String> preferences = new HashMap<>();
		@JdbcTypeCode(SqlTypes.JSON)
		String document;
		//end::guide-json-mapping[]
	}

	@Embeddable
	public static class Address {
		String city;
		public Address() {
		}
		Address(String city) {
			this.city = city;
		}
	}

	@Embeddable
	@Immutable
	public static class TypeMarkedAddress {
		String otherCity;
		@Immutable
		String fixed;
	}

	//tag::guide-columns-secondary[]
	@Entity(name = "GuideEntry")
	@Table(name = "guide_entry")
	@SecondaryTable(name = "guide_entry_details", pkJoinColumns = @PrimaryKeyJoinColumn(name = "entry_id"))
	public static class Entry {
		@Id
		Long id;
		@Column(nullable = false, length = 120)
		String title;
		@Column(precision = 12, scale = 2)
		BigDecimal price;
		@Column(updatable = false)
		String createdBy;
		@Column(table = "guide_entry_details")
		String summary;
		//tag::guide-version[]
		@Version
		Long version;
		//end::guide-version[]
		//tag::guide-year[]
		@Column(name = "published_year")
		Year published;
		//end::guide-year[]
	}
	//end::guide-columns-secondary[]

	//tag::guide-proxy-equality[]
	@Entity(name = "GuideAuthor")
	@Table(name = "guide_author")
	public static class Author {
		@Id
		Long id;
		@NaturalId
		@Column(nullable = false, unique = true)
		private String code;
		String name;
		public Author() {
		}
		public Author(Long id, String code, String name) {
			this.id = id;
			this.code = Objects.requireNonNull( code );
			this.name = name;
		}
		public String getCode() {
			return code;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		@Override
		public boolean equals(Object other) {
			return this == other || other instanceof Author author
					&& getCode() != null && getCode().equals( author.getCode() );
		}
		@Override
		public int hashCode() {
			return Objects.hashCode( getCode() );
		}
	}
	//end::guide-proxy-equality[]
}
