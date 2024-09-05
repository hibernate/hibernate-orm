/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.enumeratedvalue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EnumAndColumnDefinitionTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-17017")
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(MariaDBDialect.class)
@RequiresDialect(H2Dialect.class)
public class EnumAndColumnDefinitionTest {

	@AfterAll
	public static void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		var id = 1L;
		var enumValue = MyEnum.A;
		var anotherEnumValue = AnotherMyEnum.B;
		var anotherEnumValue2 = AnotherMyEnum.A;
		scope.inTransaction(
				session ->
						session.persist( new TestEntity( id, enumValue, anotherEnumValue, anotherEnumValue2 ) )
		);

		scope.inSession(
				session -> {
					var selectMyEnum = session.createNativeQuery(
									"select my_enum from test_entity",
									String.class
							)
							.getSingleResult();
					assertThat( selectMyEnum ).isEqualTo( "0" );

					var selectAnotherMyEnum = session.createNativeQuery(
									"select another_my_enum from test_entity",
									String.class
							)
							.getSingleResult();
					assertThat( selectAnotherMyEnum ).isEqualTo( "1" );

					var selectAnotherMyEnum2 = session.createNativeQuery(
									"select another_my_enum_2 from test_entity",
									String.class
							)
							.getSingleResult();
					// because the attribute is annotated with @JdbcTypeCode(SqlTypes.VARCHAR)
					// the enum string value has been saved
					assertThat( selectAnotherMyEnum2 ).isEqualTo( "A" );
				}
		);

		scope.inSession(
				session -> {
					var testEntity = session.find( TestEntity.class, id );
					assertThat( testEntity.myEnum ).isEqualTo( enumValue );
					assertThat( testEntity.anotherMyEnum ).isEqualTo( anotherEnumValue );
					assertThat( testEntity.anotherMyEnum2 ).isEqualTo( anotherEnumValue2 );
				}
		);
	}

	public enum MyEnum {
		A( 0 ),
		B( 1 );

		@EnumeratedValue
		final int intValue;

		MyEnum(int intValue) {
			this.intValue = intValue;
		}
	}

	public enum AnotherMyEnum {
		A,
		B;
	}

	@Entity(name = "TestEntity")
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		Long id;

		@Enumerated(value = EnumType.ORDINAL)
		@Column(name = "my_enum", columnDefinition = "VARCHAR(255) NOT NULL")
		/*
			Annotating the enum with @EnumeratedValue permits to store the ordinal value
			and retrieve the correct enum value even when the colum is VARCHAR without the
			need to specify the @JdbcTypeCode(...).
		*/
		MyEnum myEnum;

		@Enumerated(value = EnumType.ORDINAL)
		@Column(name = "another_my_enum", columnDefinition = "VARCHAR(255) NOT NULL")
		/*
			Without specifying the JdbcTypeCode Hibernate has no clue
			of the column being a VARCHAR and being the enum type an
			ordinal so a TinyIntJdbcType is used.
			Using @JdbcTypeCode(SqlTypes.INTEGER) the ordinal values is saved.
		 */
		@JdbcTypeCode(SqlTypes.INTEGER)
		AnotherMyEnum anotherMyEnum;

		@Enumerated(value = EnumType.ORDINAL)
		@Column(name = "another_my_enum_2", columnDefinition = "VARCHAR(255) NOT NULL")
		/*
			Without specifying the JdbcTypeCode Hibernate has no clue
			of the column being a VARCHAR and being the enum type an
			ordinal so TinyIntJdbcType is used.
			Using @JdbcTypeCode(SqlTypes.VARCHAR) the enum string value is saved.
		 */
		@JdbcTypeCode(SqlTypes.VARCHAR)
		AnotherMyEnum anotherMyEnum2;

		String name;

		public TestEntity() {

		}

		public TestEntity(Long id, MyEnum myEnum, AnotherMyEnum anotherMyEnum, AnotherMyEnum anotherMyEnum2) {
			this.id = id;
			this.myEnum = myEnum;
			this.anotherMyEnum = anotherMyEnum;
			this.anotherMyEnum2 = anotherMyEnum2;
		}
	}
}
