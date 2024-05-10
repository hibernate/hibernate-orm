/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.converted.enums;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.orm.test.mapping.enumeratedvalue.EnumeratedValueTests;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ConvertedEnumCheckConstraintsTests {
	@Test
	@DomainModel(annotatedClasses = Person.class)
	@SessionFactory
	void testBasicUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new Person( 1, "John", Gender.MALE ) );
		} );

		scope.inTransaction( (session) -> {
			session.doWork( (connection) -> {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery( "select gender from persons" )) {
						assertThat( resultSet.next() ).isTrue();
						final String storedGender = resultSet.getString( 1 );
						assertThat( storedGender ).isEqualTo( "M" );
					}
				}
			} );
		} );
	}

	@DomainModel(annotatedClasses = Person.class)
	@SessionFactory(useCollectingStatementInspector = true)
	@Test
	void testNulls(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new Person( 1, "John", null ) );
		} );

		scope.inTransaction( (session) -> {
			session.doWork( (connection) -> {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery( "select gender from persons" )) {
						assertThat( resultSet.next() ).isTrue();
						final String storedGender = resultSet.getString( 1 );
						assertThat( resultSet.wasNull() ).isTrue();
						assertThat( storedGender ).isNull();
					}
				}
			} );
		} );
	}

	@DomainModel(annotatedClasses = EnumeratedValueTests.Person.class)
	@SessionFactory(useCollectingStatementInspector = true)
	@Test
	void verifyCheckConstraints(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (PreparedStatement statement = connection.prepareStatement( "insert into persons (id, gender) values (?, ?)" ) ) {
				statement.setInt( 1, 100 );
				// this would work without check constraints or with check constraints based solely on EnumType#STRING
				statement.setString( 2, "MALE" );
				statement.executeUpdate();
				fail( "Expecting a failure" );
			}
			catch (SQLException expected) {
			}
		} ) );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createMutationQuery( "delete Person" ).executeUpdate() );
	}

	public enum Gender {
		MALE( 'M' ),
		FEMALE( 'F' ),
		OTHER( 'U' );
		private final char code;

		Gender(char code) {
			this.code = code;
		}

		public char getCode() {
			return code;
		}
	}

	public static class GenderConverter implements AttributeConverter<Gender,Character> {
		@Override
		public Character convertToDatabaseColumn(Gender attribute) {
			if ( attribute == null ) {
				return null;
			}
			return attribute.getCode();
		}

		@Override
		public Gender convertToEntityAttribute(Character dbData) {
			if ( dbData == null ) {
				return null;
			}
			switch ( dbData ) {
				case 'M' -> {
					return Gender.MALE;
				}
				case 'F' -> {
					return Gender.FEMALE;
				}
				case 'U' -> {
					return Gender.OTHER;
				}
				default -> throw new IllegalArgumentException( "Bad data: " + dbData );
			}
		}
	}

	@SuppressWarnings({ "FieldCanBeLocal", "unused" })
	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;
		@JdbcTypeCode(SqlTypes.CHAR)
		@Column(length = 1)
		@Convert( converter = GenderConverter.class )
		private Gender gender;

		public Person() {
		}

		public Person(Integer id, String name, Gender gender) {
			this.id = id;
			this.name = name;
			this.gender = gender;
		}
	}
}
