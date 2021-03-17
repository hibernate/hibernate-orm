/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi.metadatabuildercontributor;

import java.time.YearMonth;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@TestForIssue( jiraKey = "HHH-13040" )
public class AttributeConverterMetadataBuilderContributorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Employee.class,
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put(
				EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR,
				(MetadataBuilderContributor) metadataBuilder ->
						metadataBuilder.applyAttributeConverter( YearMonthAttributeConverter.class )
		);
	}

	final Employee employee = new Employee();

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			employee.id = 1L;
			employee.username = "user@acme.com";
			employee.nextVacation = YearMonth.of( 2018, 12 );

			entityManager.persist( employee );
		} );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = entityManager.find(Employee.class, 1L);

			assertEquals( YearMonth.of( 2018, 12 ), employee.nextVacation );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		@Column(name = "next_vacation", columnDefinition = "INTEGER")
		private YearMonth nextVacation;
	}

	@Converter(autoApply = true)
	public static class YearMonthAttributeConverter
			implements AttributeConverter<YearMonth, Integer> {

		@Override
		public Integer convertToDatabaseColumn(YearMonth attribute) {
			return (attribute.getYear() * 100) + attribute.getMonth().getValue();
		}

		@Override
		public YearMonth convertToEntityAttribute(Integer dbData) {
			return YearMonth.of(dbData / 100, dbData % 100);
		}
	}

}
