/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-10778" )
@DomainModel( annotatedClasses = PackagePrivateAttributeConverterEntityManagerFactoryTest.Tester.class )
@SessionFactory
public class PackagePrivateAttributeConverterEntityManagerFactoryTest {
	public final String sql = "select code from Tester where id = :id";

	@Test
	@NotImplementedYet( strict = false, reason = "Support for passing `resultType` to `#createNativeQuery` not yet implemented" )
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Tester tester = new Tester();
					tester.setId( 1L );
					tester.setCode( 123 );

					session.persist( tester );
				}
		);

		scope.inTransaction(
				(session) -> {
					final Tuple tuple = (Tuple) session.createNativeQuery( sql, Tuple.class )
							.setParameter( "id", 1L )
							.getSingleResult();
					assertEquals( "123", tuple.get( "code" ) );

					final Tester tester = session.find( Tester.class, 1L );
					assertEquals( 123, (int) tester.getCode() );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createQuery( "delete Tester" ).executeUpdate() );
	}

	// Entity declarations used in the test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Entity(name = "Tester")
	public static class Tester {
		@Id
		private Long id;

		@Convert( converter = IntegerToVarcharConverter.class )
		private Integer code;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getCode() {
			return code;
		}

		public void setCode(Integer code) {
			this.code = code;
		}
	}

	@Converter( autoApply = true )
	static class IntegerToVarcharConverter implements AttributeConverter<Integer,String> {
		@Override
		public String convertToDatabaseColumn(Integer attribute) {
			return attribute == null ? null : attribute.toString();
		}

		@Override
		public Integer convertToEntityAttribute(String dbData) {
			return dbData == null ? null : Integer.valueOf( dbData );
		}
	}
}
