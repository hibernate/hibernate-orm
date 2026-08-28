/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.enumeratedvalue;

import org.hibernate.dialect.SpannerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reading a column value that matches no {@link EnumeratedValue} must fail,
 * the same way the {@code EnumType.STRING} mapping fails via {@code Enum.valueOf()},
 * instead of silently producing a {@code null} attribute value.
 * <p>
 * The schema is created by hand because the exported schema carries a check
 * constraint over the known values, which would reject the bad row up front.
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-20824")
@DomainModel(annotatedClasses = UnknownEnumeratedValueTests.Person.class)
@SessionFactory(exportSchema = false)
@SkipForDialect(dialectClass = SpannerDialect.class, matchSubTypes = true,
		reason = "Spanner GoogleSQL cannot parse the ANSI DDL that this test writes by hand instead of the exported schema")
public class UnknownEnumeratedValueTests {

	@BeforeEach
	void createSchema(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createNativeMutationQuery(
				"create table unknown_enum_persons (id integer not null primary key, gender varchar(1), status integer)"
		).executeUpdate() );
	}

	@AfterEach
	void dropSchema(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createNativeMutationQuery(
				"drop table unknown_enum_persons"
		).executeUpdate() );
	}

	@Test
	void testUnknownOrdinalValue(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createNativeMutationQuery(
				"insert into unknown_enum_persons (id, status) values (1, 999)"
		).executeUpdate() );

		scope.inTransaction( (session) ->
				assertThatThrownBy( () -> session.find( Person.class, 1 ) )
						.isInstanceOf( IllegalArgumentException.class )
						.hasMessageContaining( "Unknown value [999] for enum class [" + Status.class.getName() + "]" )
		);
	}

	@Test
	void testUnknownStringValue(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createNativeMutationQuery(
				"insert into unknown_enum_persons (id, gender) values (1, 'X')"
		).executeUpdate() );

		scope.inTransaction( (session) ->
				assertThatThrownBy( () -> session.find( Person.class, 1 ) )
						.isInstanceOf( IllegalArgumentException.class )
						.hasMessageContaining( "Unknown value [X] for enum class [" + Gender.class.getName() + "]" )
		);
	}

	@Test
	void testKnownValuesStillResolve(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createNativeMutationQuery(
				"insert into unknown_enum_persons (id, gender, status) values (1, 'F', 300)"
		).executeUpdate() );

		scope.inTransaction( (session) -> {
			final Person person = session.find( Person.class, 1 );
			assertThat( person.gender ).isEqualTo( Gender.FEMALE );
			assertThat( person.status ).isEqualTo( Status.INACTIVE );
		} );
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
	}

	@Entity(name = "Person")
	@Table(name = "unknown_enum_persons")
	@SuppressWarnings({ "FieldCanBeLocal", "unused" })
	public static class Person {
		@Id
		private Integer id;
		@Enumerated(EnumType.STRING)
		private Gender gender;
		@Enumerated
		private Status status;
	}
}
