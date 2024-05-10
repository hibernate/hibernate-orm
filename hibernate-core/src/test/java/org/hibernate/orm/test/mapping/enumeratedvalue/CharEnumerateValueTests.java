/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.enumeratedvalue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.orm.test.mapping.converted.enums.ConvertedEnumCheckConstraintsTests;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * The spec says that for {@linkplain jakarta.persistence.EnumType#STRING}, only {@linkplain String}
 * is supported.  But {@code char} / {@linkplain Character} make a lot of sense to support as well
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class CharEnumerateValueTests {
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

	@DomainModel(annotatedClasses = Person.class)
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

		@EnumeratedValue
		private final char code;

		Gender(char code) {
			this.code = code;
		}

		public char getCode() {
			return code;
		}
	}

	@SuppressWarnings({ "FieldCanBeLocal", "unused" })
	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;
		@Enumerated(EnumType.STRING)
		@JdbcTypeCode(SqlTypes.CHAR)
		@Column(length = 1)
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
