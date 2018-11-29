/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Tuple;

import org.hibernate.orm.test.jpa.EntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-10778" )
public class PackagePrivateAttributeConverterEntityManagerFactoryTest
		extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Tester.class };
	}

	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Tester tester = new Tester();
			tester.setId( 1L );
			tester.setCode( 123 );

			entityManager.persist( tester );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Tuple tuple = entityManager.createQuery(
				"select t.code as code " +
				"from Tester t " +
				"where t.id = :id", Tuple.class )
			.setParameter( "id", 1L )
			.getSingleResult();

			assertEquals( "123", tuple.get( "code" ) );

			Tester tester = entityManager.find( Tester.class, 1L );

			assertEquals( 123, (int) tester.getCode() );
		} );
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
