package org.hibernate.orm.test.mapping.enumeratedvalue;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.mapping.enumeratedvalue.EnumeratedValueTests.Gender;
import static org.hibernate.orm.test.mapping.enumeratedvalue.EnumeratedValueTests.Status;

@JiraKey("HHH-20410")
@SuppressWarnings("JUnitMalformedDeclaration")
public class ImplicitEnumeratedValueTests {
	@Test
	@DomainModel(annotatedClasses = ImplicitEntity.class)
	@SessionFactory
	void testFieldAccess(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new ImplicitEntity();
			entity.id = 1;
			entity.gender = Gender.FEMALE;
			entity.status = Status.ACTIVE;
			entity.ordinary = Ordinary.SECOND;
			session.persist( entity );
			session.flush();
			session.clear();

			session.doWork( connection -> {
				try (var statement = connection.createStatement();
						var result = statement.executeQuery( "select gender, status, ordinary from implicit_enum" )) {
					assertThat( result.next() ).isTrue();
					assertThat( result.getString( 1 ) ).isEqualTo( "F" );
					assertThat( result.getInt( 2 ) ).isEqualTo( 200 );
					assertThat( result.getInt( 3 ) ).isEqualTo( 1 );
				}
			} );

			final var loaded = session.find( ImplicitEntity.class, 1 );
			assertThat( loaded.gender ).isEqualTo( Gender.FEMALE );
			assertThat( loaded.status ).isEqualTo( Status.ACTIVE );
			assertThat( loaded.ordinary ).isEqualTo( Ordinary.SECOND );
			assertThat( session.createQuery( "from ImplicitEntity where gender = :gender", ImplicitEntity.class )
					.setParameter( "gender", Gender.FEMALE ).getSingleResult() ).isSameAs( loaded );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = ImplicitEntity.class)
	@SessionFactory
	void testNulls(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new ImplicitEntity();
			entity.id = 1;
			session.persist( entity );
			session.flush();
			session.clear();

			session.doWork( connection -> {
				try (var statement = connection.createStatement();
						var result = statement.executeQuery( "select gender, status, ordinary from implicit_enum" )) {
					assertThat( result.next() ).isTrue();
					assertThat( result.getObject( 1 ) ).isNull();
					assertThat( result.getObject( 2 ) ).isNull();
					assertThat( result.getObject( 3 ) ).isNull();
				}
			} );

			final var loaded = session.find( ImplicitEntity.class, 1 );
			assertThat( loaded.gender ).isNull();
			assertThat( loaded.status ).isNull();
			assertThat( loaded.ordinary ).isNull();
		} );
	}

	@Test
	@DomainModel(annotatedClasses = PropertyEntity.class)
	@SessionFactory
	void testPropertyAccess(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new PropertyEntity();
			entity.setId( 1 );
			entity.setGender( Gender.FEMALE );
			session.persist( entity );
			session.flush();
			session.clear();

			assertThat( session.createNativeQuery( "select gender from property_enum", String.class )
					.getSingleResult() ).isEqualTo( "F" );
			assertThat( session.find( PropertyEntity.class, 1 ).getGender() ).isEqualTo( Gender.FEMALE );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = CollectionEntity.class)
	@SessionFactory
	void testElementCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new CollectionEntity();
			entity.id = 1;
			entity.genders.add( Gender.FEMALE );
			entity.genders.add( Gender.MALE );
			session.persist( entity );
			session.flush();
			session.clear();

			assertThat( session.createNativeQuery( "select gender from implicit_enum_values", String.class )
					.getResultList() ).containsExactlyInAnyOrder( "F", "M" );
			assertThat( session.find( CollectionEntity.class, 1 ).genders )
					.containsExactlyInAnyOrder( Gender.FEMALE, Gender.MALE );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = { ConvertedEntity.class, GenderConverter.class })
	@SessionFactory
	void testConverterPrecedence(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new ConvertedEntity();
			entity.id = 1;
			entity.explicitGender = Gender.FEMALE;
			entity.implicitGender = Gender.MALE;
			session.persist( entity );
			session.flush();
			session.clear();

			session.doWork( connection -> {
				try (var statement = connection.createStatement();
						var result = statement.executeQuery( "select explicitGender, implicitGender from converted_enum" )) {
					assertThat( result.next() ).isTrue();
					assertThat( result.getString( 1 ) ).isEqualTo( "FEMALE" );
					assertThat( result.getString( 2 ) ).isEqualTo( "MALE" );
				}
			} );

			final var loaded = session.find( ConvertedEntity.class, 1 );
			assertThat( loaded.explicitGender ).isEqualTo( Gender.FEMALE );
			assertThat( loaded.implicitGender ).isEqualTo( Gender.MALE );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	enum Ordinary {
		FIRST, SECOND
	}

	@Entity(name = "ImplicitEntity")
	@Table(name = "implicit_enum")
	public static class ImplicitEntity {
		@Id
		private Integer id;
		private Gender gender;
		private Status status;
		private Ordinary ordinary;
	}

	@Entity(name = "PropertyEntity")
	@Table(name = "property_enum")
	public static class PropertyEntity {
		private Integer id;
		private Gender gender;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}
	}

	@Entity(name = "CollectionEntity")
	public static class CollectionEntity {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(name = "implicit_enum_values")
		@Column(name = "gender")
		private Set<Gender> genders = new HashSet<>();
	}

	@Entity(name = "ConvertedEntity")
	@Table(name = "converted_enum")
	public static class ConvertedEntity {
		@Id
		private Integer id;
		@Convert(converter = GenderConverter.class)
		private Gender explicitGender;
		private Gender implicitGender;
	}

	@Converter(autoApply = true)
	public static class GenderConverter implements AttributeConverter<Gender, String> {
		@Override
		public String convertToDatabaseColumn(Gender attribute) {
			return attribute == null ? null : attribute.name();
		}

		@Override
		public Gender convertToEntityAttribute(String dbData) {
			return dbData == null ? null : Gender.valueOf( dbData );
		}
	}
}
