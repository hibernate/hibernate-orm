/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.enumeratedvalue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Test for {@linkplain EnumeratedValue} introduced in JPA 3.2
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class EnumeratedValueTests {

	@DomainModel(annotatedClasses = Person.class)
	@SessionFactory(useCollectingStatementInspector = true)
	@Test
	void testBasicUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new Person( 1, "John", Gender.MALE, Status.ACTIVE ) );
		} );

		scope.inTransaction( (session) -> {
			session.doWork( (connection) -> {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery( "select gender, status from persons" )) {
						assertThat( resultSet.next() ).isTrue();
						final String storedGender = resultSet.getString( 1 );
						assertThat( storedGender ).isEqualTo( "M" );
						final int storedStatus = resultSet.getInt( 2 );
						assertThat( storedStatus ).isEqualTo( 200 );
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
			session.persist( new Person( 1, "John", null, null ) );
		} );

		scope.inTransaction( (session) -> {
			session.doWork( (connection) -> {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery( "select gender, status from persons" )) {
						assertThat( resultSet.next() ).isTrue();
						final String storedGender = resultSet.getString( 1 );
						assertThat( resultSet.wasNull() ).isTrue();
						assertThat( storedGender ).isNull();
						final int storedStatus = resultSet.getInt( 2 );
						assertThat( resultSet.wasNull() ).isTrue();
					}
				}
			} );
		} );
	}

	@DomainModel(annotatedClasses = Person.class)
	@SessionFactory(useCollectingStatementInspector = true)
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsColumnCheck.class )
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

			try (PreparedStatement statement = connection.prepareStatement( "insert into persons (id, status) values (?, ?)" ) ) {
				statement.setInt( 1, 101 );
				// this would work without check constraints or with check constraints based solely on EnumType#ORDINAL
				statement.setInt( 2, 1 );
				statement.executeUpdate();
				fail( "Expecting a failure" );
			}
			catch (SQLException expected) {
			}
		} ) );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	public enum Gender {
		MALE( "M" ),
		FEMALE( "F" ),
		OTHER( "U" );

		@EnumeratedValue
		private final String code;

		Gender(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	public enum Status {
		PENDING( 100 ),
		ACTIVE( 200 ),
		INACTIVE( 300 );

		@EnumeratedValue
		private final int code;

		Status(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}

	@Entity(name="Person")
	@Table(name="persons")
	@SuppressWarnings({ "FieldCanBeLocal", "unused" })
	public static class Person {
		@Id
		private Integer id;
		private String name;
		@Enumerated(EnumType.STRING)
		private Gender gender;
		@Enumerated
		private Status status;

		public Person() {
		}

		public Person(Integer id, String name, Gender gender, Status status) {
			this.id = id;
			this.name = name;
			this.gender = gender;
			this.status = status;
		}
	}
}
